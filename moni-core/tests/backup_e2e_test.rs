mod common;

use std::io::Write;

/// 端到端备份测试：创建记录 → 导出 → 清空 → 导入 → 验证数据一致。
#[test]
fn test_backup_export_then_import_restores_data() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        // 1. 获取预设分类 ID 并创建记录
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let category_id = state["categories"][0]["id"].as_i64().unwrap();

        let intent = format!(
            r#"{{"type":"record_create","amount_cents":5678,"record_type":"expense","category_id":{category_id},"note":"备份测试","timestamp":null}}"#
        );
        let update = core.dispatch(intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let records_before = state["records"].as_array().unwrap().clone();
        let categories_before = state["categories"].as_array().unwrap().clone();
        assert_eq!(records_before.len(), 1);

        // 2. 导出备份
        let tmp_dir = std::env::temp_dir().join("moni_test_e2e");
        let _ = std::fs::create_dir_all(&tmp_dir);
        let out_zip = tmp_dir.join("test_backup.zip");

        let report = core
            .backup_export(
                out_zip.to_str().unwrap().to_string(),
                r#"{"schema":1,"currency_symbol":"¥","theme_mode":"system","dynamic_color":false,"seed_color":4284612842}"#.to_string(),
                "0.1.0-test".to_string(),
                1,
                "Test".to_string(),
                "TestDevice".to_string(),
                36,
                None,
            )
            .await
            .unwrap();

        assert_eq!(report.record_count, 1);
        assert_eq!(report.category_count, categories_before.len() as u64);
        assert!(report.total_bytes > 0);
        assert!(out_zip.exists());

        // 3. 清空数据库（模拟恢复目标环境）
        let db_path = tmp_dir.join("moni_target.db");
        {
            let conn = rusqlite::Connection::open(&db_path).unwrap();
            moni_core::db::schema::init_schema(&conn).unwrap();
            conn.execute("DELETE FROM records", []).unwrap();
            conn.execute("DELETE FROM categories", []).unwrap();
        }

        // 4. 导入备份到目标数据库
        let restore_report = core
            .backup_restore(
                out_zip.to_str().unwrap().to_string(),
                db_path.to_str().unwrap().to_string(),
                None,
            )
            .await
            .unwrap();

        assert_eq!(restore_report.restored_record_count, 1);
        assert_eq!(
            restore_report.restored_category_count,
            categories_before.len() as u64
        );

        // 5. 验证导入后的数据库内容
        let conn = rusqlite::Connection::open(&db_path).unwrap();
        let record_count: u64 = conn
            .query_row("SELECT COUNT(*) FROM records", [], |row| row.get(0))
            .unwrap();
        let category_count: u64 = conn
            .query_row("SELECT COUNT(*) FROM categories", [], |row| row.get(0))
            .unwrap();
        assert_eq!(record_count, 1);
        assert_eq!(category_count, categories_before.len() as u64);

        let amount: i64 = conn
            .query_row(
                "SELECT amount_cents FROM records LIMIT 1",
                [],
                |row| row.get(0),
            )
            .unwrap();
        assert_eq!(amount, 5678);

        // 6. 清理
        let _ = std::fs::remove_file(&out_zip);
        let _ = std::fs::remove_file(&db_path);
        let _ = std::fs::remove_dir(&tmp_dir);
    });
}

/// 测试导入损坏的 ZIP 应失败。
#[test]
fn test_backup_import_corrupted_zip_fails() {
    let core = common::setup_core();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let tmp_dir = std::env::temp_dir().join("moni_test_corrupt");
        let _ = std::fs::create_dir_all(&tmp_dir);
        let bad_zip = tmp_dir.join("bad.zip");

        // 写入一个不是 ZIP 的文件
        std::fs::write(&bad_zip, b"this is not a zip").unwrap();

        let db_path = tmp_dir.join("moni.db");
        {
            let conn = rusqlite::Connection::open(&db_path).unwrap();
            moni_core::db::schema::init_schema(&conn).unwrap();
        }

        let result = core
            .backup_restore(
                bad_zip.to_str().unwrap().to_string(),
                db_path.to_str().unwrap().to_string(),
                None,
            )
            .await;

        assert!(result.is_err());

        let _ = std::fs::remove_file(&bad_zip);
        let _ = std::fs::remove_file(&db_path);
        let _ = std::fs::remove_dir(&tmp_dir);
    });
}

/// 测试备份文件完整性校验能检测篡改。
#[test]
fn test_backup_manifest_integrity_detects_tampering() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let tmp_dir = std::env::temp_dir().join("moni_test_tamper");
        let _ = std::fs::create_dir_all(&tmp_dir);
        let out_zip = tmp_dir.join("tamper.zip");

        // 导出正常备份
        core.backup_export(
            out_zip.to_str().unwrap().to_string(),
            r#"{"schema":1,"currency_symbol":"¥","theme_mode":"light","dynamic_color":false,"seed_color":4284612842}"#.to_string(),
            "0.1.0".to_string(),
            1,
            "Test".to_string(),
            "TestDevice".to_string(),
            36,
            None,
        )
        .await
        .unwrap();

        // 篡改 ZIP：替换 db/moni.db 的内容
        {
            let file = std::fs::File::open(&out_zip).unwrap();
            let mut zip_in = zip::ZipArchive::new(file).unwrap();
            let tmp_out = tmp_dir.join("tampered.zip");
            let file_out = std::fs::File::create(&tmp_out).unwrap();
            let mut zip_out = zip::ZipWriter::new(file_out);

            for i in 0..zip_in.len() {
                let mut entry = zip_in.by_index(i).unwrap();
                let name = entry.name().to_string();
                let mut content = Vec::new();
                std::io::copy(&mut entry, &mut content).unwrap();

                if name == "db/moni.db" {
                    content.extend_from_slice(b"TAMPERED");
                }

                let options = zip::write::FileOptions::<'_, ()>::default()
                    .compression_method(zip::CompressionMethod::Deflated)
                    .compression_level(Some(9));
                zip_out.start_file(name, options).unwrap();
                zip_out.write_all(&content).unwrap();
            }
            zip_out.finish().unwrap();
            std::fs::rename(&tmp_out, &out_zip).unwrap();
        }

        let db_path = tmp_dir.join("moni.db");
        {
            let conn = rusqlite::Connection::open(&db_path).unwrap();
            moni_core::db::schema::init_schema(&conn).unwrap();
        }

        // 导入应因校验失败而报错
        let result = core
            .backup_restore(
                out_zip.to_str().unwrap().to_string(),
                db_path.to_str().unwrap().to_string(),
                None,
            )
            .await;

        assert!(result.is_err(), "篡改后的备份应校验失败");

        let _ = std::fs::remove_file(&out_zip);
        let _ = std::fs::remove_file(&db_path);
        let _ = std::fs::remove_dir(&tmp_dir);
    });
}
