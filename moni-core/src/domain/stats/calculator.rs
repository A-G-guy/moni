use crate::dto::{BudgetDto, RecordDayGroup};
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

    let (sel_year, sel_month) = match parse_year_month(selected_year_month) {
        Some(v) => v,
        None => return crate::models::state::OverviewMetrics::default(),
    };

    let total_days = days_in_month(sel_year, sel_month);

    let today_date = match NaiveDate::parse_from_str(today, "%Y-%m-%d").ok() {
        Some(d) => d,
        None => return crate::models::state::OverviewMetrics::default(),
    };
    let today_year = today_date.year();
    let today_month = today_date.month();
    let today_day = today_date.day();

    let (elapsed_days, remaining_days) =
        calculate_day_counts(sel_year, sel_month, today_year, today_month, today_day, total_days);

    // 月度汇总
    let summary = monthly_summaries
        .iter()
        .find(|s| s.year_month == selected_year_month);
    let month_expense = summary.map(|s| s.expense_cents).unwrap_or(0);
    let month_income = summary.map(|s| s.income_cents).unwrap_or(0);
    let month_balance = summary.map(|s| s.balance_cents).unwrap_or(0);

    // 今日支出（仅当前月有效）
    let today_expense = if sel_year == today_year && sel_month == today_month {
        let today_str = today_date.format("%Y-%m-%d").to_string();
        record_groups
            .iter()
            .find(|g| g.date == today_str)
            .map(|g| g.expense_cents)
    } else {
        None
    };

    // 日均支出（未来月不显示）
    let daily_avg = if sel_year > today_year || (sel_year == today_year && sel_month > today_month) {
        None
    } else {
        Some(month_expense / elapsed_days.max(1) as i64)
    };

    // 总预算
    let total_budget = budgets.iter().find(|b| b.category_id.is_none()).cloned();

    // 日均剩余：有总预算时为"剩余总预算 / 剩余天数"，否则为"月结余 / 剩余天数"
    let daily_remaining = if remaining_days > 0 {
        if let Some(ref budget) = total_budget {
            Some((budget.amount_cents - month_expense) / remaining_days as i64)
        } else {
            Some(month_balance / remaining_days as i64)
        }
    } else {
        None
    };

    // 总预算进度状态（基于实际支出与理想时间进度的对比）
    let budget_progress_status = total_budget.as_ref().map(|budget| {
        #[allow(clippy::cast_precision_loss)]
        let actual_percentage = if budget.amount_cents > 0 {
            month_expense as f64 / budget.amount_cents as f64
        } else {
            0.0
        };
        #[allow(clippy::cast_precision_loss)]
        let ideal_percentage = if total_days > 0 {
            elapsed_days as f64 / total_days as f64
        } else {
            0.0
        };
        if actual_percentage > ideal_percentage {
            "overrun".to_string()
        } else if actual_percentage > ideal_percentage * 0.9 {
            "warning".to_string()
        } else {
            "normal".to_string()
        }
    });

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
        budget_progress_status,
    }
}

fn parse_year_month(year_month_str: &str) -> Option<(i32, u32)> {
    let parts: Vec<&str> = year_month_str.split('-').collect();
    if parts.len() != 2 {
        return None;
    }
    let year = parts[0].parse::<i32>().ok()?;
    let month = parts[1].parse::<u32>().ok()?;
    if !(1..=12).contains(&month) {
        return None;
    }
    Some((year, month))
}

fn days_in_month(year: i32, month: u32) -> u32 {
    match month {
        1 | 3 | 5 | 7 | 8 | 10 | 12 => 31,
        4 | 6 | 9 | 11 => 30,
        2 => {
            if is_leap_year(year) {
                29
            } else {
                28
            }
        }
        _ => 30,
    }
}

fn is_leap_year(year: i32) -> bool {
    (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}

fn calculate_day_counts(
    sel_year: i32,
    sel_month: u32,
    today_year: i32,
    today_month: u32,
    today_day: u32,
    total_days: u32,
) -> (i32, i32) {
    if sel_year == today_year && sel_month == today_month {
        let elapsed = today_day.max(1) as i32;
        let remaining = (total_days - today_day).max(0) as i32;
        (elapsed, remaining)
    } else if sel_year < today_year || (sel_year == today_year && sel_month < today_month) {
        (total_days as i32, 0)
    } else {
        (0, total_days as i32)
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
