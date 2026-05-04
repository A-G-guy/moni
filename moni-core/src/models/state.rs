use serde::{Deserialize, Serialize};

use crate::dto::{CategoryDto, RecordDayGroup, RecordDto};
use moni_contracts::stats::{CategoryBreakdown, MonthlySummary};

/// 应用状态根
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AppState {
    pub records: Vec<RecordDto>,
    pub record_groups: Vec<RecordDayGroup>,
    pub categories: Vec<CategoryDto>,
    pub monthly_summaries: Vec<MonthlySummary>,
    pub current_month_breakdown: Vec<CategoryBreakdown>,
    pub settings: AppSettings,
    pub ui: UiState,
}

/// 应用设置
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AppSettings {
    pub currency_symbol: String,
}

impl Default for AppSettings {
    fn default() -> Self {
        Self {
            currency_symbol: "¥".to_string(),
        }
    }
}

/// UI 状态
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UiState {
    pub active_tab: String,
    pub selected_record_id: Option<i64>,
    pub error_message: Option<String>,
}

impl Default for UiState {
    fn default() -> Self {
        Self {
            active_tab: "records".to_string(),
            selected_record_id: None,
            error_message: None,
        }
    }
}
