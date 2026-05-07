use moni_contracts::budget::BudgetStatus;

/// 前端消费用的预算 DTO，包含计算后的实时状态字段。
#[derive(Debug, Clone, serde::Serialize, serde::Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct BudgetDto {
    pub id: i64,
    pub category_id: Option<i64>,
    pub category_name: Option<String>,
    pub amount_cents: i64,
    pub period_type: String,
    pub created_at: i64,
    pub updated_at: i64,
    /// 该预算范围内的实际已用金额（实时计算）
    pub spent_cents: i64,
    /// 名义剩余 = amount_cents - spent_cents
    pub remaining_cents: i64,
    /// 使用率（0.0 ~ N.N）
    pub percentage: f64,
    /// 预算状态：safe / critical / overrun
    pub status: String,
    /// 是否为月度快照（true=快照，false=继承模板）
    pub is_snapshot: bool,
}

impl BudgetDto {
    pub fn from_budget(budget: &moni_contracts::budget::Budget) -> Self {
        Self {
            id: budget.id,
            category_id: budget.category_id,
            category_name: None,
            amount_cents: budget.amount_cents,
            period_type: "monthly".to_string(),
            created_at: budget.created_at,
            updated_at: budget.updated_at,
            spent_cents: 0,
            remaining_cents: budget.amount_cents,
            percentage: 0.0,
            status: BudgetStatus::Safe.as_str().to_string(),
            is_snapshot: budget.year_month.is_some(),
        }
    }
}
