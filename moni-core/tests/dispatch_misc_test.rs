mod common;

/// 验证 NavigateTo 意图会更新 `state.ui.activeTab`。
#[test]
fn test_navigate_to_updates_active_tab() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        assert_eq!(state["ui"]["activeTab"], "records", "默认 tab 应为 records");

        let intent = r#"{"type":"navigate_to","screen":"settings"}"#.to_string();
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null());
        assert_eq!(state["ui"]["activeTab"], "settings");

        // 切换到第二个 tab
        let intent = r#"{"type":"navigate_to","screen":"stats"}"#.to_string();
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert_eq!(state["ui"]["activeTab"], "stats");
    });
}

/// 验证 DismissError 意图会清空 `state.ui.errorMessage`。
#[test]
fn test_dismiss_error_clears_error_message() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        // 先制造一个错误（使用不存在的分类创建记录）
        let bad_intent = r#"{"type":"record_create","amount_cents":100,"record_type":"expense","category_id":999999,"note":"","timestamp":null}"#.to_string();
        let update = core.dispatch(bad_intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(
            !state["ui"]["errorMessage"].is_null(),
            "应已设置错误信息"
        );

        // 派发 DismissError 意图
        let intent = r#"{"type":"dismiss_error"}"#.to_string();
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(
            state["ui"]["errorMessage"].is_null(),
            "DismissError 后应清空错误信息"
        );
    });
}

/// 二级分类金额按一级分类聚合：父分类应吸收子分类金额。
#[test]
fn test_stats_category_breakdown_aggregate_by_parent() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();

        // 预设中"餐饮"是一级分类，"早餐"是其子分类
        let parent = state["categories"]
            .as_array()
            .unwrap()
            .iter()
            .find(|c| c["name"] == "餐饮")
            .expect("应存在'餐饮'分类")
            .clone();
        let parent_id = parent["id"].as_i64().unwrap();

        let child = state["categories"]
            .as_array()
            .unwrap()
            .iter()
            .find(|c| c["name"] == "早餐")
            .expect("应存在'早餐'分类");
        let child_id = child["id"].as_i64().unwrap();
        assert_eq!(child["parentId"].as_i64(), Some(parent_id), "早餐应是餐饮的子分类");

        // 当月的固定时间戳
        let now = chrono::Utc::now();
        let timestamp = now.timestamp();
        let year_month = now.format("%Y-%m").to_string();

        // 在父分类记 100 元，在子分类记 50 元
        let intent = format!(
            r#"{{"type":"record_create","amount_cents":10000,"record_type":"expense","category_id":{parent_id},"note":"父","timestamp":{timestamp}}}"#
        );
        core.dispatch(intent).await.unwrap();

        let intent = format!(
            r#"{{"type":"record_create","amount_cents":5000,"record_type":"expense","category_id":{child_id},"note":"子","timestamp":{timestamp}}}"#
        );
        core.dispatch(intent).await.unwrap();

        // aggregate_by_parent=false 时父子分别计为两条
        let intent = format!(
            r#"{{"type":"stats_category_breakdown","year_month":"{year_month}","aggregate_by_parent":false}}"#
        );
        let update = core.dispatch(intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let breakdown = state["currentMonthBreakdown"].as_array().unwrap();
        assert_eq!(breakdown.len(), 2, "未聚合时应分两条");

        // aggregate_by_parent=true 时子分类金额并入父分类，仅一条
        let intent = format!(
            r#"{{"type":"stats_category_breakdown","year_month":"{year_month}","aggregate_by_parent":true}}"#
        );
        let update = core.dispatch(intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let breakdown = state["currentMonthBreakdown"].as_array().unwrap();
        assert_eq!(breakdown.len(), 1, "聚合后应仅一条");
        assert_eq!(
            breakdown[0]["amountCents"], 15000,
            "父分类应吸收子分类金额（10000 + 5000）"
        );
        assert_eq!(breakdown[0]["categoryId"].as_i64(), Some(parent_id));
        assert_eq!(breakdown[0]["percentage"], 100.0);
    });
}

/// 验证非法意图 JSON 会被解析层拒绝（dispatch 直接 Err，而非写入 errorMessage）。
#[test]
fn test_dispatch_rejects_malformed_intent_json() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let result = core
            .dispatch(r#"{"type":"unknown_intent_does_not_exist"}"#.to_string())
            .await;
        assert!(result.is_err(), "未知意图应被解析层直接拒绝");

        let result = core.dispatch("not_a_json".to_string()).await;
        assert!(result.is_err(), "无效 JSON 应被解析层直接拒绝");
    });
}
