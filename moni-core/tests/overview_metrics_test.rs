mod common;

use moni_core::domain::stats::calculator;

fn total_budget(amount_cents: i64, spent_cents: i64) -> moni_core::dto::BudgetDto {
    moni_core::dto::BudgetDto {
        id: 1,
        category_id: None,
        category_name: None,
        amount_cents,
        period_type: "monthly".to_string(),
        created_at: 0,
        updated_at: 0,
        spent_cents,
        remaining_cents: amount_cents - spent_cents,
        percentage: spent_cents as f64 / amount_cents as f64,
        status: "safe".to_string(),
        progress_status: None,
        is_snapshot: false,
    }
}

#[test]
fn test_overview_metrics_current_month() {
    let groups = vec![
        moni_core::dto::RecordDayGroup {
            date: "2024-03-15".to_string(),
            income_cents: 0,
            expense_cents: 2000,
            records: vec![],
        },
        moni_core::dto::RecordDayGroup {
            date: "2024-03-14".to_string(),
            income_cents: 0,
            expense_cents: 3000,
            records: vec![],
        },
    ];

    let summaries = vec![moni_contracts::stats::MonthlySummary {
        year_month: "2024-03".to_string(),
        income_cents: 10000,
        expense_cents: 5000,
        balance_cents: 5000,
    }];

    let budgets = vec![total_budget(30000, 5000)];

    let metrics = calculator::calculate_overview_metrics(
        "2024-03",
        &groups,
        &summaries,
        &budgets,
        "2024-03-15",
    );

    assert_eq!(metrics.month_expense, 5000);
    assert_eq!(metrics.month_income, 10000);
    assert_eq!(metrics.month_balance, 5000);
    assert_eq!(metrics.today_expense, Some(2000));
    assert_eq!(metrics.daily_avg, Some(333)); // 5000 / 15
    assert_eq!(metrics.total_days, 31);
    assert_eq!(metrics.elapsed_days, 15);
    assert_eq!(metrics.remaining_days, 16);
    assert!(metrics.total_budget.is_some());
    assert_eq!(metrics.total_budget.as_ref().unwrap().amount_cents, 30000);
    // 有总预算时，日均剩余作为今日支出的稳定对照线：30000 / 31 = 967
    assert_eq!(metrics.daily_remaining, Some(967));
}

#[test]
fn test_overview_metrics_future_month() {
    let metrics = calculator::calculate_overview_metrics("2024-06", &[], &[], &[], "2024-03-15");

    assert_eq!(metrics.month_expense, 0);
    assert_eq!(metrics.total_days, 30);
    assert_eq!(metrics.elapsed_days, 0);
    assert_eq!(metrics.remaining_days, 30);
    assert_eq!(metrics.today_expense, None);
    assert_eq!(metrics.daily_avg, None);
    assert_eq!(metrics.daily_remaining, Some(0));
}

#[test]
fn test_overview_metrics_past_month() {
    let summaries = vec![moni_contracts::stats::MonthlySummary {
        year_month: "2024-02".to_string(),
        income_cents: 20000,
        expense_cents: 8000,
        balance_cents: 12000,
    }];

    let metrics =
        calculator::calculate_overview_metrics("2024-02", &[], &summaries, &[], "2024-03-15");

    assert_eq!(metrics.month_expense, 8000);
    assert_eq!(metrics.total_days, 29); // 2024 是闰年
    assert_eq!(metrics.elapsed_days, 29);
    assert_eq!(metrics.remaining_days, 0);
    assert_eq!(metrics.today_expense, None);
    assert_eq!(metrics.daily_avg, Some(275)); // 8000 / 29
    assert_eq!(metrics.daily_remaining, None); // remaining_days = 0
}

#[test]
fn test_overview_metrics_current_month_no_records_today() {
    // 当前月，但今日无记录：today_expense 应为 Some(0)
    let groups = vec![moni_core::dto::RecordDayGroup {
        date: "2024-03-14".to_string(),
        income_cents: 0,
        expense_cents: 3000,
        records: vec![],
    }];

    let summaries = vec![moni_contracts::stats::MonthlySummary {
        year_month: "2024-03".to_string(),
        income_cents: 0,
        expense_cents: 3000,
        balance_cents: -3000,
    }];

    let metrics =
        calculator::calculate_overview_metrics("2024-03", &groups, &summaries, &[], "2024-03-15");

    assert_eq!(metrics.today_expense, Some(0));
    assert_eq!(metrics.month_expense, 3000);
}

#[test]
fn test_daily_remaining_with_budget_is_stable_daily_cap() {
    let budgets = vec![total_budget(30000, 0)];
    let low_spend_summary = vec![moni_contracts::stats::MonthlySummary {
        year_month: "2024-03".to_string(),
        income_cents: 0,
        expense_cents: 1000,
        balance_cents: -1000,
    }];
    let high_spend_summary = vec![moni_contracts::stats::MonthlySummary {
        year_month: "2024-03".to_string(),
        income_cents: 0,
        expense_cents: 12000,
        balance_cents: -12000,
    }];
    let high_spend_groups = vec![moni_core::dto::RecordDayGroup {
        date: "2024-03-15".to_string(),
        income_cents: 0,
        expense_cents: 1200,
        records: vec![],
    }];

    let low_spend_metrics = calculator::calculate_overview_metrics(
        "2024-03",
        &[],
        &low_spend_summary,
        &budgets,
        "2024-03-15",
    );
    let high_spend_metrics = calculator::calculate_overview_metrics(
        "2024-03",
        &high_spend_groups,
        &high_spend_summary,
        &budgets,
        "2024-03-15",
    );

    assert_eq!(low_spend_metrics.daily_remaining, Some(967));
    assert_eq!(high_spend_metrics.daily_remaining, Some(967));
    assert_eq!(high_spend_metrics.today_expense, Some(1200));
}

#[test]
fn test_daily_remaining_with_budget_is_available_on_last_day() {
    let budgets = vec![total_budget(31000, 30000)];
    let summaries = vec![moni_contracts::stats::MonthlySummary {
        year_month: "2024-03".to_string(),
        income_cents: 0,
        expense_cents: 30000,
        balance_cents: -30000,
    }];

    let metrics =
        calculator::calculate_overview_metrics("2024-03", &[], &summaries, &budgets, "2024-03-31");

    assert_eq!(metrics.remaining_days, 0);
    assert_eq!(metrics.daily_remaining, Some(1000));
}

#[test]
fn test_overview_metrics_no_budget() {
    let summaries = vec![moni_contracts::stats::MonthlySummary {
        year_month: "2024-03".to_string(),
        income_cents: 10000,
        expense_cents: 5000,
        balance_cents: 5000,
    }];

    let metrics = calculator::calculate_overview_metrics(
        "2024-03",
        &[],
        &summaries,
        &[], // 无预算
        "2024-03-15",
    );

    assert_eq!(metrics.month_expense, 5000);
    assert!(metrics.total_budget.is_none());
    // 日均剩余 = 月结余 / 剩余天数 = 5000 / 16 = 312
    assert_eq!(metrics.daily_remaining, Some(312));
}

#[test]
fn test_overview_metrics_leap_year_february() {
    let metrics = calculator::calculate_overview_metrics("2024-02", &[], &[], &[], "2024-02-15");

    assert_eq!(metrics.total_days, 29); // 闰年
    assert_eq!(metrics.elapsed_days, 15);
    assert_eq!(metrics.remaining_days, 14);
}

#[test]
fn test_overview_metrics_non_leap_year_february() {
    let metrics = calculator::calculate_overview_metrics("2023-02", &[], &[], &[], "2023-02-15");

    assert_eq!(metrics.total_days, 28); // 非闰年
    assert_eq!(metrics.elapsed_days, 15);
    assert_eq!(metrics.remaining_days, 13);
}
