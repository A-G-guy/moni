use moni_contracts::stats::{CategoryBreakdown, MonthlySummary};
use crate::shared::types::AmountCents;

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
    aggregates: Vec<(i64, String, String, AmountCents)>,
) -> Vec<CategoryBreakdown> {
    let total: AmountCents = aggregates.iter().map(|(_, _, _, amount)| amount).sum();
    if total == 0 {
        return Vec::new();
    }

    aggregates
        .into_iter()
        .map(|(category_id, category_name, color_hex, amount_cents)| CategoryBreakdown {
            category_id,
            category_name,
            color_hex,
            amount_cents,
            percentage: (amount_cents as f64 / total as f64) * 100.0,
        })
        .collect()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_monthly_summary() {
        let input = vec![
            ("2026-04".to_string(), 10000, 5000),
            ("2026-05".to_string(), 12000, 8000),
        ];
        let result = calculate_monthly_summary(input);
        assert_eq!(result.len(), 2);
        assert_eq!(result[0].balance_cents, 5000);
        assert_eq!(result[1].balance_cents, 4000);
    }

    #[test]
    fn test_category_breakdown() {
        let input = vec![
            (1, "餐饮".to_string(), "#FF6B6B".to_string(), 6000),
            (2, "交通".to_string(), "#4ECDC4".to_string(), 4000),
        ];
        let result = calculate_category_breakdown(input);
        assert_eq!(result.len(), 2);
        assert_eq!(result[0].percentage, 60.0);
        assert_eq!(result[1].percentage, 40.0);
    }

    #[test]
    fn test_category_breakdown_empty() {
        let result = calculate_category_breakdown(Vec::new());
        assert!(result.is_empty());
    }

    #[test]
    fn test_category_breakdown_zero_total() {
        let input = vec![
            (1, "餐饮".to_string(), "#FF6B6B".to_string(), 0),
        ];
        let result = calculate_category_breakdown(input);
        assert!(result.is_empty());
    }
}
