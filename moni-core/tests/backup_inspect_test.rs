mod common;

use std::sync::{Arc, Mutex};

use moni_core::models::backup::BackupProgressListener;

/// 测试用的进度监听器：把 (stage, percent) 元组顺序记录到共享 Vec 里，便于断言。
struct RecordingProgressListener {
    log: Arc<Mutex<Vec<(String, i32)>>>,
}

impl BackupProgressListener for RecordingProgressListener {
    fn on_stage(&self, stage: String, percent: i32) {
        self.log.lock().unwrap().push((stage, percent));
    }
}

/// 验证 `backup_inspect` 在正常 ZIP 上能读出 manifest 的字段。
#[test]
fn test_backup_inspect_returns_manifest_summary() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let tmp_dir = std::env::temp_dir().join("moni_test_inspect");
        let _ = std::fs::create_dir_all(&tmp_dir);
        let out_zip = tmp_dir.join("inspect.zip");

        let export_report = core
            .backup_export(
                out_zip.to_str().unwrap().to_string(),
                r#"{"schema":1,"currency_symbol":"¥","theme_mode":"system","dynamic_color":false,"seed_color":4284612842}"#.to_string(),
                "1.2.3".to_string(),
                42,
                "InspectVendor".to_string(),
                "InspectModel".to_string(),
                36,
                None,
            )
            .await
            .expect("导出应成功");

        // 调用 backup_inspect 应能读出与导出一致的概览
        let inspection = core
            .backup_inspect(out_zip.to_str().unwrap().to_string())
            .await
            .expect("inspect 应成功");

        assert_eq!(inspection.app_version_name, "1.2.3");
        assert_eq!(inspection.app_version_code, 42);
        assert_eq!(inspection.format_version, 1);
        assert_eq!(inspection.record_count, export_report.record_count);
        assert_eq!(inspection.category_count, export_report.category_count);
        assert_eq!(inspection.settings_count, export_report.settings_count);
        // total_bytes 来自文件元信息，应当 > 0 且与导出报告一致
        assert!(inspection.total_bytes > 0);
        assert_eq!(inspection.total_bytes, export_report.total_bytes);
        assert!(!inspection.created_at.is_empty(), "created_at 不应为空");

        let _ = std::fs::remove_file(&out_zip);
        let _ = std::fs::remove_dir(&tmp_dir);
    });
}

/// 验证 `backup_inspect` 在路径不存在时返回 `BackupIo` 错误。
#[test]
fn test_backup_inspect_missing_file_errors() {
    let core = common::setup_core();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let result = core
            .backup_inspect("/tmp/this_file_does_not_exist_moni_inspect.zip".to_string())
            .await;
        assert!(result.is_err(), "缺失文件应失败");
        let err_msg = format!("{:?}", result.unwrap_err());
        assert!(
            err_msg.contains("打开备份 ZIP 失败")
                || err_msg.contains("BackupIo")
                || err_msg.to_lowercase().contains("no such file"),
            "错误应来自 BackupIo 路径，实际: {err_msg}"
        );
    });
}

/// 验证 `backup_inspect` 在非 ZIP 文件上返回 `BackupZipError`。
#[test]
fn test_backup_inspect_invalid_zip_errors() {
    let core = common::setup_core();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let tmp_dir = std::env::temp_dir().join("moni_test_inspect_invalid");
        let _ = std::fs::create_dir_all(&tmp_dir);
        let bad = tmp_dir.join("not_a_zip.bin");
        std::fs::write(&bad, b"hello, not a zip at all").unwrap();

        let result = core.backup_inspect(bad.to_str().unwrap().to_string()).await;
        assert!(result.is_err(), "非 ZIP 文件应失败");

        let _ = std::fs::remove_file(&bad);
        let _ = std::fs::remove_dir(&tmp_dir);
    });
}

/// 验证 `backup_export` 与 `backup_restore` 在传入 progress listener 时会收到回调。
/// 同步覆盖 lib.rs 中 `if let Some(ref p) = progress { p.on_stage(...) }` 分支。
#[test]
fn test_backup_export_and_restore_invoke_progress_listener() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let tmp_dir = std::env::temp_dir().join("moni_test_progress");
        let _ = std::fs::create_dir_all(&tmp_dir);
        let out_zip = tmp_dir.join("with_progress.zip");

        // 导出阶段：传入 progress listener
        let export_log: Arc<Mutex<Vec<(String, i32)>>> = Arc::new(Mutex::new(Vec::new()));
        let export_listener = RecordingProgressListener {
            log: Arc::clone(&export_log),
        };
        core.backup_export(
            out_zip.to_str().unwrap().to_string(),
            r#"{"schema":1,"currency_symbol":"¥","theme_mode":"system","dynamic_color":false,"seed_color":4284612842}"#.to_string(),
            "0.1.0-progress".to_string(),
            1,
            "Test".to_string(),
            "TestDevice".to_string(),
            36,
            Some(Box::new(export_listener)),
        )
        .await
        .expect("导出应成功");

        let captured_export = export_log.lock().unwrap().clone();
        assert!(!captured_export.is_empty(), "导出过程应至少回调一次");
        assert!(
            captured_export.iter().any(|(_, p)| *p == 100),
            "导出最后阶段 percent 应到达 100，实际: {captured_export:?}"
        );

        // 恢复阶段：使用 progress listener
        let target_db = tmp_dir.join("moni_target.db");
        {
            let conn = rusqlite::Connection::open(&target_db).unwrap();
            moni_core::db::schema::init_schema(&conn).unwrap();
        }
        let restore_log: Arc<Mutex<Vec<(String, i32)>>> = Arc::new(Mutex::new(Vec::new()));
        let restore_listener = RecordingProgressListener {
            log: Arc::clone(&restore_log),
        };
        core.backup_restore(
            out_zip.to_str().unwrap().to_string(),
            target_db.to_str().unwrap().to_string(),
            Some(Box::new(restore_listener)),
        )
        .await
        .expect("恢复应成功");

        let captured_restore = restore_log.lock().unwrap().clone();
        assert!(!captured_restore.is_empty(), "恢复过程应至少回调一次");
        assert!(
            captured_restore.iter().any(|(_, p)| *p == 100),
            "恢复最后阶段 percent 应到达 100，实际: {captured_restore:?}"
        );

        let _ = std::fs::remove_file(&out_zip);
        let _ = std::fs::remove_file(&target_db);
        let _ = std::fs::remove_dir(&tmp_dir);
    });
}
