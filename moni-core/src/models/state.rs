use serde::{Deserialize, Serialize};

use crate::dto::{BudgetDto, CategoryDto, RecordDayGroup, RecordDto};
use moni_contracts::stats::{CategoryBreakdown, MonthlySummary};

/// 月度概览指标
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct OverviewMetrics {
    pub month_expense: i64,
    pub month_income: i64,
    pub month_balance: i64,
    pub today_expense: Option<i64>,
    pub daily_avg: Option<i64>,
    pub daily_remaining: Option<i64>,
    pub total_budget: Option<BudgetDto>,
    pub elapsed_days: i32,
    pub total_days: i32,
    pub remaining_days: i32,
    /// 总预算进度状态：normal / warning / overrun
    pub budget_progress_status: Option<String>,
}

/// 应用状态根
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AppState {
    pub records: Vec<RecordDto>,
    pub record_groups: Vec<RecordDayGroup>,
    pub categories: Vec<CategoryDto>,
    pub monthly_summaries: Vec<MonthlySummary>,
    pub current_month_breakdown: Vec<CategoryBreakdown>,
    pub budgets: Vec<BudgetDto>,
    pub budget_check_result: Option<BudgetCheckResult>,
    pub overview_metrics: Option<OverviewMetrics>,
    pub settings: AppSettings,
    pub ui: UiState,
}

/// 预算检查结果（用于记账页实时预警）
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct BudgetCheckResult {
    pub category_id: i64,
    pub amount_cents: i64,
    pub effective_available: Option<i64>,
    /// 瓶颈预算类型："total" | "parent" | "self"
    pub bottleneck_budget: Option<String>,
    /// 瓶颈预算对应的具体分类名称（total 为 None）
    pub bottleneck_category_name: Option<String>,
    pub post_save_status: Option<String>,
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
