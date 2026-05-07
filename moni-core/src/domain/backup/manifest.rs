use serde::{Deserialize, Serialize};
use sha2::{Digest, Sha256};
use std::io::{BufReader, Read};

use crate::domain::backup::MAX_SUPPORTED_FORMAT_VERSION;

/// ZIP 包内的文件指纹项。
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FileFingerprint {
    pub path: String,
    pub size: u64,
    pub sha256: String,
}

/// 设备信息。
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DeviceInfo {
    pub manufacturer: String,
    pub model: String,
    pub android_sdk: i32,
}

/// 备份统计。
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct BackupStats {
    pub record_count: u64,
    pub category_count: u64,
    #[serde(default)]
    pub budget_count: Option<u64>,
    pub settings_count: u64,
}

/// 备份清单结构。
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct BackupManifest {
    pub format_version: u32,
    pub schema_version: u32,
    pub app_version_name: String,
    pub app_version_code: i64,
    pub product_name: String,
    pub package_name: String,
    pub created_at: String,
    pub device: DeviceInfo,
    pub stats: BackupStats,
    pub files: Vec<FileFingerprint>,
    #[serde(rename = "manifest_sha256")]
    pub manifest_sha256: String,
}

/// 将字节数组编码为十六进制字符串。
pub fn bytes_to_hex(bytes: &[u8]) -> String {
    bytes.iter().map(|b| format!("{b:02x}")).collect()
}

/// 计算输入流的 SHA-256 十六进制字符串。
pub fn compute_sha256_hex<R: Read>(reader: R) -> Result<String, std::io::Error> {
    let mut hasher = Sha256::new();
    let mut buf_reader = BufReader::new(reader);
    let mut buffer = [0u8; 8192];

    loop {
        let bytes_read = buf_reader.read(&mut buffer)?;
        if bytes_read == 0 {
            break;
        }
        hasher.update(&buffer[..bytes_read]);
    }

    Ok(bytes_to_hex(&hasher.finalize()))
}

/// 从 ZIP 中读取并解析 manifest.json。
pub fn read_manifest<R: Read + std::io::Seek>(
    zip: &mut zip::ZipArchive<R>,
) -> Result<BackupManifest, crate::core::error::CoreError> {
    let mut manifest_file = zip
        .by_name("manifest.json")
        .map_err(|e| crate::core::error::CoreError::BackupManifestInvalid(e.to_string()))?;
    let mut content = String::new();
    manifest_file
        .read_to_string(&mut content)
        .map_err(|e| crate::core::error::CoreError::BackupIo(e.to_string()))?;
    let manifest: BackupManifest = serde_json::from_str(&content)
        .map_err(|e| crate::core::error::CoreError::BackupManifestInvalid(e.to_string()))?;
    Ok(manifest)
}

/// 验证 manifest 格式版本是否在当前支持范围内。
pub fn validate_format_version(format_version: u32) -> Result<(), crate::core::error::CoreError> {
    if format_version > MAX_SUPPORTED_FORMAT_VERSION {
        return Err(crate::core::error::CoreError::BackupTooNew {
            required: format_version,
            supported: MAX_SUPPORTED_FORMAT_VERSION,
        });
    }
    Ok(())
}

/// 计算不含 manifest_sha256 字段的 manifest 自身指纹。
pub fn compute_manifest_sha256(manifest: &BackupManifest) -> Result<String, crate::core::error::CoreError> {
    let mut stripped = manifest.clone();
    stripped.manifest_sha256 = String::new();
    let json = serde_json::to_string(&stripped)
        .map_err(|e| crate::core::error::CoreError::Internal(format!("清单序列化失败: {e}")))?;
    let mut hasher = Sha256::new();
    hasher.update(json.as_bytes());
    Ok(bytes_to_hex(&hasher.finalize()))
}

/// 验证 manifest 自身 sha256。
pub fn verify_manifest_integrity(
    manifest: &BackupManifest,
) -> Result<(), crate::core::error::CoreError> {
    let expected = compute_manifest_sha256(manifest)?;
    if expected != manifest.manifest_sha256 {
        return Err(crate::core::error::CoreError::BackupManifestInvalid(
            "清单完整性校验失败".to_string(),
        ));
    }
    Ok(())
}
