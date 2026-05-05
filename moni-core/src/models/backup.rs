use serde::{Deserialize, Serialize};

/// 备份导出报告。
#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct BackupExportReport {
    pub out_zip_path: String,
    pub record_count: u64,
    pub category_count: u64,
    pub settings_count: u64,
    pub total_bytes: u64,
}

/// 备份包预览信息。
#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct BackupInspection {
    pub format_version: u32,
    pub schema_version: u32,
    pub app_version_name: String,
    pub app_version_code: i64,
    pub created_at: String,
    pub record_count: u64,
    pub category_count: u64,
    pub settings_count: u64,
    pub total_bytes: u64,
}

/// 备份恢复报告。
#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct BackupRestoreReport {
    pub restored_record_count: u64,
    pub restored_category_count: u64,
    pub restored_settings_count: u64,
    pub settings_json: String,
}

/// 备份进度监听器（UniFFI Callback）。
#[uniffi::export(callback_interface)]
pub trait BackupProgressListener: Send + Sync {
    fn on_stage(&self, stage: String, percent: i32);
}
