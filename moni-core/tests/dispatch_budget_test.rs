mod common;

#[test]
fn test_budget_upsert_total_budget() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent = r#"{"type":"budget_upsert","category_id":null,"amount_cents":500000}"#.to_string();
        let update = core.dispatch(intent).await.expect("设置总预算应成功");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null(), "不应有错误");

        let budgets = state["budgets"].as_array().unwrap();
        assert_eq!(budgets.len(), 1);
        assert!(budgets[0]["categoryId"].is_null());
        assert_eq!(budgets[0]["amountCents"], 500000);
        assert_eq!(budgets[0]["categoryName"], "总预算");
    });
}

#[test]
fn test_budget_upsert_category_budget() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let category_id = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "餐饮").unwrap()["id"]
            .as_i64().unwrap();

        let intent = format!(
            r#"{{"type":"budget_upsert","category_id":{},"amount_cents":200000}}"#,
            category_id
        );
        let update = core.dispatch(intent).await.expect("设置分类预算应成功");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null());

        let budgets = state["budgets"].as_array().unwrap();
        assert_eq!(budgets.len(), 1);
        assert_eq!(budgets[0]["categoryId"], category_id);
        assert_eq!(budgets[0]["amountCents"], 200000);
        assert_eq!(budgets[0]["categoryName"], "餐饮");
    });
}

#[test]
fn test_budget_upsert_invalid_amount_fails() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent = r#"{"type":"budget_upsert","category_id":null,"amount_cents":0}"#.to_string();
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(!state["ui"]["errorMessage"].is_null(), "金额必须大于0");
    });
}

#[test]
fn test_budget_upsert_income_category_fails() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let income_id = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "工资").unwrap()["id"]
            .as_i64().unwrap();

        let intent = format!(
            r#"{{"type":"budget_upsert","category_id":{},"amount_cents":100000}}"#,
            income_id
        );
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(!state["ui"]["errorMessage"].is_null(), "预算仅支持支出分类");
    });
}

#[test]
fn test_budget_delete_success() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        // 先创建预算
        let intent = r#"{"type":"budget_upsert","category_id":null,"amount_cents":500000}"#.to_string();
        core.dispatch(intent).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let budget_id = state["budgets"][0]["id"].as_i64().unwrap();

        // 删除预算
        let delete_intent = format!(r#"{{"type":"budget_delete","id":{}}}"#, budget_id);
        let update = core.dispatch(delete_intent).await.expect("删除应成功");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null());
        assert!(state["budgets"].as_array().unwrap().is_empty());
    });
}

#[test]
fn test_budget_list_populates_state() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        // 创建总预算和分类预算
        core.dispatch(r#"{"type":"budget_upsert","category_id":null,"amount_cents":500000}"#.to_string()).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let category_id = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "餐饮").unwrap()["id"]
            .as_i64().unwrap();

        core.dispatch(format!(
            r#"{{"type":"budget_upsert","category_id":{},"amount_cents":200000}}"#,
            category_id
        )).await.unwrap();

        // 调用 budget_list
        let update = core.dispatch(r#"{"type":"budget_list"}"#.to_string()).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let budgets = state["budgets"].as_array().unwrap();
        assert_eq!(budgets.len(), 2);
    });
}

#[test]
fn test_budget_check_returns_result() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let category_id = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "餐饮").unwrap()["id"]
            .as_i64().unwrap();

        // 设置餐饮预算
        core.dispatch(format!(
            r#"{{"type":"budget_upsert","category_id":{},"amount_cents":100000}}"#,
            category_id
        )).await.unwrap();

        // 创建一笔支出
        core.dispatch(format!(
            r#"{{"type":"record_create","amount_cents":50000,"record_type":"expense","category_id":{},"note":"","timestamp":null}}"#,
            category_id
        )).await.unwrap();

        // 预算检查
        let year_month = chrono::Utc::now().format("%Y-%m").to_string();
        let check_intent = format!(
            r#"{{"type":"budget_check","category_id":{},"year_month":"{}","amount_cents":30000}}"#,
            category_id, year_month
        );
        let update = core.dispatch(check_intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();

        let result = &state["budgetCheckResult"];
        assert_eq!(result["categoryId"], category_id);
        assert_eq!(result["amountCents"], 30000);
        // 餐饮预算 ¥1000，已用 ¥500，剩余 ¥500，输入 ¥300，检查后应为 safe
        assert!(result["effectiveAvailable"].is_number());
    });
}

#[test]
fn test_budget_status_overrun() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let category_id = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "餐饮").unwrap()["id"]
            .as_i64().unwrap();

        // 设置餐饮预算 ¥100
        core.dispatch(format!(
            r#"{{"type":"budget_upsert","category_id":{},"amount_cents":10000}}"#,
            category_id
        )).await.unwrap();

        // 支出 ¥150（超支）
        core.dispatch(format!(
            r#"{{"type":"record_create","amount_cents":15000,"record_type":"expense","category_id":{},"note":"","timestamp":null}}"#,
            category_id
        )).await.unwrap();

        let update = core.dispatch(r#"{"type":"budget_list"}"#.to_string()).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();

        let budget = &state["budgets"][0];
        assert_eq!(budget["status"], "overrun");
        assert_eq!(budget["spentCents"], 15000);
        assert_eq!(budget["remainingCents"], -5000);
    });
}

#[test]
fn test_budget_status_critical() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let category_id = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "餐饮").unwrap()["id"]
            .as_i64().unwrap();

        // 设置餐饮预算 ¥100
        core.dispatch(format!(
            r#"{{"type":"budget_upsert","category_id":{},"amount_cents":10000}}"#,
            category_id
        )).await.unwrap();

        // 支出 ¥85（临界）
        core.dispatch(format!(
            r#"{{"type":"record_create","amount_cents":8500,"record_type":"expense","category_id":{},"note":"","timestamp":null}}"#,
            category_id
        )).await.unwrap();

        let update = core.dispatch(r#"{"type":"budget_list"}"#.to_string()).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();

        let budget = &state["budgets"][0];
        assert_eq!(budget["status"], "critical");
        assert_eq!(budget["spentCents"], 8500);
    });
}

#[test]
fn test_budget_refresh_after_record_create() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let category_id = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "餐饮").unwrap()["id"]
            .as_i64().unwrap();

        // 设置预算
        core.dispatch(format!(
            r#"{{"type":"budget_upsert","category_id":{},"amount_cents":100000}}"#,
            category_id
        )).await.unwrap();

        // 创建支出记录
        core.dispatch(format!(
            r#"{{"type":"record_create","amount_cents":30000,"record_type":"expense","category_id":{},"note":"","timestamp":null}}"#,
            category_id
        )).await.unwrap();

        // 创建后预算应自动刷新
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let budget = &state["budgets"][0];
        assert_eq!(budget["spentCents"], 30000);
        assert_eq!(budget["remainingCents"], 70000);
        assert_eq!(budget["status"], "safe");
    });
}

#[test]
fn test_budget_refresh_after_record_delete() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let category_id = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "餐饮").unwrap()["id"]
            .as_i64().unwrap();

        // 设置预算
        core.dispatch(format!(
            r#"{{"type":"budget_upsert","category_id":{},"amount_cents":100000}}"#,
            category_id
        )).await.unwrap();

        // 创建支出记录
        let create = format!(
            r#"{{"type":"record_create","amount_cents":50000,"record_type":"expense","category_id":{},"note":"","timestamp":null}}"#,
            category_id
        );
        core.dispatch(create).await.unwrap();

        // 获取记录 ID
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let record_id = state["records"][0]["id"].as_i64().unwrap();

        // 删除记录
        core.dispatch(format!(
            r#"{{"type":"record_delete","id":{}}}"#,
            record_id
        )).await.unwrap();

        // 删除后预算应自动刷新
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let budget = &state["budgets"][0];
        assert_eq!(budget["spentCents"], 0);
        assert_eq!(budget["remainingCents"], 100000);
    });
}
