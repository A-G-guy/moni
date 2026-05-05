mod common;

use std::io::{Read, Write};

use moni_core::domain::backup::manifest::{compute_manifest_sha256, BackupManifest};

/// 构造 manifest.stats 与实际数据库行数不一致的备份包，验证恢复阶段会触发 `db_validation` 行数校验失败路径。
///
/// 该测试覆盖 `importer.rs::validate_restored_db` 的行数不匹配分支：当 manifest 声明的
/// 记录数 / 分类数与 ZIP 中数据库实际查询到的行数不一致时，应返回
/// `BackupRestoreFailed { stage: "validation", reason: ... }`，且 reason 中应包含具体差异。
#[test]
fn test_backup_restore_detects_row_count_mismatch() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let tmp_dir = std::env::temp_dir().join("moni_test_row_mismatch");
        let _ = std::fs::create_dir_all(&tmp_dir);
        let original_zip = tmp_dir.join("original.zip");
        let tampered_zip = tmp_dir.join("tampered.zip");

        // 1. 先创建一些记录确保 db 非空，再正常导出
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let category_id = state["categories"][0]["id"].as_i64().unwrap();
        let intent = format!(
            r#"{{"type":"record_create","amount_cents":1000,"record_type":"expense","category_id":{category_id},"note":"row_mismatch_test","timestamp":null}}"#
        );
        core.dispatch(intent).await.unwrap();

        core.backup_export(
            original_zip.to_str().unwrap().to_string(),
            r#"{"schema":1,"currency_symbol":"¥","theme_mode":"system","dynamic_color":false,"seed_color":4284612842}"#.to_string(),
            "0.1.0-mismatch".to_string(),
            1,
            "Test".to_string(),
            "TestDevice".to_string(),
            36,
            None,
        )
        .await
        .expect("正常导出应成功");

        // 2. 读取原备份的全部条目（保留 db/moni.db、settings、README 原内容与校验和），仅改写 manifest
        let mut original_entries: Vec<(String, Vec<u8>)> = Vec::new();
        let mut original_manifest: Option<BackupManifest> = None;
        {
            let file = std::fs::File::open(&original_zip).unwrap();
            let mut zip = zip::ZipArchive::new(file).unwrap();
            for i in 0..zip.len() {
                let mut entry = zip.by_index(i).unwrap();
                let name = entry.name().to_string();
                let mut buf = Vec::new();
                entry.read_to_end(&mut buf).unwrap();
                if name == "manifest.json" {
                    original_manifest =
                        Some(serde_json::from_slice(&buf).expect("manifest.json 应可解析"));
                } else {
                    original_entries.push((name, buf));
                }
            }
        }
        let mut manifest = original_manifest.expect("备份必须包含 manifest.json");

        // 3. 故意把 record_count 调到和实际不符（实际 = 1，故意写 999）
        let real_record_count = manifest.stats.record_count;
        assert_eq!(real_record_count, 1, "前置条件：实际记录数应为 1");
        manifest.stats.record_count = 999;
        // 重新计算 manifest 自身指纹，以**确保 manifest 完整性校验通过**——这样错误才能真正暴露在 db_validation 阶段，而不是被 `verify_manifest_integrity` 拦在前面。
        manifest.manifest_sha256 =
            compute_manifest_sha256(&manifest).expect("重算 manifest_sha256 应成功");

        // 4. 写出篡改后的 ZIP：保留所有原始条目 + 替换后的 manifest.json
        let tampered_manifest_bytes =
            serde_json::to_vec_pretty(&manifest).expect("manifest 序列化应成功");
        {
            let file = std::fs::File::create(&tampered_zip).unwrap();
            let mut zip = zip::ZipWriter::new(file);
            let options = zip::write::FileOptions::<'_, ()>::default()
                .compression_method(zip::CompressionMethod::Deflated)
                .compression_level(Some(9));
            for (name, buf) in &original_entries {
                zip.start_file(name.clone(), options).unwrap();
                zip.write_all(buf).unwrap();
            }
            zip.start_file("manifest.json", options).unwrap();
            zip.write_all(&tampered_manifest_bytes).unwrap();
            zip.finish().unwrap();
        }

        // 5. 准备目标 db 路径
        let target_db = tmp_dir.join("moni_target.db");
        {
            let conn = rusqlite::Connection::open(&target_db).unwrap();
            moni_core::db::schema::init_schema(&conn).unwrap();
        }

        // 6. 触发恢复 —— 应在行数校验阶段失败
        let err = core
            .backup_restore(
                tampered_zip.to_str().unwrap().to_string(),
                target_db.to_str().unwrap().to_string(),
                None,
            )
            .await
            .expect_err("manifest.stats 与 db 行数不匹配时应失败");

        let err_msg = format!("{err:?}");
        assert!(
            err_msg.contains("validation") || err_msg.contains("行数不匹配"),
            "错误应来自 validation 阶段或包含\"行数不匹配\"，实际: {err_msg}"
        );
        assert!(
            err_msg.contains("999") || err_msg.contains("1"),
            "错误信息应包含具体的行数对比，实际: {err_msg}"
        );

        // 7. 清理
        let _ = std::fs::remove_file(&original_zip);
        let _ = std::fs::remove_file(&tampered_zip);
        let _ = std::fs::remove_file(&target_db);
        let _ = std::fs::remove_dir(&tmp_dir);
    });
}
