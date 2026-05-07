use std::io::Write;

use moni_core::domain::backup::manifest::{
    BackupManifest, BackupStats, DeviceInfo, FileFingerprint, bytes_to_hex,
    compute_manifest_sha256, compute_sha256_hex, read_manifest, validate_format_version,
    verify_manifest_integrity,
};

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
            record_count: 10,
            category_count: 5,
            budget_count: Some(0),
            settings_count: 4,
        },
        files: vec![
            FileFingerprint {
                path: "db/moni.db".to_string(),
                size: 1024,
                sha256: "abcd1234".to_string(),
            },
        ],
        manifest_sha256: String::new(),
    }
}

#[test]
fn test_bytes_to_hex() {
    assert_eq!(bytes_to_hex(&[0x00, 0x0f, 0xff]), "000fff");
    assert_eq!(bytes_to_hex(&[]), "");
}

#[test]
fn test_compute_sha256_hex_known_value() {
    // SHA-256("hello")
    let expected = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";
    let actual = compute_sha256_hex("hello".as_bytes()).unwrap();
    assert_eq!(actual, expected);
}

#[test]
fn test_compute_manifest_sha256_stable() {
    let mut manifest = sample_manifest();
    manifest.manifest_sha256 = String::new();
    let hash1 = compute_manifest_sha256(&manifest).unwrap();
    let hash2 = compute_manifest_sha256(&manifest).unwrap();
    assert_eq!(hash1, hash2, "相同 manifest 应产生相同 sha256");
    assert_eq!(hash1.len(), 64, "sha256 应为 64 位十六进制字符串");
}

#[test]
fn test_verify_manifest_integrity_success() {
    let mut manifest = sample_manifest();
    manifest.manifest_sha256 = compute_manifest_sha256(&manifest).unwrap();
    assert!(verify_manifest_integrity(&manifest).is_ok());
}

#[test]
fn test_verify_manifest_integrity_failure() {
    let mut manifest = sample_manifest();
    manifest.manifest_sha256 = "invalid_hash".to_string();
    let result = verify_manifest_integrity(&manifest);
    assert!(result.is_err());
}

#[test]
fn test_validate_format_version_accepted() {
    assert!(validate_format_version(1).is_ok());
}

#[test]
fn test_validate_format_version_rejected() {
    let result = validate_format_version(999);
    assert!(result.is_err());
}

#[test]
fn test_manifest_serde_roundtrip() {
    let mut manifest = sample_manifest();
    manifest.manifest_sha256 = compute_manifest_sha256(&manifest).unwrap();

    let json = serde_json::to_string_pretty(&manifest).unwrap();
    let decoded: BackupManifest = serde_json::from_str(&json).unwrap();

    assert_eq!(decoded.format_version, manifest.format_version);
    assert_eq!(decoded.schema_version, manifest.schema_version);
    assert_eq!(decoded.app_version_name, manifest.app_version_name);
    assert_eq!(decoded.files.len(), manifest.files.len());
    assert_eq!(decoded.manifest_sha256, manifest.manifest_sha256);
}

#[test]
fn test_read_manifest_from_zip() {
    use std::io::Write;

    let mut manifest = sample_manifest();
    manifest.manifest_sha256 = compute_manifest_sha256(&manifest).unwrap();
    let json = serde_json::to_string_pretty(&manifest).unwrap();

    let tmp = std::env::temp_dir().join("moni_test_manifest.zip");
    {
        let file = std::fs::File::create(&tmp).unwrap();
        let mut zip = zip::ZipWriter::new(file);
        let options = zip::write::FileOptions::<'_, ()>::default()
            .compression_method(zip::CompressionMethod::Stored);
        zip.start_file("manifest.json", options).unwrap();
        zip.write_all(json.as_bytes()).unwrap();
        zip.finish().unwrap();
    }

    let file = std::fs::File::open(&tmp).unwrap();
    let mut zip = zip::ZipArchive::new(file).unwrap();
    let read = read_manifest(&mut zip).unwrap();

    assert_eq!(read.format_version, 1);
    assert_eq!(read.app_version_name, "0.1.0");
    let _ = std::fs::remove_file(&tmp);
}

#[test]
fn test_read_manifest_missing_entry() {
    let tmp = std::env::temp_dir().join("moni_test_no_manifest.zip");
    {
        let file = std::fs::File::create(&tmp).unwrap();
        let mut zip = zip::ZipWriter::new(file);
        let options = zip::write::FileOptions::<'_, ()>::default()
            .compression_method(zip::CompressionMethod::Stored);
        zip.start_file("README.md", options).unwrap();
        zip.write_all(b"test").unwrap();
        zip.finish().unwrap();
    }

    let file = std::fs::File::open(&tmp).unwrap();
    let mut zip = zip::ZipArchive::new(file).unwrap();
    let result = read_manifest(&mut zip);
    assert!(result.is_err());
    let _ = std::fs::remove_file(&tmp);
}
