use serde::{Deserialize, Serialize};

use moni_contracts::export::ExportFormat;
use moni_contracts::record::RecordType;
use moni_contracts::types::{AmountCents, CategoryId, RecordId, TimestampSec};

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
    RecordDelete { id: RecordId },
    RecordList { page: u32, page_size: u32 },
    RecordGet { id: RecordId },

    CategoryCreate {
        name: String,
        category_type: RecordType,
        icon_name: String,
        color_hex: String,
    },
    CategoryDelete { id: CategoryId },
    CategoryList,

    StatsMonthlySummary { months: u8 },
    StatsCategoryBreakdown { year_month: String },

    SettingsUpdateCurrency { symbol: String },
    SettingsExportData { format: ExportFormat },

    DevClearAllData,
    DevGenerateMockData { count: u32, preset: String },

    NavigateTo { screen: String },
    DismissError,
}
