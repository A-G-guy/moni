mod common;

use std::io::Write;

use moni_core::domain::backup::manifest::{
    BackupManifest, BackupStats, DeviceInfo, FileFingerprint, compute_manifest_sha256,
};

/// 计算字节数组的 SHA-256 十六进制字符串。
fn sha256_hex(data: &[u8]) -> String {
    use sha2::{Digest, Sha256};
    let mut hasher = Sha256::new();
    hasher.update(data);
    moni_core::domain::backup::manifest::bytes_to_hex(&hasher.finalize())
}

/// 构造一个最小合法的 manifest，用于在测试中按需篡改字段。
fn minimal_manifest() -> BackupManifest {
    BackupManifest {
        format_version: 1,
        schema_version: 1,
        app_version_name: "0.1.0".to_string(),
        app_version_code: 1,
        product_name: "Moni".to_string(),
        package_name: "com.agguy.moni".to_string(),
        created_at: "2026-05-06T12:00:00+08:00".to_string(),
        device: DeviceInfo {
            manufacturer: "Test".to_string(),
            model: "TestDevice".to_string(),
            android_sdk: 36,
        },
        stats: BackupStats {
            record_count: 0,
            category_count: 0,
            budget_count: Some(0),
            settings_count: 0,
        },
        files: vec![],
        manifest_sha256: String::new(),
    }
}

/// 将 manifest 和若干文件条目写入一个临时 ZIP 文件，返回路径。
/// `entries` 为 (zip 内路径, 内容字节) 的列表。
fn write_zip_with_entries(
    tmp_dir: &std::path::Path,
    manifest: &BackupManifest,
    entries: &[(String, Vec<u8>)],
) -> std::path::PathBuf {
    let zip_path = tmp_dir.join("test.zip");
    let file = std::fs::File::create(&zip_path).unwrap();
    let mut zip = zip::ZipWriter::new(file);
    let options = zip::write::FileOptions::<'_, ()>::default()
        .compression_method(zip::CompressionMethod::Stored);

    for (name, data) in entries {
        zip.start_file(name.clone(), options).unwrap();
        zip.write_all(data).unwrap();
    }

    // manifest.json 最后写入，且不参与 files 列表的指纹校验（importer 会 skip manifest.json）
    let manifest_json = serde_json::to_string_pretty(manifest).unwrap();
    zip.start_file("manifest.json", options).unwrap();
    zip.write_all(manifest_json.as_bytes()).unwrap();

    zip.finish().unwrap();
    zip_path
}

// ---------------------------------------------------------------------------
// 1. 文件不存在
// ---------------------------------------------------------------------------

/// 导入不存在的 ZIP 文件应返回 `BackupIo` 错误，且错误信息包含"打开备份 ZIP 失败"。
#[test]
fn test_backup_restore_missing_file() {
    let core = common::setup_core();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let result = core
            .backup_restore(
                "/tmp/this_file_does_not_exist_moni_9999.zip".to_string(),
                "/tmp/moni_target.db".to_string(),
                None,
            )
            .await;

        assert!(result.is_err(), "不存在的文件应失败");
        let err_msg = format!("{:?}", result.unwrap_err());
        assert!(
            err_msg.contains("打开备份 ZIP 失败") || err_msg.contains("BackupIo"),
            "错误应来自文件打开阶段，实际: {err_msg}"
        );
    });
}

// ---------------------------------------------------------------------------
// 2. 空 ZIP（没有 manifest.json）
// ---------------------------------------------------------------------------

/// 导入没有任何条目的空 ZIP 应因找不到 manifest.json 而失败。
#[test]
fn test_backup_restore_empty_zip_no_manifest() {
    let core = common::setup_core();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let tmp_dir = std::env::temp_dir().join("moni_test_empty_zip");
        let _ = std::fs::create_dir_all(&tmp_dir);
        let empty_zip = tmp_dir.join("empty.zip");
        {
            let file = std::fs::File::create(&empty_zip).unwrap();
            let zip = zip::ZipWriter::new(file);
            zip.finish().unwrap();
        }

        let db_path = tmp_dir.join("moni.db");
        {
            let conn = rusqlite::Connection::open(&db_path).unwrap();
            moni_core::db::schema::init_schema(&conn).unwrap();
        }

        let result = core
            .backup_restore(
                empty_zip.to_str().unwrap().to_string(),
                db_path.to_str().unwrap().to_string(),
                None,
            )
            .await;

        assert!(result.is_err(), "空 ZIP 应失败");
        let err_msg = format!("{:?}", result.unwrap_err());
        assert!(
            err_msg.contains("manifest") || err_msg.contains("BackupManifestInvalid"),
            "错误应来自 manifest 读取阶段，实际: {err_msg}"
        );

        let _ = std::fs::remove_file(&empty_zip);
        let _ = std::fs::remove_file(&db_path);
        let _ = std::fs::remove_dir(&tmp_dir);
    });
}

// ---------------------------------------------------------------------------
// 3. manifest.json 是非法 JSON
// ---------------------------------------------------------------------------

/// manifest.json 内容不是合法 JSON 时应返回 `BackupManifestInvalid`。
#[test]
fn test_backup_restore_manifest_invalid_json() {
    let core = common::setup_core();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let tmp_dir = std::env::temp_dir().join("moni_test_bad_manifest");
        let _ = std::fs::create_dir_all(&tmp_dir);
        let bad_manifest_zip = tmp_dir.join("bad_manifest.zip");
        {
            let file = std::fs::File::create(&bad_manifest_zip).unwrap();
            let mut zip = zip::ZipWriter::new(file);
            let options = zip::write::FileOptions::<'_, ()>::default()
                .compression_method(zip::CompressionMethod::Stored);
            zip.start_file("manifest.json", options).unwrap();
            zip.write_all(b"this is not json {{").unwrap();
            zip.finish().unwrap();
        }

        let db_path = tmp_dir.join("moni.db");
        {
            let conn = rusqlite::Connection::open(&db_path).unwrap();
            moni_core::db::schema::init_schema(&conn).unwrap();
        }

        let result = core
            .backup_restore(
                bad_manifest_zip.to_str().unwrap().to_string(),
                db_path.to_str().unwrap().to_string(),
                None,
            )
            .await;

        assert!(result.is_err(), "非法 JSON manifest 应失败");
        let err_msg = format!("{:?}", result.unwrap_err());
        assert!(
            err_msg.contains("BackupManifestInvalid"),
            "错误类型应为 BackupManifestInvalid，实际: {err_msg}"
        );

        let _ = std::fs::remove_file(&bad_manifest_zip);
        let _ = std::fs::remove_file(&db_path);
        let _ = std::fs::remove_dir(&tmp_dir);
    });
}

// ---------------------------------------------------------------------------
// 4. manifest 缺少必要字段（如 stats）
// ---------------------------------------------------------------------------

/// manifest 缺少 `stats` 字段时 serde 反序列化应失败，返回 `BackupManifestInvalid`。
#[test]
fn test_backup_restore_manifest_missing_required_field() {
    let core = common::setup_core();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let tmp_dir = std::env::temp_dir().join("moni_test_missing_field");
        let _ = std::fs::create_dir_all(&tmp_dir);
        let bad_manifest_zip = tmp_dir.join("missing_field.zip");
        {
            let file = std::fs::File::create(&bad_manifest_zip).unwrap();
            let mut zip = zip::ZipWriter::new(file);
            let options = zip::write::FileOptions::<'_, ()>::default()
                .compression_method(zip::CompressionMethod::Stored);
            zip.start_file("manifest.json", options).unwrap();
            // 故意缺少 stats、files、manifest_sha256 等字段
            let partial = r#"{"format_version":1,"schema_version":1,"app_version_name":"0.1.0","app_version_code":1,"product_name":"Moni","package_name":"com.agguy.moni","created_at":"2026-05-06T12:00:00+08:00","device":{"manufacturer":"Test","model":"TestDevice","android_sdk":36}}"#;
            zip.write_all(partial.as_bytes()).unwrap();
            zip.finish().unwrap();
        }

        let db_path = tmp_dir.join("moni.db");
        {
            let conn = rusqlite::Connection::open(&db_path).unwrap();
            moni_core::db::schema::init_schema(&conn).unwrap();
        }

        let result = core
            .backup_restore(
                bad_manifest_zip.to_str().unwrap().to_string(),
                db_path.to_str().unwrap().to_string(),
                None,
            )
            .await;

        assert!(result.is_err(), "缺少必要字段的 manifest 应失败");
        let err_msg = format!("{:?}", result.unwrap_err());
        assert!(
            err_msg.contains("BackupManifestInvalid"),
            "错误类型应为 BackupManifestInvalid，实际: {err_msg}"
        );

        let _ = std::fs::remove_file(&bad_manifest_zip);
        let _ = std::fs::remove_file(&db_path);
        let _ = std::fs::remove_dir(&tmp_dir);
    });
}

// ---------------------------------------------------------------------------
// 5. format_version 是未来版本
// ---------------------------------------------------------------------------

/// manifest 的 format_version 超过当前支持的最大值时应返回 `BackupTooNew`。
#[test]
fn test_backup_restore_format_version_too_new() {
    let core = common::setup_core();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let tmp_dir = std::env::temp_dir().join("moni_test_version_too_new");
        let _ = std::fs::create_dir_all(&tmp_dir);

        // 构造一个合法的 db 与 settings
        let db_bytes = {
            let db_path = tmp_dir.join("source.db");
            let conn = rusqlite::Connection::open(&db_path).unwrap();
            moni_core::db::schema::init_schema(&conn).unwrap();
            std::fs::read(&db_path).unwrap()
        };
        let settings_bytes = b"{}".to_vec();

        let mut manifest = minimal_manifest();
        manifest.format_version = 999; // 未来版本
        manifest.stats = BackupStats {
            record_count: 0,
            category_count: 0,
            settings_count: 0,
            budget_count: Some(0),
        };
        manifest.files = vec![
            FileFingerprint {
                path: "db/moni.db".to_string(),
                size: db_bytes.len() as u64,
                sha256: sha256_hex(&db_bytes),
            },
            FileFingerprint {
                path: "settings/preferences.json".to_string(),
                size: settings_bytes.len() as u64,
                sha256: sha256_hex(&settings_bytes),
            },
        ];
        manifest.manifest_sha256 = compute_manifest_sha256(&manifest).unwrap();

        let zip_path = write_zip_with_entries(
            &tmp_dir,
            &manifest,
            &[
                ("db/moni.db".to_string(), db_bytes),
                ("settings/preferences.json".to_string(), settings_bytes),
            ],
        );

        let db_path = tmp_dir.join("moni.db");
        {
            let conn = rusqlite::Connection::open(&db_path).unwrap();
            moni_core::db::schema::init_schema(&conn).unwrap();
        }

        let result = core
            .backup_restore(
                zip_path.to_str().unwrap().to_string(),
                db_path.to_str().unwrap().to_string(),
                None,
            )
            .await;

        assert!(result.is_err(), "未来 format_version 应失败");
        let err_msg = format!("{:?}", result.unwrap_err());
        assert!(
            err_msg.contains("BackupTooNew") || err_msg.contains("版本过新"),
            "错误应提示版本过新，实际: {err_msg}"
        );

        let _ = std::fs::remove_file(&zip_path);
        let _ = std::fs::remove_file(&db_path);
        let _ = std::fs::remove_dir(&tmp_dir);
    });
}

// ---------------------------------------------------------------------------
// 6. manifest 自身指纹不匹配
// ---------------------------------------------------------------------------

/// 篡改 manifest 的 manifest_sha256 字段后，完整性校验应失败。
#[test]
fn test_backup_restore_manifest_integrity_mismatch() {
    let core = common::setup_core();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let tmp_dir = std::env::temp_dir().join("moni_test_manifest_integrity");
        let _ = std::fs::create_dir_all(&tmp_dir);

        let db_bytes = {
            let db_path = tmp_dir.join("source.db");
            let conn = rusqlite::Connection::open(&db_path).unwrap();
            moni_core::db::schema::init_schema(&conn).unwrap();
            std::fs::read(&db_path).unwrap()
        };
        let settings_bytes = b"{}".to_vec();

        let mut manifest = minimal_manifest();
        manifest.stats = BackupStats {
            record_count: 0,
            category_count: 0,
            settings_count: 0,
            budget_count: Some(0),
        };
        manifest.files = vec![
            FileFingerprint {
                path: "db/moni.db".to_string(),
                size: db_bytes.len() as u64,
                sha256: sha256_hex(&db_bytes),
            },
            FileFingerprint {
                path: "settings/preferences.json".to_string(),
                size: settings_bytes.len() as u64,
                sha256: sha256_hex(&settings_bytes),
            },
        ];
        // 先计算正确指纹，再故意篡改
        manifest.manifest_sha256 = compute_manifest_sha256(&manifest).unwrap();
        manifest.manifest_sha256 = "deadbeef".to_string();

        let zip_path = write_zip_with_entries(
            &tmp_dir,
            &manifest,
            &[
                ("db/moni.db".to_string(), db_bytes),
                ("settings/preferences.json".to_string(), settings_bytes),
            ],
        );

        let db_path = tmp_dir.join("moni.db");
        {
            let conn = rusqlite::Connection::open(&db_path).unwrap();
            moni_core::db::schema::init_schema(&conn).unwrap();
        }

        let result = core
            .backup_restore(
                zip_path.to_str().unwrap().to_string(),
                db_path.to_str().unwrap().to_string(),
                None,
            )
            .await;

        assert!(result.is_err(), "manifest 指纹不匹配应失败");
        let err_msg = format!("{:?}", result.unwrap_err());
        assert!(
            err_msg.contains("BackupManifestInvalid") || err_msg.contains("完整性校验失败"),
            "错误应来自 manifest 完整性校验，实际: {err_msg}"
        );

        let _ = std::fs::remove_file(&zip_path);
        let _ = std::fs::remove_file(&db_path);
        let _ = std::fs::remove_dir(&tmp_dir);
    });
}

// ---------------------------------------------------------------------------
// 7. db 文件指纹不匹配（ZIP 内 db 被篡改）
// ---------------------------------------------------------------------------

/// ZIP 内 db/moni.db 的内容与 manifest 中的 sha256 不匹配时应返回 `BackupCorrupted`。
#[test]
fn test_backup_restore_db_checksum_mismatch() {
    let core = common::setup_core();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let tmp_dir = std::env::temp_dir().join("moni_test_db_checksum");
        let _ = std::fs::create_dir_all(&tmp_dir);

        let db_bytes = {
            let db_path = tmp_dir.join("source.db");
            let conn = rusqlite::Connection::open(&db_path).unwrap();
            moni_core::db::schema::init_schema(&conn).unwrap();
            std::fs::read(&db_path).unwrap()
        };
        let settings_bytes = b"{}".to_vec();

        // manifest 中记录的是原始 db 的指纹
        let mut manifest = minimal_manifest();
        manifest.stats = BackupStats {
            record_count: 0,
            category_count: 0,
            settings_count: 0,
            budget_count: Some(0),
        };
        manifest.files = vec![
            FileFingerprint {
                path: "db/moni.db".to_string(),
                size: db_bytes.len() as u64,
                sha256: sha256_hex(&db_bytes),
            },
            FileFingerprint {
                path: "settings/preferences.json".to_string(),
                size: settings_bytes.len() as u64,
                sha256: sha256_hex(&settings_bytes),
            },
        ];
        manifest.manifest_sha256 = compute_manifest_sha256(&manifest).unwrap();

        // 但 ZIP 中实际写入的 db 被篡改了
        let tampered_db = [db_bytes.as_slice(), b"TAMPERED"].concat();

        let zip_path = write_zip_with_entries(
            &tmp_dir,
            &manifest,
            &[
                ("db/moni.db".to_string(), tampered_db),
                ("settings/preferences.json".to_string(), settings_bytes),
            ],
        );

        let db_path = tmp_dir.join("moni.db");
        {
            let conn = rusqlite::Connection::open(&db_path).unwrap();
            moni_core::db::schema::init_schema(&conn).unwrap();
        }

        let result = core
            .backup_restore(
                zip_path.to_str().unwrap().to_string(),
                db_path.to_str().unwrap().to_string(),
                None,
            )
            .await;

        assert!(result.is_err(), "db 指纹不匹配应失败");
        let err_msg = format!("{:?}", result.unwrap_err());
        assert!(
            err_msg.contains("BackupCorrupted") || err_msg.contains("校验失败"),
            "错误应来自文件指纹校验，实际: {err_msg}"
        );

        let _ = std::fs::remove_file(&zip_path);
        let _ = std::fs::remove_file(&db_path);
        let _ = std::fs::remove_dir(&tmp_dir);
    });
}

// ---------------------------------------------------------------------------
// 8. 畸形 SQLite 文件导致 schema 初始化失败
// ---------------------------------------------------------------------------

/// ZIP 内的 db/moni.db 不是有效 SQLite 文件（4 字节垃圾），解压后 `Connection::open`
/// 虽可成功，但 `init_schema` 执行时会因"file is not a database"而失败，
/// 触发 importer 中 schema 初始化错误路径。
#[test]
fn test_backup_restore_malformed_sqlite() {
    let core = common::setup_core();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let tmp_dir = std::env::temp_dir().join("moni_test_malformed_sqlite");
        let _ = std::fs::create_dir_all(&tmp_dir);

        let db_bytes = b"XXXX".to_vec();
        let settings_bytes = b"{}".to_vec();

        let mut manifest = minimal_manifest();
        manifest.stats = BackupStats {
            record_count: 0,
            category_count: 0,
            settings_count: 0,
            budget_count: Some(0),
        };
        manifest.files = vec![
            FileFingerprint {
                path: "db/moni.db".to_string(),
                size: db_bytes.len() as u64,
                sha256: sha256_hex(&db_bytes),
            },
            FileFingerprint {
                path: "settings/preferences.json".to_string(),
                size: settings_bytes.len() as u64,
                sha256: sha256_hex(&settings_bytes),
            },
        ];
        manifest.manifest_sha256 = compute_manifest_sha256(&manifest).unwrap();

        let zip_path = write_zip_with_entries(
            &tmp_dir,
            &manifest,
            &[
                ("db/moni.db".to_string(), db_bytes),
                ("settings/preferences.json".to_string(), settings_bytes),
            ],
        );

        let db_path = tmp_dir.join("moni.db");
        {
            let conn = rusqlite::Connection::open(&db_path).unwrap();
            moni_core::db::schema::init_schema(&conn).unwrap();
        }

        let result = core
            .backup_restore(
                zip_path.to_str().unwrap().to_string(),
                db_path.to_str().unwrap().to_string(),
                None,
            )
            .await;

        assert!(result.is_err(), "畸形 SQLite 应失败");
        let err_msg = format!("{:?}", result.unwrap_err());
        assert!(
            err_msg.contains("Database") || err_msg.contains("schema") || err_msg.contains("database"),
            "错误应来自数据库/schema 阶段，实际: {err_msg}"
        );

        let _ = std::fs::remove_file(&zip_path);
        let _ = std::fs::remove_file(&db_path);
        let _ = std::fs::remove_dir(&tmp_dir);
    });
}

// ---------------------------------------------------------------------------
// 9. rename 失败触发回滚（直接调用 importer::backup_restore）
// ---------------------------------------------------------------------------

/// 当目标 `db_path` 是一个已存在的目录时，`std::fs::rename` 会失败，
/// importer 应尝试从快照回滚并返回 `BackupRestoreFailed { stage: "replace" }`。
#[test]
fn test_backup_restore_rename_failure_rollback() {
    let tmp_dir = std::env::temp_dir().join("moni_test_rename_failure");
    let _ = std::fs::create_dir_all(&tmp_dir);

    // 构造一个合法的备份 ZIP
    let db_bytes = {
        let db_path = tmp_dir.join("source.db");
        let conn = rusqlite::Connection::open(&db_path).unwrap();
        moni_core::db::schema::init_schema(&conn).unwrap();
        std::fs::read(&db_path).unwrap()
    };
    let settings_bytes = b"{}".to_vec();

    let mut manifest = minimal_manifest();
    manifest.stats = BackupStats {
        record_count: 0,
        category_count: 0,
        settings_count: 0,
        budget_count: Some(0),
    };
    manifest.files = vec![
        FileFingerprint {
            path: "db/moni.db".to_string(),
            size: db_bytes.len() as u64,
            sha256: sha256_hex(&db_bytes),
        },
        FileFingerprint {
            path: "settings/preferences.json".to_string(),
            size: settings_bytes.len() as u64,
            sha256: sha256_hex(&settings_bytes),
        },
    ];
    manifest.manifest_sha256 = compute_manifest_sha256(&manifest).unwrap();

    let zip_path = write_zip_with_entries(
        &tmp_dir,
        &manifest,
        &[
            ("db/moni.db".to_string(), db_bytes),
            ("settings/preferences.json".to_string(), settings_bytes),
        ],
    );

    // 准备一个快照文件（任意合法 db 即可）
    let snapshot_path = tmp_dir.join("snapshot.db");
    {
        let conn = rusqlite::Connection::open(&snapshot_path).unwrap();
        moni_core::db::schema::init_schema(&conn).unwrap();
    }

    // 让 db_path 是一个已存在的目录，这样 rename 会失败
    let db_path = tmp_dir.join("existing_dir");
    std::fs::create_dir(&db_path).unwrap();

    let result = moni_core::domain::backup::importer::backup_restore(
        zip_path.to_str().unwrap(),
        db_path.to_str().unwrap(),
        Some(snapshot_path.to_str().unwrap()),
        None,
    );

    assert!(result.is_err(), "rename 到目录应失败");
    let err_msg = format!("{:?}", result.unwrap_err());
    assert!(
        err_msg.contains("replace") || err_msg.contains("BackupRestoreFailed"),
        "错误应来自 replace 阶段，实际: {err_msg}"
    );

    let _ = std::fs::remove_file(&zip_path);
    let _ = std::fs::remove_file(&snapshot_path);
    let _ = std::fs::remove_dir(&db_path);
    let _ = std::fs::remove_dir(&tmp_dir);
}

// ---------------------------------------------------------------------------
// 10. manifest skip 分支：manifest.json 自身不参与文件指纹校验
// ---------------------------------------------------------------------------

/// 验证 importer 在校验文件指纹时会跳过 manifest.json 自身（L121 continue 分支）。
/// 构造一个 ZIP，其中 manifest.json 的指纹故意不匹配自身内容，但其他文件指纹正确。
/// 由于 importer 会 skip manifest.json，整个恢复流程应成功。
#[test]
fn test_backup_restore_manifest_skip_self_checksum() {
    let core = common::setup_core();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let tmp_dir = std::env::temp_dir().join("moni_test_manifest_skip");
        let _ = std::fs::create_dir_all(&tmp_dir);

        let db_bytes = {
            let db_path = tmp_dir.join("source.db");
            let conn = rusqlite::Connection::open(&db_path).unwrap();
            moni_core::db::schema::init_schema(&conn).unwrap();
            std::fs::read(&db_path).unwrap()
        };
        let settings_bytes = b"{}".to_vec();

        let mut manifest = minimal_manifest();
        manifest.stats = BackupStats {
            record_count: 0,
            category_count: 0,
            settings_count: 0,
            budget_count: Some(0),
        };
        manifest.files = vec![
            FileFingerprint {
                path: "db/moni.db".to_string(),
                size: db_bytes.len() as u64,
                sha256: sha256_hex(&db_bytes),
            },
            FileFingerprint {
                path: "settings/preferences.json".to_string(),
                size: settings_bytes.len() as u64,
                sha256: sha256_hex(&settings_bytes),
            },
            // 故意把 manifest.json 也加入 files 列表，并给一个错误指纹
            FileFingerprint {
                path: "manifest.json".to_string(),
                size: 1,
                sha256: "0000000000000000000000000000000000000000000000000000000000000000".to_string(),
            },
        ];
        manifest.manifest_sha256 = compute_manifest_sha256(&manifest).unwrap();

        let zip_path = write_zip_with_entries(
            &tmp_dir,
            &manifest,
            &[
                ("db/moni.db".to_string(), db_bytes),
                ("settings/preferences.json".to_string(), settings_bytes),
            ],
        );

        let db_path = tmp_dir.join("moni.db");
        {
            let conn = rusqlite::Connection::open(&db_path).unwrap();
            moni_core::db::schema::init_schema(&conn).unwrap();
        }

        let result = core
            .backup_restore(
                zip_path.to_str().unwrap().to_string(),
                db_path.to_str().unwrap().to_string(),
                None,
            )
            .await;

        assert!(
            result.is_ok(),
            "manifest.json 自身指纹错误不应影响恢复，实际: {:?}",
            result
        );

        let _ = std::fs::remove_file(&zip_path);
        let _ = std::fs::remove_file(&db_path);
        let _ = std::fs::remove_dir(&tmp_dir);
    });
}
