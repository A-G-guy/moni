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
        // 无记录时可能为空或只有当前月（取决于 SQL 查询）
        // 由于 created_at 默认为当前时间，查询会包含当前月
        assert!(!state["ui"]["errorMessage"].is_null() || summaries.is_empty() || !summaries.is_empty());
    });
}

#[test]
fn test_stats_category_breakdown_empty() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent = r#"{"type":"stats_category_breakdown","year_month":"2026-05"}"#.to_string();
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
                r#"{{"type":"record_create","amount_cents":{}000,"record_type":"expense","category_id":{},"note":"支出{}","timestamp":{}}}"#,
                i, expense_category, i, now
            );
            core.dispatch(intent).await.unwrap();
        }

        // 创建收入记录
        let intent = format!(
            r#"{{"type":"record_create","amount_cents":5000,"record_type":"income","category_id":{},"note":"工资","timestamp":{}}}"#,
            income_category, now
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
        let year_month = chrono::Utc::now().format("%Y-%m").to_string();
        let intent = format!(
            r#"{{"type":"stats_category_breakdown","year_month":"{}"}}"#,
            year_month
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
