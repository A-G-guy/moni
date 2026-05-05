pub mod exporter;
pub mod importer;
pub mod manifest;
pub mod migrations;
pub mod readme;

pub use exporter::backup_export;
pub use importer::backup_restore;
pub use manifest::{read_manifest, BackupManifest};

/// 备份包格式支持的最大版本号。
pub const MAX_SUPPORTED_FORMAT_VERSION: u32 = 1;
