//! 备份迁移与清单模块的补充测试。
//!
//! 覆盖范围：
//! - `apply_migrations`：`from_version < to_version` 时空注册表分支 happy-path 与边界值。
//! - `BackupManifest` serde：缺字段失败、字段类型错误失败、未知字段忽略。
//! - `read_manifest`：ZIP 内 manifest.json 是无效 JSON 时的错误路径。
//! - 指纹/哈希函数：空输入、跨缓冲块大输入、与未来版本号校验。
//! - `compute_manifest_sha256`：忽略 `manifest_sha256` 字段值的稳定性（自指纹移除验证）。

use std::io::Write;

use moni_core::db::schema::{init_schema, CURRENT_SCHEMA_VERSION};
use moni_core::domain::backup::manifest::{
    BackupManifest, BackupStats, DeviceInfo, FileFingerprint, bytes_to_hex,
    compute_manifest_sha256, compute_sha256_hex, read_manifest, validate_format_version,
};
use moni_core::domain::backup::migrations::apply_migrations;
use moni_core::domain::backup::MAX_SUPPORTED_FORMAT_VERSION;

/// 构造一个最小可用的 manifest 样本。
fn sample_manifest() -> BackupManifest {
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
            settings_count: 0,
        },
        files: vec![],
        manifest_sha256: String::new(),
    }
}

/// `apply_migrations(0, 1)` 在空注册表下应直接成功，覆盖 `from < to` 进入循环但零次迭代的路径。
#[test]
fn test_apply_migrations_from_zero_to_current_succeeds() {
    let conn = rusqlite::Connection::open_in_memory().expect("内存数据库创建失败");
    init_schema(&conn).expect("init_schema 应成功");

    apply_migrations(&conn, 0, CURRENT_SCHEMA_VERSION)
        .expect("from=0 to=current 在空注册表下应直接 Ok");
}

/// `apply_migrations` 接受跨大跨度且 `from < to` 的输入，循环零次后仍 Ok。
#[test]
fn test_apply_migrations_large_span_succeeds_with_empty_registry() {
    let conn = rusqlite::Connection::open_in_memory().expect("内存数据库创建失败");
    init_schema(&conn).expect("init_schema 应成功");

    // 远超当前版本的目标，注册表为空时仍应成功。
    apply_migrations(&conn, 0, 100).expect("超大目标版本不应导致失败");
    apply_migrations(&conn, 7, 99).expect("中段跨度不应导致失败");
}

/// 同一 manifest 多次序列化反序列化，所有字段应保持一致。
#[test]
fn test_manifest_serde_full_roundtrip_preserves_fields() {
    let mut manifest = sample_manifest();
    manifest.files.push(FileFingerprint {
        path: "db/moni.db".to_string(),
        size: 8192,
        sha256: "deadbeef".to_string(),
    });
    manifest.stats.record_count = 12;
    manifest.manifest_sha256 = compute_manifest_sha256(&manifest).unwrap();

    let json = serde_json::to_string(&manifest).unwrap();
    let decoded: BackupManifest = serde_json::from_str(&json).unwrap();

    assert_eq!(decoded.format_version, manifest.format_version);
    assert_eq!(decoded.schema_version, manifest.schema_version);
    assert_eq!(decoded.app_version_code, manifest.app_version_code);
    assert_eq!(decoded.product_name, manifest.product_name);
    assert_eq!(decoded.package_name, manifest.package_name);
    assert_eq!(decoded.device.android_sdk, manifest.device.android_sdk);
    assert_eq!(decoded.stats.record_count, 12);
    assert_eq!(decoded.files.len(), 1);
    assert_eq!(decoded.files[0].path, "db/moni.db");
    assert_eq!(decoded.manifest_sha256, manifest.manifest_sha256);
}

/// 缺失关键字段的 JSON 反序列化应失败（serde 严格模式）。
#[test]
fn test_manifest_deserialize_missing_field_fails() {
    // 缺少 schema_version 等必填字段
    let bad_json = r#"{"format_version": 1}"#;
    let result: Result<BackupManifest, _> = serde_json::from_str(bad_json);
    assert!(result.is_err(), "缺字段应反序列化失败");
}

/// 字段类型错误时 serde 应返回错误。
#[test]
fn test_manifest_deserialize_wrong_type_fails() {
    // format_version 期望 u32，但给了字符串
    let bad_json = r#"{
        "format_version": "not_a_number",
        "schema_version": 1,
        "app_version_name": "0.1.0",
        "app_version_code": 1,
        "product_name": "Moni",
        "package_name": "com.agguy.moni",
        "created_at": "2026-05-06T12:00:00+08:00",
        "device": {"manufacturer": "T", "model": "M", "android_sdk": 36},
        "stats": {"record_count": 0, "category_count": 0, "settings_count": 0},
        "files": [],
        "manifest_sha256": ""
    }"#;
    let result: Result<BackupManifest, _> = serde_json::from_str(bad_json);
    assert!(result.is_err(), "字段类型错误应反序列化失败");
}

/// 含未知字段的 JSON 反序列化应忽略未知字段并成功。
#[test]
fn test_manifest_deserialize_unknown_field_is_ignored() {
    // 加入一个 future_extension 字段，serde 默认忽略未知字段
    let json = r#"{
        "format_version": 1,
        "schema_version": 1,
        "app_version_name": "0.1.0",
        "app_version_code": 1,
        "product_name": "Moni",
        "package_name": "com.agguy.moni",
        "created_at": "2026-05-06T12:00:00+08:00",
        "device": {"manufacturer": "T", "model": "M", "android_sdk": 36},
        "stats": {"record_count": 0, "category_count": 0, "settings_count": 0},
        "files": [],
        "manifest_sha256": "",
        "future_extension": {"some": "value"}
    }"#;
    let result: Result<BackupManifest, _> = serde_json::from_str(json);
    assert!(result.is_ok(), "未知字段应被静默忽略");
}

/// `read_manifest` 在 ZIP 内 manifest.json 非法 JSON 时应返回 `BackupManifestInvalid`。
#[test]
fn test_read_manifest_invalid_json_returns_error() {
    let tmp = std::env::temp_dir().join("moni_test_invalid_manifest.zip");
    {
        let file = std::fs::File::create(&tmp).unwrap();
        let mut zip = zip::ZipWriter::new(file);
        let options = zip::write::FileOptions::<'_, ()>::default()
            .compression_method(zip::CompressionMethod::Stored);
        zip.start_file("manifest.json", options).unwrap();
        zip.write_all(b"this is not valid json").unwrap();
        zip.finish().unwrap();
    }

    let file = std::fs::File::open(&tmp).unwrap();
    let mut zip = zip::ZipArchive::new(file).unwrap();
    let result = read_manifest(&mut zip);
    assert!(result.is_err(), "非法 JSON 应失败");
    let err_msg = format!("{:?}", result.unwrap_err());
    assert!(
        err_msg.contains("BackupManifestInvalid") || err_msg.to_lowercase().contains("manifest"),
        "错误应来自 manifest 校验路径，实际: {err_msg}"
    );
    let _ = std::fs::remove_file(&tmp);
}

/// `validate_format_version` 在最大支持版本边界上的等值校验：等于上限应通过。
#[test]
fn test_validate_format_version_at_boundary_succeeds() {
    assert!(
        validate_format_version(MAX_SUPPORTED_FORMAT_VERSION).is_ok(),
        "等于上限版本应被接受"
    );
}

/// `validate_format_version` 上限 + 1 应被拒绝且错误中带版本数字。
#[test]
fn test_validate_format_version_just_above_boundary_fails() {
    let result = validate_format_version(MAX_SUPPORTED_FORMAT_VERSION + 1);
    assert!(result.is_err(), "上限+1 应被拒绝");
    let err = format!("{:?}", result.unwrap_err());
    assert!(err.contains("BackupTooNew"), "应返回 BackupTooNew，实际: {err}");
}

/// `compute_sha256_hex` 处理空输入应返回 SHA-256 已知零长度值。
#[test]
fn test_compute_sha256_hex_empty_input() {
    let empty: &[u8] = &[];
    let actual = compute_sha256_hex(empty).unwrap();
    let expected = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    assert_eq!(actual, expected, "空输入的 SHA-256 应等于已知常量");
}

/// `compute_sha256_hex` 处理大于内部缓冲区 (8192) 的输入，触发多轮读循环。
#[test]
fn test_compute_sha256_hex_multi_buffer_input() {
    // 16 KiB + 13 字节，强制 read() 多次返回。
    let data = vec![0xABu8; 8192 * 2 + 13];
    let hash = compute_sha256_hex(data.as_slice()).unwrap();
    assert_eq!(hash.len(), 64, "返回应为 64 位十六进制");
    // 确保确定性：再算一次得到相同值。
    let hash2 = compute_sha256_hex(data.as_slice()).unwrap();
    assert_eq!(hash, hash2, "相同输入应得到相同 hash");
}

/// `compute_manifest_sha256` 应忽略 `manifest_sha256` 字段本身，
/// 因此修改该字段不影响计算结果（自指纹自洽性）。
#[test]
fn test_compute_manifest_sha256_ignores_self_field() {
    let mut manifest = sample_manifest();
    manifest.manifest_sha256 = String::new();
    let hash_a = compute_manifest_sha256(&manifest).unwrap();

    // 把自指纹字段改成任意噪声值；理论结果应不变。
    manifest.manifest_sha256 = "noise_value_should_not_affect".to_string();
    let hash_b = compute_manifest_sha256(&manifest).unwrap();

    assert_eq!(
        hash_a, hash_b,
        "manifest_sha256 字段不应参与自指纹计算"
    );
}

/// `bytes_to_hex` 边界：空数组与单字节最大值。
#[test]
fn test_bytes_to_hex_boundaries() {
    assert_eq!(bytes_to_hex(&[]), "");
    assert_eq!(bytes_to_hex(&[0xff]), "ff");
    assert_eq!(bytes_to_hex(&[0x00]), "00");
}
