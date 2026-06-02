mod common;

use chrono::Months;

fn current_ym() -> String {
    chrono::Local::now().format("%Y-%m").to_string()
}

fn next_ym() -> String {
    chrono::Local::now()
        .date_naive()
        .checked_add_months(Months::new(1))
        .expect("下个月日期应可计算")
        .format("%Y-%m")
        .to_string()
}

fn upsert_intent(category_id: Option<i64>, amount_cents: i64, scope: &str) -> String {
    match category_id {
        Some(cid) => format!(
            r#"{{"type":"budget_upsert","category_id":{},"amount_cents":{},"year_month":"{}","scope":"{}"}}"#,
            cid,
            amount_cents,
            current_ym(),
            scope
        ),
        None => format!(
            r#"{{"type":"budget_upsert","category_id":null,"amount_cents":{},"year_month":"{}","scope":"{}"}}"#,
            amount_cents,
            current_ym(),
            scope
        ),
    }
}

fn delete_intent(id: i64, scope: &str) -> String {
    format!(
        r#"{{"type":"budget_delete","id":{},"year_month":"{}","scope":"{}"}}"#,
        id,
        current_ym(),
        scope
    )
}

#[test]
fn test_budget_upsert_total_budget() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent = upsert_intent(None, 500000, "this_and_future");
        let update = core.dispatch(intent).await.expect("设置总预算应成功");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null(), "不应有错误");

        let budgets = state["budgets"].as_array().unwrap();
        assert_eq!(budgets.len(), 1);
        assert!(budgets[0]["categoryId"].is_null());
        assert_eq!(budgets[0]["amountCents"], 500000);
        assert_eq!(budgets[0]["categoryName"], "总预算");
        assert_eq!(budgets[0]["isSnapshot"], false); // 使用的是模板
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
            .as_array()
            .unwrap()
            .iter()
            .find(|c| c["name"] == "餐饮")
            .unwrap()["id"]
            .as_i64()
            .unwrap();

        let intent = upsert_intent(Some(category_id), 200000, "this_and_future");
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
        let intent = format!(
            r#"{{"type":"budget_upsert","category_id":null,"amount_cents":0,"year_month":"{}","scope":"this_and_future"}}"#,
            current_ym()
        );
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
            .as_array()
            .unwrap()
            .iter()
            .find(|c| c["name"] == "工资")
            .unwrap()["id"]
            .as_i64()
            .unwrap();

        let intent = upsert_intent(Some(income_id), 100000, "this_and_future");
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
        let intent = upsert_intent(None, 500000, "this_and_future");
        core.dispatch(intent).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let budget_id = state["budgets"][0]["id"].as_i64().unwrap();

        let delete_intent = delete_intent(budget_id, "this_month");
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
        core.dispatch(upsert_intent(None, 500000, "this_and_future"))
            .await
            .unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let category_id = state["categories"]
            .as_array()
            .unwrap()
            .iter()
            .find(|c| c["name"] == "餐饮")
            .unwrap()["id"]
            .as_i64()
            .unwrap();

        core.dispatch(upsert_intent(Some(category_id), 200000, "this_and_future"))
            .await
            .unwrap();

        let list_intent = format!(
            r#"{{"type":"budget_list","year_month":"{}"}}"#,
            current_ym()
        );
        let update = core.dispatch(list_intent).await.unwrap();
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

        core.dispatch(upsert_intent(Some(category_id), 100000, "this_and_future")).await.unwrap();

        core.dispatch(format!(
            r#"{{"type":"record_create","amount_cents":50000,"record_type":"expense","category_id":{},"note":"","timestamp":null}}"#,
            category_id
        )).await.unwrap();

        let check_intent = format!(
            r#"{{"type":"budget_check","category_id":{},"year_month":"{}","amount_cents":30000}}"#,
            category_id, current_ym()
        );
        let update = core.dispatch(check_intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();

        let result = &state["budgetCheckResult"];
        assert_eq!(result["categoryId"], category_id);
        assert_eq!(result["amountCents"], 30000);
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

        core.dispatch(upsert_intent(Some(category_id), 10000, "this_and_future")).await.unwrap();

        core.dispatch(format!(
            r#"{{"type":"record_create","amount_cents":15000,"record_type":"expense","category_id":{},"note":"","timestamp":null}}"#,
            category_id
        )).await.unwrap();

        let update = core.dispatch(format!(
            r#"{{"type":"budget_list","year_month":"{}"}}"#,
            current_ym()
        )).await.unwrap();
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

        core.dispatch(upsert_intent(Some(category_id), 10000, "this_and_future")).await.unwrap();

        core.dispatch(format!(
            r#"{{"type":"record_create","amount_cents":8500,"record_type":"expense","category_id":{},"note":"","timestamp":null}}"#,
            category_id
        )).await.unwrap();

        let update = core.dispatch(format!(
            r#"{{"type":"budget_list","year_month":"{}"}}"#,
            current_ym()
        )).await.unwrap();
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

        core.dispatch(upsert_intent(Some(category_id), 100000, "this_and_future")).await.unwrap();

        core.dispatch(format!(
            r#"{{"type":"record_create","amount_cents":30000,"record_type":"expense","category_id":{},"note":"","timestamp":null}}"#,
            category_id
        )).await.unwrap();

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

        core.dispatch(upsert_intent(Some(category_id), 100000, "this_and_future")).await.unwrap();

        let create = format!(
            r#"{{"type":"record_create","amount_cents":50000,"record_type":"expense","category_id":{},"note":"","timestamp":null}}"#,
            category_id
        );
        core.dispatch(create).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let record_id = state["records"][0]["id"].as_i64().unwrap();

        core.dispatch(format!(
            r#"{{"type":"record_delete","id":{}}}"#,
            record_id
        )).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let budget = &state["budgets"][0];
        assert_eq!(budget["spentCents"], 0);
        assert_eq!(budget["remainingCents"], 100000);
    });
}

// ===== 月度快照新测试 =====

#[test]
fn test_budget_snapshot_overrides_template() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let category_id = state["categories"]
            .as_array()
            .unwrap()
            .iter()
            .find(|c| c["name"] == "餐饮")
            .unwrap()["id"]
            .as_i64()
            .unwrap();

        // 设置模板 ¥1500
        core.dispatch(upsert_intent(Some(category_id), 150000, "this_and_future"))
            .await
            .unwrap();

        // 设置本月快照 ¥2000（覆盖模板）
        core.dispatch(upsert_intent(Some(category_id), 200000, "this_month"))
            .await
            .unwrap();

        // 查询本月预算应为快照 ¥2000
        let update = core
            .dispatch(format!(
                r#"{{"type":"budget_list","year_month":"{}"}}"#,
                current_ym()
            ))
            .await
            .unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let budget = &state["budgets"][0];
        assert_eq!(budget["amountCents"], 200000);
        assert_eq!(budget["isSnapshot"], true);
    });
}

#[test]
fn test_budget_this_month_does_not_affect_future() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let category_id = state["categories"]
            .as_array()
            .unwrap()
            .iter()
            .find(|c| c["name"] == "餐饮")
            .unwrap()["id"]
            .as_i64()
            .unwrap();

        // 设置模板 ¥1500
        core.dispatch(upsert_intent(Some(category_id), 150000, "this_and_future"))
            .await
            .unwrap();

        // 本月临时改为 ¥3000（仅本月）
        core.dispatch(upsert_intent(Some(category_id), 300000, "this_month"))
            .await
            .unwrap();

        // 查询未来月份应使用模板 ¥1500
        let update = core
            .dispatch(format!(
                r#"{{"type":"budget_list","year_month":"{}"}}"#,
                next_ym()
            ))
            .await
            .unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let budget = &state["budgets"][0];
        assert_eq!(budget["amountCents"], 150000);
        assert_eq!(budget["isSnapshot"], false);
    });
}

#[test]
fn test_budget_future_only_preserves_current_month() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let category_id = state["categories"]
            .as_array()
            .unwrap()
            .iter()
            .find(|c| c["name"] == "餐饮")
            .unwrap()["id"]
            .as_i64()
            .unwrap();

        // 设置模板 ¥1500
        core.dispatch(upsert_intent(Some(category_id), 150000, "this_and_future"))
            .await
            .unwrap();

        // 仅以后月份改为 ¥2000
        core.dispatch(upsert_intent(Some(category_id), 200000, "future_only"))
            .await
            .unwrap();

        // 查询本月应为旧模板 ¥1500（已自动创建快照保留）
        let update = core
            .dispatch(format!(
                r#"{{"type":"budget_list","year_month":"{}"}}"#,
                current_ym()
            ))
            .await
            .unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let budget = &state["budgets"][0];
        assert_eq!(budget["amountCents"], 150000);
        assert_eq!(budget["isSnapshot"], true); // 自动创建的快照

        // 查询未来月份应为新模板 ¥2000
        let update = core
            .dispatch(format!(
                r#"{{"type":"budget_list","year_month":"{}"}}"#,
                next_ym()
            ))
            .await
            .unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let budget = &state["budgets"][0];
        assert_eq!(budget["amountCents"], 200000);
        assert_eq!(budget["isSnapshot"], false);
    });
}

#[test]
fn test_budget_delete_keeps_history() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let category_id = state["categories"]
            .as_array()
            .unwrap()
            .iter()
            .find(|c| c["name"] == "餐饮")
            .unwrap()["id"]
            .as_i64()
            .unwrap();

        // 设置模板 ¥1500
        core.dispatch(upsert_intent(Some(category_id), 150000, "this_and_future"))
            .await
            .unwrap();

        // 为本月创建快照 ¥2000
        core.dispatch(upsert_intent(Some(category_id), 200000, "this_month"))
            .await
            .unwrap();

        // 获取快照 ID 并删除（从本月起停止）
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let budget_id = state["budgets"][0]["id"].as_i64().unwrap();

        core.dispatch(delete_intent(budget_id, "this_month"))
            .await
            .unwrap();

        // 本月预算应为空
        let update = core
            .dispatch(format!(
                r#"{{"type":"budget_list","year_month":"{}"}}"#,
                current_ym()
            ))
            .await
            .unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let budgets = state["budgets"].as_array().unwrap();
        let has_catering = budgets.iter().any(|b| b["categoryId"] == category_id);
        assert!(!has_catering, "本月餐饮预算应已删除");
    });
}

#[test]
fn test_budget_delete_future_only_preserves_current() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let category_id = state["categories"]
            .as_array()
            .unwrap()
            .iter()
            .find(|c| c["name"] == "餐饮")
            .unwrap()["id"]
            .as_i64()
            .unwrap();

        // 设置模板 ¥1500
        core.dispatch(upsert_intent(Some(category_id), 150000, "this_and_future"))
            .await
            .unwrap();

        // 从下月起停止
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let budget_id = state["budgets"][0]["id"].as_i64().unwrap();

        core.dispatch(delete_intent(budget_id, "future_only"))
            .await
            .unwrap();

        // 本月应保留 ¥1500（自动创建快照）
        let update = core
            .dispatch(format!(
                r#"{{"type":"budget_list","year_month":"{}"}}"#,
                current_ym()
            ))
            .await
            .unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let budget = &state["budgets"][0];
        assert_eq!(budget["amountCents"], 150000);

        // 下月应为空
        let update = core
            .dispatch(format!(
                r#"{{"type":"budget_list","year_month":"{}"}}"#,
                next_ym()
            ))
            .await
            .unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let budgets = state["budgets"].as_array().unwrap();
        let has_catering = budgets.iter().any(|b| b["categoryId"] == category_id);
        assert!(!has_catering, "下月餐饮预算应已删除");
    });
}

// ===== 分类变更与预算交互测试 =====

#[test]
fn test_archive_category_clears_budget() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        // 使用"医疗"分类（没有子分类）
        let category_id = state["categories"]
            .as_array()
            .unwrap()
            .iter()
            .find(|c| c["name"] == "医疗")
            .unwrap()["id"]
            .as_i64()
            .unwrap();

        // 设置模板 ¥1500
        core.dispatch(upsert_intent(Some(category_id), 150000, "this_and_future"))
            .await
            .unwrap();

        // 归档分类
        core.dispatch(format!(
            r#"{{"type":"category_archive","id":{}}}"#,
            category_id
        ))
        .await
        .unwrap();

        // 预算列表中不应有医疗预算
        let update = core
            .dispatch(format!(
                r#"{{"type":"budget_list","year_month":"{}"}}"#,
                current_ym()
            ))
            .await
            .unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let budgets = state["budgets"].as_array().unwrap();
        let has_medical = budgets.iter().any(|b| b["categoryId"] == category_id);
        assert!(!has_medical, "归档后医疗预算应被清除");
    });
}

#[test]
fn test_record_create_on_archived_category_fails() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let category_id = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "餐饮").unwrap()["id"]
            .as_i64().unwrap();

        // 归档分类
        core.dispatch(format!(
            r#"{{"type":"category_archive","id":{}}}"#,
            category_id
        )).await.unwrap();

        // 尝试给已归档分类记账
        let update = core.dispatch(format!(
            r#"{{"type":"record_create","amount_cents":10000,"record_type":"expense","category_id":{},"note":"","timestamp":null}}"#,
            category_id
        )).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(!state["ui"]["errorMessage"].is_null(), "已归档分类不能记账");
    });
}

#[test]
fn test_record_create_type_mismatch_fails() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        // 工资是收入分类
        let income_id = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "工资").unwrap()["id"]
            .as_i64().unwrap();

        // 尝试用支出类型记到收入分类
        let update = core.dispatch(format!(
            r#"{{"type":"record_create","amount_cents":10000,"record_type":"expense","category_id":{},"note":"","timestamp":null}}"#,
            income_id
        )).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(!state["ui"]["errorMessage"].is_null(), "类型不匹配应报错");
    });
}

#[test]
fn test_parent_category_id_preserved_after_move() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let categories = state["categories"].as_array().unwrap();

        let catering_id = categories.iter().find(|c| c["name"] == "餐饮").unwrap()["id"].as_i64().unwrap();
        let breakfast_id = categories.iter().find(|c| c["name"] == "早餐").unwrap()["id"].as_i64().unwrap();
        let entertainment_id = categories.iter().find(|c| c["name"] == "娱乐").unwrap()["id"].as_i64().unwrap();

        // 设置餐饮预算 ¥1000
        core.dispatch(upsert_intent(Some(catering_id), 100000, "this_and_future")).await.unwrap();

        // 用早餐分类记账 ¥300（此时早餐属于餐饮）
        core.dispatch(format!(
            r#"{{"type":"record_create","amount_cents":30000,"record_type":"expense","category_id":{},"note":"","timestamp":null}}"#,
            breakfast_id
        )).await.unwrap();

        // 验证餐饮预算已用 ¥300
        let update = core.dispatch(format!(
            r#"{{"type":"budget_list","year_month":"{}"}}"#,
            current_ym()
        )).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let catering_budget = state["budgets"].as_array().unwrap()
            .iter().find(|b| b["categoryId"] == catering_id).unwrap();
        assert_eq!(catering_budget["spentCents"], 30000);

        // 将早餐从餐饮移到娱乐
        core.dispatch(format!(
            r#"{{"type":"category_update","id":{},"parent_id":{},"clear_parent_id":false}}"#,
            breakfast_id, entertainment_id
        )).await.unwrap();

        // 重新查询本月预算：餐饮预算仍应按旧记录计算（¥300），不受分类移动影响
        let update = core.dispatch(format!(
            r#"{{"type":"budget_list","year_month":"{}"}}"#,
            current_ym()
        )).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let catering_budget = state["budgets"].as_array().unwrap()
            .iter().find(|b| b["categoryId"] == catering_id).unwrap();
        assert_eq!(catering_budget["spentCents"], 30000, "历史支出不应因分类移动而漂移");
    });
}

// ===== 边界条件与加固测试 =====

#[test]
fn test_budget_future_only_december_rollover() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let category_id = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "餐饮").unwrap()["id"]
            .as_i64().unwrap();

        // 先设置旧模板 ¥1000
        core.dispatch(format!(
            r#"{{"type":"budget_upsert","category_id":{},"amount_cents":100000,"year_month":"2026-12","scope":"this_and_future"}}"#,
            category_id
        )).await.unwrap();

        // 在 2026-12 使用 future_only 修改为 ¥2000
        core.dispatch(format!(
            r#"{{"type":"budget_upsert","category_id":{},"amount_cents":200000,"year_month":"2026-12","scope":"future_only"}}"#,
            category_id
        )).await.unwrap();

        // 验证 2026-12 保留了旧模板快照（自动创建以保留当前月）
        let update = core.dispatch(format!(
            r#"{{"type":"budget_list","year_month":"2026-12"}}"#
        )).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let dec_budget = state["budgets"].as_array().unwrap()
            .iter().find(|b| b["categoryId"] == category_id).unwrap();
        assert_eq!(dec_budget["isSnapshot"], true);

        // 验证 2027-01 使用了新模板（跨年正确）
        let update = core.dispatch(format!(
            r#"{{"type":"budget_list","year_month":"2027-01"}}"#
        )).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let jan_budgets = state["budgets"].as_array().unwrap();
        let jan_catering = jan_budgets.iter().find(|b| b["categoryId"] == category_id);
        assert!(jan_catering.is_some(), "2027-01 应存在餐饮预算");
        assert_eq!(jan_catering.unwrap()["amountCents"], 200000);
        assert_eq!(jan_catering.unwrap()["isSnapshot"], false);
    });
}

#[test]
fn test_budget_upsert_invalid_year_month_fails() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent = format!(
            r#"{{"type":"budget_upsert","category_id":null,"amount_cents":100000,"year_month":"invalid","scope":"this_month"}}"#
        );
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(!state["ui"]["errorMessage"].is_null(), "无效 year_month 应报错");
    });
}

#[test]
fn test_budget_subcategory_independent_budget() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let categories = state["categories"].as_array().unwrap();

        let catering_id = categories.iter().find(|c| c["name"] == "餐饮").unwrap()["id"].as_i64().unwrap();
        let breakfast_id = categories.iter().find(|c| c["name"] == "早餐").unwrap()["id"].as_i64().unwrap();

        // 设置父级（餐饮）预算 ¥1000
        core.dispatch(upsert_intent(Some(catering_id), 100000, "this_and_future")).await.unwrap();
        // 设置子级（早餐）预算 ¥300
        core.dispatch(upsert_intent(Some(breakfast_id), 30000, "this_and_future")).await.unwrap();

        // 用早餐分类记账 ¥150
        core.dispatch(format!(
            r#"{{"type":"record_create","amount_cents":15000,"record_type":"expense","category_id":{},"note":"","timestamp":null}}"#,
            breakfast_id
        )).await.unwrap();

        // 查询预算
        let update = core.dispatch(format!(
            r#"{{"type":"budget_list","year_month":"{}"}}"#,
            current_ym()
        )).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let budgets = state["budgets"].as_array().unwrap();

        let catering_budget = budgets.iter().find(|b| b["categoryId"] == catering_id).unwrap();
        let breakfast_budget = budgets.iter().find(|b| b["categoryId"] == breakfast_id).unwrap();

        // 父级预算已用 = 早餐支出 ¥150（因为早餐当时归属餐饮）
        assert_eq!(catering_budget["spentCents"], 15000);
        // 子级预算已用 = 仅早餐直接支出 ¥150
        assert_eq!(breakfast_budget["spentCents"], 15000);
    });
}

#[test]
fn test_budget_check_income_category_fails() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let income_id = state["categories"]
            .as_array()
            .unwrap()
            .iter()
            .find(|c| c["name"] == "工资")
            .unwrap()["id"]
            .as_i64()
            .unwrap();

        let intent = format!(
            r#"{{"type":"budget_check","category_id":{},"year_month":"{}","amount_cents":10000}}"#,
            income_id,
            current_ym()
        );
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(
            !state["ui"]["errorMessage"].is_null(),
            "收入分类不应支持预算检查"
        );
    });
}

#[test]
fn test_budget_check_nonexistent_category_fails() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent = format!(
            r#"{{"type":"budget_check","category_id":99999,"year_month":"{}","amount_cents":10000}}"#,
            current_ym()
        );
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(!state["ui"]["errorMessage"].is_null(), "不存在的分类不应支持预算检查");
    });
}

#[test]
fn test_budget_status_exactly_80_percent() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let category_id = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "餐饮").unwrap()["id"]
            .as_i64().unwrap();

        // 预算 ¥1000，支出 ¥800（正好 80%）
        core.dispatch(upsert_intent(Some(category_id), 100000, "this_and_future")).await.unwrap();
        core.dispatch(format!(
            r#"{{"type":"record_create","amount_cents":80000,"record_type":"expense","category_id":{},"note":"","timestamp":null}}"#,
            category_id
        )).await.unwrap();

        let update = core.dispatch(format!(
            r#"{{"type":"budget_list","year_month":"{}"}}"#,
            current_ym()
        )).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let budget = &state["budgets"][0];
        assert_eq!(budget["status"], "critical", "正好 80% 应为 critical");
        assert_eq!(budget["spentCents"], 80000);
    });
}

#[test]
fn test_budget_status_exactly_100_percent() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let category_id = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "餐饮").unwrap()["id"]
            .as_i64().unwrap();

        // 预算 ¥1000，支出 ¥1000（正好 100%）
        core.dispatch(upsert_intent(Some(category_id), 100000, "this_and_future")).await.unwrap();
        core.dispatch(format!(
            r#"{{"type":"record_create","amount_cents":100000,"record_type":"expense","category_id":{},"note":"","timestamp":null}}"#,
            category_id
        )).await.unwrap();

        let update = core.dispatch(format!(
            r#"{{"type":"budget_list","year_month":"{}"}}"#,
            current_ym()
        )).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let budget = &state["budgets"][0];
        assert_eq!(budget["status"], "critical", "正好 100% 应为 critical（不超支但已用完）");
        assert_eq!(budget["spentCents"], 100000);
        assert_eq!(budget["remainingCents"], 0);
    });
}

#[test]
fn test_budget_refresh_after_record_update() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let category_id = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "餐饮").unwrap()["id"]
            .as_i64().unwrap();

        core.dispatch(upsert_intent(Some(category_id), 100000, "this_and_future")).await.unwrap();

        // 创建记录 ¥300
        core.dispatch(format!(
            r#"{{"type":"record_create","amount_cents":30000,"record_type":"expense","category_id":{},"note":"","timestamp":null}}"#,
            category_id
        )).await.unwrap();

        // 获取记录 ID
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let record_id = state["records"][0]["id"].as_i64().unwrap();

        // 更新记录金额为 ¥500
        core.dispatch(format!(
            r#"{{"type":"record_update","id":{},"amount_cents":50000,"record_type":"expense","category_id":{},"note":""}}"#,
            record_id, category_id
        )).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let budget = &state["budgets"].as_array().unwrap()
            .iter().find(|b| b["categoryId"] == category_id).unwrap();
        assert_eq!(budget["spentCents"], 50000, "更新后预算应重新计算");
        assert_eq!(budget["remainingCents"], 50000);
    });
}

#[test]
fn test_budget_check_result_cleared_on_list() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let category_id = state["categories"]
            .as_array()
            .unwrap()
            .iter()
            .find(|c| c["name"] == "餐饮")
            .unwrap()["id"]
            .as_i64()
            .unwrap();

        core.dispatch(upsert_intent(Some(category_id), 100000, "this_and_future"))
            .await
            .unwrap();

        // 调用 budget_check
        core.dispatch(format!(
            r#"{{"type":"budget_check","category_id":{},"year_month":"{}","amount_cents":10000}}"#,
            category_id,
            current_ym()
        ))
        .await
        .unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        assert!(
            state["budgetCheckResult"].is_object(),
            "budget_check 后应有结果"
        );

        // 调用 budget_list 后 budget_check_result 应被清空
        core.dispatch(format!(
            r#"{{"type":"budget_list","year_month":"{}"}}"#,
            current_ym()
        ))
        .await
        .unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        assert!(
            state["budgetCheckResult"].is_null(),
            "budget_list 后应清空 budget_check_result"
        );
    });
}

#[test]
fn test_total_budget_snapshot_no_duplicate() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        // 连续三次修改总预算快照（同一月份）
        core.dispatch(upsert_intent(None, 500000, "this_month"))
            .await
            .unwrap();
        core.dispatch(upsert_intent(None, 600000, "this_month"))
            .await
            .unwrap();
        core.dispatch(upsert_intent(None, 700000, "this_month"))
            .await
            .unwrap();

        let update = core
            .dispatch(format!(
                r#"{{"type":"budget_list","year_month":"{}"}}"#,
                current_ym()
            ))
            .await
            .unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let budgets = state["budgets"].as_array().unwrap();

        // 总预算快照应只有一条，且为最新金额
        let total_budgets: Vec<_> = budgets
            .iter()
            .filter(|b| b["categoryId"].is_null())
            .collect();
        assert_eq!(total_budgets.len(), 1, "总预算快照不应重复插入");
        assert_eq!(total_budgets[0]["amountCents"], 700000);
    });
}

#[test]
fn test_total_budget_template_no_duplicate() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        // 连续三次修改总预算模板
        core.dispatch(upsert_intent(None, 500000, "this_and_future"))
            .await
            .unwrap();
        core.dispatch(upsert_intent(None, 600000, "this_and_future"))
            .await
            .unwrap();
        core.dispatch(upsert_intent(None, 700000, "this_and_future"))
            .await
            .unwrap();

        // 查询未来月份应只有一条总预算模板
        let update = core
            .dispatch(format!(
                r#"{{"type":"budget_list","year_month":"{}"}}"#,
                next_ym()
            ))
            .await
            .unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let budgets = state["budgets"].as_array().unwrap();

        let total_budgets: Vec<_> = budgets
            .iter()
            .filter(|b| b["categoryId"].is_null())
            .collect();
        assert_eq!(total_budgets.len(), 1, "总预算模板不应重复插入");
        assert_eq!(total_budgets[0]["amountCents"], 700000);
        assert_eq!(total_budgets[0]["isSnapshot"], false);
    });
}
