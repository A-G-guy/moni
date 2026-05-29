use rusqlite::Connection;
use std::fs::File;
use std::io::Read;

use crate::db::schema::{self, CURRENT_SCHEMA_VERSION};
use crate::domain::backup::manifest::{
    read_manifest, validate_format_version, verify_manifest_integrity,
};
use crate::domain::backup::migrations::apply_migrations;
use crate::models::backup::BackupRestoreReport;

const DB_ENTRY_PATH: &str = "db/moni.db";
const SETTINGS_ENTRY_PATH: &str = "settings/preferences.json";

/// 校验 ZIP 内指定条目的 SHA-256 与 manifest 中的指纹是否匹配。
fn verify_file_checksum<R: Read + std::io::Seek>(
    zip: &mut zip::ZipArchive<R>,
    expected_path: &str,
    expected_sha256: &str,
) -> Result<(), crate::core::error::CoreError> {
    let mut entry = zip.by_name(expected_path).map_err(|e| {
        crate::core::error::CoreError::BackupCorrupted(format!(
            "ZIP 中缺少条目 {expected_path}: {e}"
        ))
    })?;

    let actual = crate::domain::backup::manifest::compute_sha256_hex(&mut entry)
        .map_err(|e| crate::core::error::CoreError::BackupIo(e.to_string()))?;

    if actual != expected_sha256 {
        return Err(crate::core::error::CoreError::BackupCorrupted(format!(
            "{expected_path} 校验失败: 期望 {expected_sha256}, 实际 {actual}"
        )));
    }
    Ok(())
}

/// 从 ZIP 解压指定条目到目标路径。
fn extract_entry<R: Read + std::io::Seek>(
    zip: &mut zip::ZipArchive<R>,
    entry_name: &str,
    dest_path: &str,
) -> Result<(), crate::core::error::CoreError> {
    let mut entry = zip
        .by_name(entry_name)
        .map_err(|e| crate::core::error::CoreError::BackupZipError(e.to_string()))?;
    let mut out = File::create(dest_path)
        .map_err(|e| crate::core::error::CoreError::BackupIo(e.to_string()))?;
    std::io::copy(&mut entry, &mut out)
        .map_err(|e| crate::core::error::CoreError::BackupIo(e.to_string()))?;
    Ok(())
}

/// 验证解压后的数据库基本完整性（能打开、有预期表、行数合理）。
#[doc(hidden)]
pub fn validate_restored_db(
    db_path: &str,
    expected_record_count: u64,
    expected_category_count: u64,
    expected_budget_count: Option<u64>,
) -> Result<(), crate::core::error::CoreError> {
    let conn = Connection::open(db_path).map_err(|e| {
        crate::core::error::CoreError::Database(format!("恢复后的数据库无法打开: {e}"))
    })?;

    let actual_records: u64 = conn
        .query_row("SELECT COUNT(*) FROM records", [], |row| row.get(0))
        .map_err(|e| {
            crate::core::error::CoreError::Database(format!("恢复后 records 表查询失败: {e}"))
        })?;
    let actual_categories: u64 = conn
        .query_row("SELECT COUNT(*) FROM categories", [], |row| row.get(0))
        .map_err(|e| {
            crate::core::error::CoreError::Database(format!("恢复后 categories 表查询失败: {e}"))
        })?;

    if actual_records != expected_record_count || actual_categories != expected_category_count {
        return Err(crate::core::error::CoreError::BackupRestoreFailed {
            stage: "db_validation".to_string(),
            reason: format!(
                "恢复后行数不匹配: records {actual_records}/{expected_record_count}, categories {actual_categories}/{expected_category_count}"
            ),
        });
    }

    // budgets 校验仅在 manifest 包含 budget_count 时执行（兼容旧备份）
    if let Some(expected) = expected_budget_count {
        let actual_budgets: u64 = conn
            .query_row("SELECT COUNT(*) FROM budgets", [], |row| row.get(0))
            .map_err(|e| {
                crate::core::error::CoreError::Database(format!("恢复后 budgets 表查询失败: {e}"))
            })?;
        if actual_budgets != expected {
            return Err(crate::core::error::CoreError::BackupRestoreFailed {
                stage: "db_validation".to_string(),
                reason: format!("恢复后 budgets 行数不匹配: {actual_budgets}/{expected}"),
            });
        }
    }

    Ok(())
}

/// 读取 ZIP 中 settings JSON 内容。
fn read_settings_json<R: Read + std::io::Seek>(
    zip: &mut zip::ZipArchive<R>,
) -> Result<String, crate::core::error::CoreError> {
    let mut entry = zip
        .by_name(SETTINGS_ENTRY_PATH)
        .map_err(|e| crate::core::error::CoreError::BackupZipError(e.to_string()))?;
    let mut content = String::new();
    entry
        .read_to_string(&mut content)
        .map_err(|e| crate::core::error::CoreError::BackupIo(e.to_string()))?;
    Ok(content)
}

/// 执行全量恢复。
/// `pre_restore_snapshot_path` 是恢复前快照路径，替换数据库失败时必须依赖它回滚。
pub fn backup_restore(
    in_zip_path: &str,
    db_path: &str,
    pre_restore_snapshot_path: Option<&str>,
    on_progress: Option<&dyn Fn(&str, i32)>,
) -> Result<BackupRestoreReport, crate::core::error::CoreError> {
    let snapshot_path = pre_restore_snapshot_path.ok_or_else(|| {
        crate::core::error::CoreError::BackupRestoreFailed {
            stage: "prepare".to_string(),
            reason: "缺少恢复前快照，拒绝执行非原子替换".to_string(),
        }
    })?;

    if let Some(cb) = on_progress {
        cb("打开备份文件...", 5);
    }
    let file = File::open(in_zip_path)
        .map_err(|e| crate::core::error::CoreError::BackupIo(format!("打开备份 ZIP 失败: {e}")))?;
    let mut zip = zip::ZipArchive::new(file)
        .map_err(|e| crate::core::error::CoreError::BackupZipError(e.to_string()))?;

    // 1. 读取并校验 manifest
    if let Some(cb) = on_progress {
        cb("读取清单...", 15);
    }
    let manifest = read_manifest(&mut zip)?;
    validate_format_version(manifest.format_version)?;
    verify_manifest_integrity(&manifest)?;

    // 2. 逐文件 SHA-256 校验
    if let Some(cb) = on_progress {
        cb("校验文件完整性...", 30);
    }
    for fp in &manifest.files {
        if fp.path == "manifest.json" {
            continue;
        }
        verify_file_checksum(&mut zip, &fp.path, &fp.sha256)?;
    }

    // 3. 解压数据库到本次恢复专属临时位置。
    if let Some(cb) = on_progress {
        cb("解压数据库...", 50);
    }
    let tmp_dir = std::env::temp_dir().join(format!("moni_restore_{}", uuid::Uuid::new_v4()));
    std::fs::create_dir_all(&tmp_dir).map_err(|e| {
        crate::core::error::CoreError::BackupIo(format!("创建恢复临时目录失败: {e}"))
    })?;
    let tmp_db = tmp_dir.join("moni_restore.db");
    extract_entry(
        &mut zip,
        DB_ENTRY_PATH,
        tmp_db.to_str().unwrap_or("moni_restore.db"),
    )?;

    // 4. 打开临时数据库，运行迁移
    if let Some(cb) = on_progress {
        cb("迁移数据...", 65);
    }
    let tmp_conn = Connection::open(&tmp_db)
        .map_err(|e| crate::core::error::CoreError::Database(format!("打开恢复数据库失败: {e}")))?;
    schema::init_schema(&tmp_conn).map_err(|e| {
        crate::core::error::CoreError::Database(format!("恢复数据库 schema 初始化失败: {e}"))
    })?;
    apply_migrations(&tmp_conn, manifest.schema_version, CURRENT_SCHEMA_VERSION)?;
    drop(tmp_conn);

    // 5. 验证行数
    if let Some(cb) = on_progress {
        cb("验证数据...", 80);
    }
    let result = validate_restored_db(
        tmp_db.to_str().unwrap_or(""),
        manifest.stats.record_count,
        manifest.stats.category_count,
        manifest.stats.budget_count,
    );
    if let Err(e) = result {
        let _ = std::fs::remove_file(&tmp_db);
        let _ = std::fs::remove_dir_all(&tmp_dir);
        return Err(crate::core::error::CoreError::BackupRestoreFailed {
            stage: "validation".to_string(),
            reason: e.to_string(),
        });
    }

    // 6. 原子替换（rename 在跨文件系统时可能失败，回退到 copy + remove）
    if let Some(cb) = on_progress {
        cb("应用恢复...", 90);
    }
    if let Err(e) = std::fs::rename(&tmp_db, db_path) {
        log::warn!("rename 跨文件系统失败，回退到 copy+remove: {e}");
        if let Err(copy_err) = std::fs::copy(&tmp_db, db_path) {
            if let Err(rollback_err) = std::fs::copy(snapshot_path, db_path) {
                log::error!("恢复替换失败后回滚快照失败: {rollback_err}");
            }
            let _ = std::fs::remove_file(&tmp_db);
            let _ = std::fs::remove_dir_all(&tmp_dir);
            return Err(crate::core::error::CoreError::BackupRestoreFailed {
                stage: "replace".to_string(),
                reason: format!("rename 失败且 copy 也失败: {copy_err}"),
            });
        }
        let _ = std::fs::remove_file(&tmp_db);
    }

    // 7. 提取 settings JSON
    let settings_json = read_settings_json(&mut zip)?;

    // 8. 清理临时文件
    let _ = std::fs::remove_file(&tmp_db);
    let _ = std::fs::remove_dir_all(&tmp_dir);
    if let Some(cb) = on_progress {
        cb("完成", 100);
    }

    Ok(BackupRestoreReport {
        restored_record_count: manifest.stats.record_count,
        restored_category_count: manifest.stats.category_count,
        restored_settings_count: manifest.stats.settings_count,
        settings_json,
    })
}
