mod common;

#[test]
fn test_stats_monthly_summary_empty() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent = r#"{"type":"stats_monthly_summary","months":6}"#.to_string();
        let update = core.dispatch(intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null());
        let summaries = state["monthlySummaries"].as_array().unwrap();
        // 无任何 record 时 SQL 查询不会回填零值月份，应直接为空数组。
        assert!(
            summaries.is_empty(),
            "无任何 record 时月度汇总应为空，实际: {summaries:?}"
        );
    });
}

#[test]
fn test_stats_category_breakdown_empty() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent = r#"{"type":"stats_category_breakdown","year_month":"2026-05","aggregate_by_parent":false}"#.to_string();
        let update = core.dispatch(intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null());
        let breakdown = state["currentMonthBreakdown"].as_array().unwrap();
        assert!(breakdown.is_empty());
    });
}

#[test]
fn test_stats_with_records() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let expense_category = state["categories"]
            .as_array()
            .unwrap()
            .iter()
            .find(|c| c["categoryType"] == "expense")
            .unwrap()["id"]
            .as_i64()
            .unwrap();
        let income_category = state["categories"]
            .as_array()
            .unwrap()
            .iter()
            .find(|c| c["categoryType"] == "income")
            .unwrap()["id"]
            .as_i64()
            .unwrap();

        let now = chrono::Utc::now().timestamp();

        // 创建支出记录
        for i in 1..=3 {
            let intent = format!(
                r#"{{"type":"record_create","amount_cents":{i}000,"record_type":"expense","category_id":{expense_category},"note":"支出{i}","timestamp":{now}}}"#
            );
            core.dispatch(intent).await.unwrap();
        }

        // 创建收入记录
        let intent = format!(
            r#"{{"type":"record_create","amount_cents":5000,"record_type":"income","category_id":{income_category},"note":"工资","timestamp":{now}}}"#
        );
        core.dispatch(intent).await.unwrap();

        // 查询月度统计
        let intent = r#"{"type":"stats_monthly_summary","months":6}"#.to_string();
        let update = core.dispatch(intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null());
        let summaries = state["monthlySummaries"].as_array().unwrap();
        assert!(!summaries.is_empty());

        let current = &summaries[summaries.len() - 1];
        assert_eq!(current["incomeCents"], 5000);
        assert_eq!(current["expenseCents"], 6000); // 1000 + 2000 + 3000
        assert_eq!(current["balanceCents"], -1000);

        // 查询分类占比（当前月）
        let year_month = chrono::Local::now().format("%Y-%m").to_string();
        let intent = format!(
            r#"{{"type":"stats_category_breakdown","year_month":"{year_month}","aggregate_by_parent":false}}"#
        );
        let update = core.dispatch(intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null());
        let breakdown = state["currentMonthBreakdown"].as_array().unwrap();
        assert_eq!(breakdown.len(), 1); // 只有一个支出分类有记录
        assert_eq!(breakdown[0]["amountCents"], 6000);
        assert_eq!(breakdown[0]["percentage"], 100.0);
    });
}
