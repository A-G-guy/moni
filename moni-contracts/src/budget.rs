use serde::{Deserialize, Serialize};

use crate::types::{AmountCents, BudgetId, CategoryId, TimestampSec};

/// 预算实体
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Budget {
    pub id: BudgetId,
    pub category_id: Option<CategoryId>,
    pub amount_cents: AmountCents,
    pub period_type: BudgetPeriodType,
    pub created_at: TimestampSec,
    pub updated_at: TimestampSec,
}

/// 预算周期类型
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum BudgetPeriodType {
    Monthly,
}

/// 预算状态
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum BudgetStatus {
    Safe,     // < 80%
    Critical, // 80% - 100%
    Overrun,  // > 100%
}

impl BudgetStatus {
    /// 根据使用率判断预算状态
    pub fn from_percentage(percentage: f64) -> Self {
        if percentage > 1.0 {
            BudgetStatus::Overrun
        } else if percentage >= 0.8 {
            BudgetStatus::Critical
        } else {
            BudgetStatus::Safe
        }
    }

    pub fn as_str(&self) -> &'static str {
        match self {
            BudgetStatus::Safe => "safe",
            BudgetStatus::Critical => "critical",
            BudgetStatus::Overrun => "overrun",
        }
    }
}
