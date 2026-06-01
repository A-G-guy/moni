use crate::dto::{BudgetDto, RecordDayGroup};
use crate::shared::date_utils;
use crate::shared::types::AmountCents;
use moni_contracts::stats::{CategoryBreakdown, MonthlySummary};

/// 计算月度概览指标。
///
/// 逻辑与 Kotlin `calculateOverviewMetrics()` 逐行对齐。
pub fn calculate_overview_metrics(
    selected_year_month: &str,
    record_groups: &[RecordDayGroup],
    monthly_summaries: &[MonthlySummary],
    budgets: &[BudgetDto],
    today: &str,
) -> crate::models::state::OverviewMetrics {
    use chrono::{Datelike, NaiveDate};

    let (sel_year, sel_month) = match date_utils::parse_year_month(selected_year_month) {
        Some(v) => v,
        None => return crate::models::state::OverviewMetrics::default(),
    };

    let total_days = date_utils::days_in_month(sel_year, sel_month);

    let today_date = match NaiveDate::parse_from_str(today, "%Y-%m-%d").ok() {
        Some(d) => d,
        None => return crate::models::state::OverviewMetrics::default(),
    };
    let today_year = today_date.year();
    let today_month = today_date.month();
    let today_day = today_date.day();

    let (elapsed_days, remaining_days) = date_utils::calculate_day_counts(
        sel_year,
        sel_month,
        today_year,
        today_month,
        today_day,
        total_days,
    );

    // 月度汇总
    let summary = monthly_summaries
        .iter()
        .find(|s| s.year_month == selected_year_month);
    let month_expense = summary.map(|s| s.expense_cents).unwrap_or(0);
    let month_income = summary.map(|s| s.income_cents).unwrap_or(0);
    let month_balance = summary.map(|s| s.balance_cents).unwrap_or(0);

    let is_current_month = sel_year == today_year && sel_month == today_month;
    let is_future_month =
        sel_year > today_year || (sel_year == today_year && sel_month > today_month);

    // 今日支出（仅当前月有效；今日无记录时视为 0）
    let today_expense = if is_current_month {
        let today_str = today_date.format("%Y-%m-%d").to_string();
        Some(
            record_groups
                .iter()
                .find(|g| g.date == today_str)
                .map(|g| g.expense_cents)
                .unwrap_or(0),
        )
    } else {
        None
    };

    // 日均支出（未来月不显示）
    let daily_avg = if is_future_month {
        None
    } else {
        Some(month_expense / elapsed_days.max(1) as i64)
    };

    // 总预算
    let total_budget = budgets.iter().find(|b| b.category_id.is_none()).cloned();

    // 累加日期严格早于指定日期的支出（`%Y-%m-%d` 字典序比较）。
    let expense_before_today = expense_before_date(record_groups, today);

    // 日均剩余：当前月有总预算时，作为今日支出的日初对照线；否则保留原剩余天数口径。
    let daily_remaining = if let Some(ref budget) = total_budget {
        if is_current_month {
            let days_from_today = total_days.saturating_sub(today_day) + 1;
            Some((budget.amount_cents - expense_before_today) / days_from_today as i64)
        } else if is_future_month {
            Some(budget.amount_cents / total_days as i64)
        } else {
            None
        }
    } else if remaining_days > 0 {
        Some(month_balance / remaining_days as i64)
    } else {
        None
    };

    crate::models::state::OverviewMetrics {
        month_expense,
        month_income,
        month_balance,
        today_expense,
        daily_avg,
        daily_remaining,
        total_budget,
        elapsed_days,
        total_days: total_days as i32,
        remaining_days,
    }
}

/// 累加日期严格早于指定日期的支出（`%Y-%m-%d` 字典序比较）。
fn expense_before_date(record_groups: &[RecordDayGroup], today: &str) -> AmountCents {
    record_groups
        .iter()
        .filter(|g| g.date.as_str() < today)
        .map(|g| g.expense_cents)
        .sum()
}

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
