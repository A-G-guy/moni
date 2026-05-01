use serde::{Deserialize, Serialize};

use crate::types::{AmountCents, CategoryId};

/// 月度收支汇总
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MonthlySummary {
    pub year_month: String,
    pub income_cents: AmountCents,
    pub expense_cents: AmountCents,
    pub balance_cents: AmountCents,
}

/// 分类支出占比
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CategoryBreakdown {
    pub category_id: CategoryId,
    pub category_name: String,
    pub color_hex: String,
    pub amount_cents: AmountCents,
    pub percentage: f64,
}
