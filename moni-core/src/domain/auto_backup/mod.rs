use chrono::Datelike;
use rusqlite::Connection;

use crate::core::error::CoreError;
use crate::domain::backup::exporter::backup_export;
use crate::models::auto_backup::{AutoBackupFrequency, AutoBackupReport};

const AUTO_BACKUP_PREFIX: &str = "Moni_Backup_";
const AUTO_BACKUP_SUFFIX: &str = ".zip";

/// 判断是否应该执行自动备份。
///
/// - `last_backup_time_iso`: 上次备份时间的 ISO 8601 字符串（如 `"2026-05-07T10:30:00+08:00"`），`None` 表示从未备份。
/// - `frequency`: 自动备份频率。
/// - `now_iso`: 当前时间的 ISO 8601 字符串。
///
/// 返回 `true` 表示应执行备份。
pub fn should_auto_backup(
    last_backup_time_iso: Option<&str>,
    frequency: AutoBackupFrequency,
    now_iso: &str,
) -> bool {
    // 每次启动总是执行
    if frequency == AutoBackupFrequency::EveryLaunch {
        return true;
    }

    // 从未备份过，应执行
    let last_str = match last_backup_time_iso {
        Some(s) => s,
        None => return true,
    };

    let last_dt = match chrono::DateTime::parse_from_rfc3339(last_str) {
        Ok(dt) => dt,
        Err(_) => return true, // 解析失败视为从未备份
    };
    let now_dt = match chrono::DateTime::parse_from_rfc3339(now_iso) {
        Ok(dt) => dt,
        Err(_) => return true, // 解析失败保守地执行备份
    };

    let last_date = last_dt.date_naive();
    let now_date = now_dt.date_naive();

    match frequency {
        AutoBackupFrequency::Daily => {
            // 上次备份日期 < 今天
            last_date < now_date
        }
        AutoBackupFrequency::Weekly => {
            // 上次备份的周数 < 当前周数（基于周一）
            let last_week = last_date.iso_week();
            let now_week = now_date.iso_week();
            last_week.year() < now_week.year()
                || (last_week.year() == now_week.year() && last_week.week() < now_week.week())
        }
        AutoBackupFrequency::Monthly => {
            // 上次备份的月份 < 当前月份
            let (last_year, last_month) = (last_date.year(), last_date.month());
            let (now_year, now_month) = (now_date.year(), now_date.month());
            last_year < now_year || (last_year == now_year && last_month < now_month)
        }
        AutoBackupFrequency::EveryLaunch => true, // 上面已处理
    }
}

/// 执行自动备份，生成 `AutoBackup_yyyyMMdd_HHmmss.zip`。
///
/// 内部调用 `backup_export` 完成实际导出工作。
pub fn perform_auto_backup(
    conn: &Connection,
    backup_dir: &str,
    settings_json: &str,
    app_version_name: &str,
    app_version_code: i64,
    device_manufacturer: &str,
    device_model: &str,
    android_sdk: i32,
    on_progress: Option<&dyn Fn(&str, i32)>,
) -> Result<AutoBackupReport, CoreError> {
    // 确保备份目录存在
    std::fs::create_dir_all(backup_dir)
        .map_err(|e| CoreError::BackupIo(format!("创建备份目录失败: {e}")))?;

    let now = chrono::Local::now();
    let timestamp = now.format("%Y%m%d_%H%M%S").to_string();
    let file_name = format!("{AUTO_BACKUP_PREFIX}{timestamp}{AUTO_BACKUP_SUFFIX}");
    let zip_path = std::path::Path::new(backup_dir).join(&file_name);
    let zip_path_str = zip_path
        .to_str()
        .ok_or_else(|| CoreError::Internal("备份路径包含非法字符".to_string()))?;

    let report = backup_export(
        conn,
        zip_path_str,
        settings_json,
        app_version_name,
        app_version_code,
        device_manufacturer,
        device_model,
        android_sdk,
        on_progress,
    )?;

    Ok(AutoBackupReport {
        zip_path: zip_path_str.to_string(),
        total_bytes: report.total_bytes,
        record_count: report.record_count,
        category_count: report.category_count,
        settings_count: report.settings_count,
        created_at: now.to_rfc3339(),
    })
}

/// 清理旧自动备份，只保留最新的 `max_count` 个。
///
/// 扫描 `backup_dir` 下所有 `AutoBackup_*.zip` 文件，按修改时间降序排列，
/// 删除超出保留数量的最旧文件。
///
/// 返回实际删除的文件数量。
pub fn cleanup_auto_backups(backup_dir: &str, max_count: u32) -> Result<u32, CoreError> {
    let dir = std::path::Path::new(backup_dir);
    if !dir.exists() {
        return Ok(0);
    }

    let mut entries: Vec<std::fs::DirEntry> = std::fs::read_dir(dir)
        .map_err(|e| CoreError::BackupIo(format!("读取备份目录失败: {e}")))?
        .filter_map(|e| e.ok())
        .filter(|e| {
            let name = e.file_name();
            let name = name.to_string_lossy();
            name.starts_with(AUTO_BACKUP_PREFIX) && name.ends_with(AUTO_BACKUP_SUFFIX)
        })
        .collect();

    // 按修改时间降序（最新的在前），时间相同时按文件名降序保证确定性
    entries.sort_by(|a, b| {
        let a_meta = a.metadata().and_then(|m| m.modified()).ok();
        let b_meta = b.metadata().and_then(|m| m.modified()).ok();
        let time_cmp = match (a_meta, b_meta) {
            (Some(a_time), Some(b_time)) => b_time.cmp(&a_time),
            (Some(_), None) => std::cmp::Ordering::Less,
            (None, Some(_)) => std::cmp::Ordering::Greater,
            (None, None) => std::cmp::Ordering::Equal,
        };
        time_cmp.then_with(|| b.file_name().cmp(&a.file_name()))
    });

    if entries.len() <= max_count as usize {
        return Ok(0);
    }

    let mut removed = 0u32;
    for entry in entries.iter().skip(max_count as usize) {
        if let Err(e) = std::fs::remove_file(entry.path()) {
            log::warn!("清理旧自动备份失败: {e}, 路径: {:?}", entry.path());
        } else {
            removed += 1;
        }
    }

    Ok(removed)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_should_auto_backup_every_launch_always_true() {
        assert!(should_auto_backup(
            Some("2026-05-07T10:00:00+08:00"),
            AutoBackupFrequency::EveryLaunch,
            "2026-05-07T10:00:00+08:00"
        ));
        assert!(should_auto_backup(
            None,
            AutoBackupFrequency::EveryLaunch,
            "2026-05-07T10:00:00+08:00"
        ));
    }

    #[test]
    fn test_should_auto_backup_daily_same_day_false() {
        let last = "2026-05-07T10:00:00+08:00";
        let now = "2026-05-07T20:00:00+08:00";
        assert!(!should_auto_backup(
            Some(last),
            AutoBackupFrequency::Daily,
            now
        ));
    }

    #[test]
    fn test_should_auto_backup_daily_next_day_true() {
        let last = "2026-05-07T23:59:59+08:00";
        let now = "2026-05-08T00:00:01+08:00";
        assert!(should_auto_backup(
            Some(last),
            AutoBackupFrequency::Daily,
            now
        ));
    }

    #[test]
    fn test_should_auto_backup_weekly_same_week_false() {
        let last = "2026-05-05T10:00:00+08:00"; // 周二
        let now = "2026-05-07T10:00:00+08:00"; // 周四（同一周）
        assert!(!should_auto_backup(
            Some(last),
            AutoBackupFrequency::Weekly,
            now
        ));
    }

    #[test]
    fn test_should_auto_backup_weekly_next_week_true() {
        let last = "2026-05-04T10:00:00+08:00"; // 周一
        let now = "2026-05-11T10:00:00+08:00"; // 下周一
        assert!(should_auto_backup(
            Some(last),
            AutoBackupFrequency::Weekly,
            now
        ));
    }

    #[test]
    fn test_should_auto_backup_monthly_same_month_false() {
        let last = "2026-05-01T10:00:00+08:00";
        let now = "2026-05-31T10:00:00+08:00";
        assert!(!should_auto_backup(
            Some(last),
            AutoBackupFrequency::Monthly,
            now
        ));
    }

    #[test]
    fn test_should_auto_backup_monthly_next_month_true() {
        let last = "2026-05-31T10:00:00+08:00";
        let now = "2026-06-01T10:00:00+08:00";
        assert!(should_auto_backup(
            Some(last),
            AutoBackupFrequency::Monthly,
            now
        ));
    }

    #[test]
    fn test_should_auto_backup_none_last_time_always_true() {
        assert!(should_auto_backup(
            None,
            AutoBackupFrequency::Daily,
            "2026-05-07T10:00:00+08:00"
        ));
        assert!(should_auto_backup(
            None,
            AutoBackupFrequency::Weekly,
            "2026-05-07T10:00:00+08:00"
        ));
        assert!(should_auto_backup(
            None,
            AutoBackupFrequency::Monthly,
            "2026-05-07T10:00:00+08:00"
        ));
    }

    #[test]
    fn test_should_auto_backup_invalid_last_time_treated_as_none() {
        assert!(should_auto_backup(
            Some("not-a-date"),
            AutoBackupFrequency::Daily,
            "2026-05-07T10:00:00+08:00"
        ));
    }
}
