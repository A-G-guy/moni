/// 自动备份频率。
#[derive(Debug, Clone, Copy, PartialEq, Eq, uniffi::Enum)]
pub enum AutoBackupFrequency {
    EveryLaunch,
    Daily,
    Weekly,
    Monthly,
}

/// 自动备份执行报告。
#[derive(Debug, Clone, uniffi::Record)]
pub struct AutoBackupReport {
    pub zip_path: String,
    pub total_bytes: u64,
    pub record_count: u64,
    pub category_count: u64,
    pub settings_count: u64,
    pub created_at: String,
}

impl AutoBackupFrequency {
    /// 从字符串解析频率，无效值返回 None。
    pub fn from_str(s: &str) -> Option<Self> {
        match s {
            "every_launch" => Some(Self::EveryLaunch),
            "daily" => Some(Self::Daily),
            "weekly" => Some(Self::Weekly),
            "monthly" => Some(Self::Monthly),
            _ => None,
        }
    }

    /// 转换为字符串表示。
    pub fn as_str(&self) -> &'static str {
        match self {
            Self::EveryLaunch => "every_launch",
            Self::Daily => "daily",
            Self::Weekly => "weekly",
            Self::Monthly => "monthly",
        }
    }
}
