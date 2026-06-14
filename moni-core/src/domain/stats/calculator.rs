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

    // 今日支出（仅当前月有效；当前月无记录时为 0）。
    let today_expense_for_daily = if is_current_month {
        today_expense.unwrap_or(0)
    } else {
        0
    };

    // 日均剩余：当天冻结的日消费额度。
    //
    // 设计目标：用户希望用「今日支出 < 日均剩余」控制总预算不超支，
    // 但提前记的未来支出会导致参考线实时跳动。
    // 因此把「今日支出」从分子里剔除（即按“昨日及以前支出 + 未来支出”计算），
    // 再把分母从“今日之后剩余天数”改为“包含今天的剩余天数”，
    // 这样：
    // - 记今天的账 -> 分子里本月总支出与今日支出同量增加，日均剩余不变；
    // - 记未来/过去日期的账 -> 分子变化，日均剩余实时更新；
    // - 每天遵守 今日支出 <= 日均剩余，即可保证总预算不超支。
    let daily_remaining = if is_current_month {
        let days_including_today = remaining_days + 1;
        if let Some(ref budget) = total_budget {
            let remaining_budget_excluding_today =
                budget.amount_cents - month_expense + today_expense_for_daily;
            if days_including_today > 0 {
                Some(remaining_budget_excluding_today / days_including_today as i64)
            } else {
                Some(remaining_budget_excluding_today)
            }
        } else if days_including_today > 0 {
            let remaining_balance_excluding_today =
                month_balance + today_expense_for_daily;
            Some(remaining_balance_excluding_today / days_including_today as i64)
        } else {
            None
        }
    } else if is_future_month {
        if let Some(ref budget) = total_budget {
            if total_days > 0 {
                Some(budget.amount_cents / total_days as i64)
            } else {
                Some(budget.amount_cents)
            }
        } else if total_days > 0 {
            Some(month_balance / total_days as i64)
        } else {
            None
        }
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
