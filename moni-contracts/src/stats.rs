use serde::{Deserialize, Serialize};

use crate::types::{AmountCents, CategoryId};

/// 月度收支汇总
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MonthlySummary {
    pub year_month: String,
    pub income_cents: AmountCents,
    pub expense_cents: AmountCents,
    pub balance_cents: AmountCents,
}

/// 分类支出占比。
///
/// 颜色不再由后端下发，前端按 `record_type` 与色阶规则统一渲染（参见 UI 层）。
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CategoryBreakdown {
    pub category_id: CategoryId,
    pub category_name: String,
    pub amount_cents: AmountCents,
    pub percentage: f64,
}
