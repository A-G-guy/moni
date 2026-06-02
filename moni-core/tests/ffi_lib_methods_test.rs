mod common;

use std::io::Write;

use moni_core::MoniCore;
use moni_core::core::error::CoreError;

/// 覆盖 `MoniCore` FFI 入口中尚未被已有测试命中的正常路径与错误路径。
///
/// 重点：
/// - `initialize()` / `initialize_with_db()` 直接调用及文件型数据库路径。
/// - `snapshot_json()` 在未初始化与已初始化两种状态下的行为。
/// - `dispatch()` 传入非法 JSON 时的解析错误分支。
/// - `backup_export()` 在目标目录不存在时的 `BackupIo` 错误。
/// - `backup_inspect()` 在空文件、缺少 manifest 的 ZIP 上的错误。
/// - `backup_restore()` 在空文件上的错误。
/// - `backup_export` -> `backup_inspect` -> `backup_restore` 完整链路。

/// 验证 `initialize()` 直接调用后状态可正常序列化。
#[test]
fn test_initialize_direct_path() {
    let core = MoniCore::new();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let update = core.initialize().await.expect("initialize 应成功");
        assert!(!update.state_json.is_empty(), "state_json 不应为空");

        let snapshot = core.snapshot_json().await.expect("snapshot_json 应成功");
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        assert_eq!(state["ui"]["activeTab"], "records");
        assert!(state["records"].as_array().unwrap().is_empty());
    });
}

/// 验证 `initialize_with_db` 使用真实文件路径时，数据可持久化到磁盘。
#[test]
fn test_initialize_with_file_db_persists_data() {
    let core = MoniCore::new();
    let rt = tokio::runtime::Runtime::new().unwrap();

    let tmp_dir = std::env::temp_dir().join(format!("moni_test_file_db_{}", uuid::Uuid::new_v4()));
    let _ = std::fs::create_dir_all(&tmp_dir);
    let db_path = tmp_dir.join("moni_persist.db");
    // 清理可能残留的同名文件（防御性）
    let _ = std::fs::remove_file(&db_path);

    rt.block_on(async {
        core.initialize_with_db(db_path.to_str().unwrap().to_string())
            .await
            .expect("initialize_with_db 应成功");

        // 填充预设
        core.dispatch(r#"{"type":"dev_seed_presets"}"#.to_string())
            .await
            .expect("填充预设应成功");

        // 创建记录
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let category_id = state["categories"][0]["id"].as_i64().unwrap();

        let intent = format!(
            r#"{{"type":"record_create","amount_cents":999,"record_type":"expense","category_id":{category_id},"note":"文件库","timestamp":null}}"#
        );
        core.dispatch(intent).await.unwrap();
    });

    // 重新打开同一文件，验证数据仍在
    let core2 = MoniCore::new();
    rt.block_on(async {
        core2
            .initialize_with_db(db_path.to_str().unwrap().to_string())
            .await
            .expect("二次打开应成功");

        // initialize_with_db 仅加载 categories 到 state，不加载 records，
        // 因此直接查询数据库验证持久化。
        let conn = rusqlite::Connection::open(&db_path).unwrap();
        let count: i64 = conn
            .query_row("SELECT COUNT(*) FROM records", [], |row| row.get(0))
            .unwrap();
        assert_eq!(count, 1, "文件数据库应保留 1 条记录");

        let amount: i64 = conn
            .query_row("SELECT amount_cents FROM records LIMIT 1", [], |row| {
                row.get(0)
            })
            .unwrap();
        assert_eq!(amount, 999);

        let note: String = conn
            .query_row("SELECT note FROM records LIMIT 1", [], |row| row.get(0))
            .unwrap();
        assert_eq!(note, "文件库");
    });

    let _ = std::fs::remove_file(&db_path);
    let _ = std::fs::remove_dir(&tmp_dir);
}

/// 验证 `snapshot_json` 在 `new()` 后、未调用 `initialize` 时仍可返回合法 JSON。
#[test]
fn test_snapshot_json_before_initialize() {
    let core = MoniCore::new();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.expect("snapshot_json 应成功");
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        assert!(state.is_object());
        assert!(state["records"].as_array().is_some());
        assert!(state["categories"].as_array().is_some());
    });
}

/// 验证 `dispatch` 传入非 JSON 时返回 `CoreError::Internal` 并包含"意图解析失败"。
#[test]
fn test_dispatch_malformed_json_returns_internal_error() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let result = core.dispatch("not_a_json_at_all".to_string()).await;
        assert!(result.is_err(), "非法 JSON 应失败");

        let err = result.unwrap_err();
        let err_str = format!("{err}");
        assert!(
            err_str.contains("意图解析失败"),
            "错误应包含'意图解析失败'，实际: {err_str}"
        );
    });
}

/// 验证 `backup_export` 在目标目录不存在时返回 `CoreError::BackupIo`。
#[test]
fn test_backup_export_to_nonexistent_dir_fails() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let bad_path = format!(
            "/tmp/nonexistent_moni_dir_{}/export.zip",
            uuid::Uuid::new_v4()
        );
        let result = core
            .backup_export(
                bad_path,
                r#"{}"#.to_string(),
                "0.0.1".to_string(),
                1,
                "Test".to_string(),
                "TestDevice".to_string(),
                36,
                None,
            )
            .await;

        assert!(result.is_err(), "目录不存在应失败");
        let err = result.unwrap_err();
        match err {
            CoreError::BackupIo(_) => {}
            _ => panic!("期望 BackupIo，实际: {err:?}"),
        }
    });
}

/// 验证 `backup_inspect` 在缺少 manifest.json 的 ZIP 上返回 `BackupManifestInvalid`。
#[test]
fn test_backup_inspect_missing_manifest() {
    let core = common::setup_core();
    let rt = tokio::runtime::Runtime::new().unwrap();

    let tmp_dir =
        std::env::temp_dir().join(format!("moni_test_no_manifest_{}", uuid::Uuid::new_v4()));
    let _ = std::fs::create_dir_all(&tmp_dir);
    let zip_path = tmp_dir.join("no_manifest.zip");

    {
        let file = std::fs::File::create(&zip_path).unwrap();
        let mut zip = zip::ZipWriter::new(file);
        let options = zip::write::FileOptions::<'_, ()>::default()
            .compression_method(zip::CompressionMethod::Deflated);
        zip.start_file("readme.txt", options).unwrap();
        zip.write_all(b"no manifest here").unwrap();
        zip.finish().unwrap();
    }

    rt.block_on(async {
        let result = core
            .backup_inspect(zip_path.to_str().unwrap().to_string())
            .await;
        assert!(result.is_err(), "缺少 manifest 应失败");
        let err = result.unwrap_err();
        match err {
            CoreError::BackupManifestInvalid(_) => {}
            _ => panic!("期望 BackupManifestInvalid，实际: {err:?}"),
        }
    });

    let _ = std::fs::remove_file(&zip_path);
    let _ = std::fs::remove_dir(&tmp_dir);
}

/// 验证 `backup_inspect` 在空文件（0 字节）上返回 `BackupZipError`。
#[test]
fn test_backup_inspect_empty_file_fails() {
    let core = common::setup_core();
    let rt = tokio::runtime::Runtime::new().unwrap();

    let tmp_dir = std::env::temp_dir().join(format!("moni_test_empty_{}", uuid::Uuid::new_v4()));
    let _ = std::fs::create_dir_all(&tmp_dir);
    let empty_zip = tmp_dir.join("empty.zip");
    std::fs::write(&empty_zip, b"").unwrap();

    rt.block_on(async {
        let result = core
            .backup_inspect(empty_zip.to_str().unwrap().to_string())
            .await;
        assert!(result.is_err(), "空文件应失败");
        let err = result.unwrap_err();
        match err {
            CoreError::BackupZipError(_) => {}
            _ => panic!("期望 BackupZipError，实际: {err:?}"),
        }
    });

    let _ = std::fs::remove_file(&empty_zip);
    let _ = std::fs::remove_dir(&tmp_dir);
}

/// 验证 `backup_restore` 在空文件上返回错误。
#[test]
fn test_backup_restore_empty_file_fails() {
    let core = common::setup_core();
    let rt = tokio::runtime::Runtime::new().unwrap();

    let tmp_dir =
        std::env::temp_dir().join(format!("moni_test_restore_empty_{}", uuid::Uuid::new_v4()));
    let _ = std::fs::create_dir_all(&tmp_dir);
    let empty_zip = tmp_dir.join("empty.zip");
    std::fs::write(&empty_zip, b"").unwrap();

    let db_path = tmp_dir.join("target.db");
    {
        let conn = rusqlite::Connection::open(&db_path).unwrap();
        moni_core::db::schema::init_schema(&conn).unwrap();
    }

    rt.block_on(async {
        let result = core
            .backup_restore(
                empty_zip.to_str().unwrap().to_string(),
                db_path.to_str().unwrap().to_string(),
                None,
            )
            .await;
        assert!(result.is_err(), "空 ZIP 恢复应失败");
    });

    let _ = std::fs::remove_file(&empty_zip);
    let _ = std::fs::remove_file(&db_path);
    let _ = std::fs::remove_dir(&tmp_dir);
}

/// 验证 `backup_export` -> `backup_inspect` -> `backup_restore` 完整链路在文件数据库上工作正常。
/// 覆盖 `backup_export` 问号传播的正常路径以及 `backup_inspect` 的 `read_manifest` 正常路径。
#[test]
fn test_backup_roundtrip_with_file_db() {
    let core = MoniCore::new();
    let rt = tokio::runtime::Runtime::new().unwrap();

    let tmp_dir =
        std::env::temp_dir().join(format!("moni_test_roundtrip_{}", uuid::Uuid::new_v4()));
    let _ = std::fs::create_dir_all(&tmp_dir);
    let db_path = tmp_dir.join("moni_roundtrip.db");
    let out_zip = tmp_dir.join("roundtrip.zip");

    rt.block_on(async {
        // 1. 初始化文件数据库并填充数据
        core.initialize_with_db(db_path.to_str().unwrap().to_string())
            .await
            .unwrap();
        core.dispatch(r#"{"type":"dev_seed_presets"}"#.to_string())
            .await
            .unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let category_id = state["categories"][0]["id"].as_i64().unwrap();

        let intent = format!(
            r#"{{"type":"record_create","amount_cents":7777,"record_type":"expense","category_id":{category_id},"note":"roundtrip","timestamp":null}}"#
        );
        core.dispatch(intent).await.unwrap();

        // 2. 导出
        let export_report = core
            .backup_export(
                out_zip.to_str().unwrap().to_string(),
                r#"{"schema":1,"currency_symbol":"¥"}"#.to_string(),
                "1.0.0".to_string(),
                10,
                "Roundtrip".to_string(),
                "Device".to_string(),
                36,
                None,
            )
            .await
            .expect("导出应成功");
        assert!(export_report.total_bytes > 0);

        // 3. inspect 应与导出报告一致
        let inspection = core
            .backup_inspect(out_zip.to_str().unwrap().to_string())
            .await
            .expect("inspect 应成功");
        assert_eq!(inspection.app_version_name, "1.0.0");
        assert_eq!(inspection.app_version_code, 10);
        assert_eq!(inspection.record_count, export_report.record_count);
        assert_eq!(inspection.total_bytes, export_report.total_bytes);

        // 4. 恢复到新数据库
        let restore_db = tmp_dir.join("moni_restored.db");
        {
            let conn = rusqlite::Connection::open(&restore_db).unwrap();
            moni_core::db::schema::init_schema(&conn).unwrap();
        }
        let restore_report = core
            .backup_restore(
                out_zip.to_str().unwrap().to_string(),
                restore_db.to_str().unwrap().to_string(),
                None,
            )
            .await
            .expect("恢复应成功");
        assert_eq!(restore_report.restored_record_count, export_report.record_count);
        assert_eq!(
            restore_report.restored_category_count,
            export_report.category_count
        );

        // 5. 验证恢复后的数据库内容
        let conn = rusqlite::Connection::open(&restore_db).unwrap();
        let amount: i64 = conn
            .query_row("SELECT amount_cents FROM records LIMIT 1", [], |row| row.get(0))
            .unwrap();
        assert_eq!(amount, 7777);
    });

    let _ = std::fs::remove_file(&db_path);
    let _ = std::fs::remove_file(&out_zip);
    let _ = std::fs::remove_dir(&tmp_dir);
}
