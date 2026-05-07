use serde::{Deserialize, Serialize};

use crate::types::{AmountCents, BudgetId, CategoryId, TimestampSec};

/// 预算实体
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Budget {
    pub id: BudgetId,
    pub category_id: Option<CategoryId>,
    pub amount_cents: AmountCents,
    /// 预算生效月份。None 表示模板（长期规则），Some("YYYY-MM") 表示月度快照。
    pub year_month: Option<String>,
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

impl BudgetPeriodType {
    pub fn as_str(&self) -> &'static str {
        match self {
            BudgetPeriodType::Monthly => "monthly",
        }
    }
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

/// 预算操作生效范围
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum BudgetScope {
    /// 仅影响本月（创建快照或删除本月及以后的模板+快照）
    ThisMonth,
    /// 影响本月及以后（创建/更新模板，删除从本月起的快照）
    ThisAndFuture,
    /// 仅影响以后（创建/更新模板，保留当前月快照）
    FutureOnly,
}

impl BudgetScope {
    pub fn as_str(&self) -> &'static str {
        match self {
            BudgetScope::ThisMonth => "this_month",
            BudgetScope::ThisAndFuture => "this_and_future",
            BudgetScope::FutureOnly => "future_only",
        }
    }
}
