use serde::{Deserialize, Serialize};

use moni_contracts::export::ExportFormat;
use moni_contracts::record::RecordType;
use moni_contracts::types::{AmountCents, CategoryId, RecordId, TimestampSec};

/// Mock 数据预设类型。
#[derive(Debug, Clone, Copy, Serialize, Deserialize)]
pub enum MockPreset {
    #[serde(rename = "normal")]
    Normal,
    #[serde(rename = "stress")]
    Stress,
}

impl std::fmt::Display for MockPreset {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            MockPreset::Normal => write!(f, "normal"),
            MockPreset::Stress => write!(f, "stress"),
        }
    }
}

/// 核心意图枚举
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(tag = "type", rename_all = "snake_case")]
pub enum CoreIntent {
    RecordCreate {
        amount_cents: AmountCents,
        record_type: RecordType,
        category_id: CategoryId,
        note: String,
        timestamp: Option<TimestampSec>,
    },
    RecordUpdate {
        id: RecordId,
        amount_cents: Option<AmountCents>,
        record_type: Option<RecordType>,
        category_id: Option<CategoryId>,
        note: Option<String>,
    },
    RecordDelete {
        id: RecordId,
    },
    RecordList {
        page: u32,
        page_size: u32,
    },
    RecordGet {
        id: RecordId,
    },

    CategoryCreate {
        name: String,
        description: Option<String>,
        category_type: RecordType,
        icon_name: String,
    },
    CategoryUpdate {
        id: CategoryId,
        name: Option<String>,
        description: Option<String>,
        icon_name: Option<String>,
    },
    CategoryArchive {
        id: CategoryId,
    },
    CategoryUnarchive {
        id: CategoryId,
    },
    CategoryList,

    StatsMonthlySummary {
        months: u8,
    },
    StatsCategoryBreakdown {
        year_month: String,
    },

    SettingsUpdateCurrency {
        symbol: String,
    },
    SettingsExportData {
        format: ExportFormat,
    },

    DevClearAllData,
    DevGenerateMockData {
        count: u32,
        preset: MockPreset,
    },

    NavigateTo {
        screen: String,
    },
    DismissError,
}
