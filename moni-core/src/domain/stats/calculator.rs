use crate::shared::types::AmountCents;
use moni_contracts::stats::{CategoryBreakdown, MonthlySummary};

/// 计算月度收支汇总。
pub fn calculate_monthly_summary(
    aggregates: Vec<(String, AmountCents, AmountCents)>,
) -> Vec<MonthlySummary> {
    aggregates
        .into_iter()
        .map(|(year_month, income_cents, expense_cents)| MonthlySummary {
            year_month,
            income_cents,
            expense_cents,
            balance_cents: income_cents - expense_cents,
        })
        .collect()
}

/// 计算分类支出占比。
pub fn calculate_category_breakdown(
    aggregates: Vec<(i64, String, AmountCents)>,
) -> Vec<CategoryBreakdown> {
    let total: AmountCents = aggregates.iter().map(|(_, _, amount)| amount).sum();
    if total == 0 {
        return Vec::new();
    }

    aggregates
        .into_iter()
        .map(
            |(category_id, category_name, amount_cents)| CategoryBreakdown {
                category_id,
                category_name,
                amount_cents,
                // 金额以分为单位、远小于 i64::MAX，转 f64 的精度损失对前端百分比无实际影响
                #[allow(clippy::cast_precision_loss)]
                percentage: (amount_cents as f64 / total as f64) * 100.0,
            },
        )
        .collect()
}
