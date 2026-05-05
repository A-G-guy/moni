mod common;

#[test]
fn test_record_create_success() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let categories = state["categories"].as_array().unwrap();
        assert!(!categories.is_empty(), "应有预设分类");
        let category_id = categories[0]["id"].as_i64().unwrap();

        let intent = format!(
            r#"{{"type":"record_create","amount_cents":1234,"record_type":"expense","category_id":{category_id},"note":"测试","timestamp":null}}"#
        );
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(
            state["ui"]["errorMessage"].is_null(),
            "不应有错误: {:?}",
            state["ui"]["errorMessage"]
        );
        let records = state["records"].as_array().unwrap();
        assert_eq!(records.len(), 1);
        assert_eq!(records[0]["amountCents"], 1234);
        assert_eq!(records[0]["recordType"], "expense");
    });
}

#[test]
fn test_record_create_invalid_amount() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent = r#"{"type":"record_create","amount_cents":0,"record_type":"expense","category_id":1,"note":"","timestamp":null}"#.to_string();
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(
            !state["ui"]["errorMessage"].is_null(),
            "金额为零应返回业务错误"
        );
    });
}

#[test]
fn test_record_create_negative_amount() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent = r#"{"type":"record_create","amount_cents":-100,"record_type":"expense","category_id":1,"note":"","timestamp":null}"#.to_string();
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(
            !state["ui"]["errorMessage"].is_null(),
            "负金额应返回业务错误"
        );
    });
}

#[test]
fn test_record_create_category_not_found() {
    let core = common::setup_core();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent = r#"{"type":"record_create","amount_cents":100,"record_type":"expense","category_id":999,"note":"","timestamp":null}"#.to_string();
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(
            !state["ui"]["errorMessage"].is_null(),
            "不存在的分类应返回业务错误"
        );
    });
}

#[test]
fn test_record_update_amount_validation() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let category_id = state["categories"][0]["id"].as_i64().unwrap();

        let create_intent = format!(
            r#"{{"type":"record_create","amount_cents":1000,"record_type":"expense","category_id":{category_id},"note":"","timestamp":null}}"#
        );
        core.dispatch(create_intent).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let record_id = state["records"][0]["id"].as_i64().unwrap();

        // 测试更新为零金额
        let update_zero = format!(
            r#"{{"type":"record_update","id":{record_id},"amount_cents":0,"record_type":null,"category_id":null,"note":null}}"#
        );
        let update = core.dispatch(update_zero).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(
            !state["ui"]["errorMessage"].is_null(),
            "更新为零金额应失败"
        );

        // 测试更新为负金额
        let update_neg = format!(
            r#"{{"type":"record_update","id":{record_id},"amount_cents":-100,"record_type":null,"category_id":null,"note":null}}"#
        );
        let update = core.dispatch(update_neg).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(
            !state["ui"]["errorMessage"].is_null(),
            "更新为负金额应失败"
        );

        // 测试正常更新
        let update_ok = format!(
            r#"{{"type":"record_update","id":{record_id},"amount_cents":2000,"record_type":null,"category_id":null,"note":"更新备注"}}"#
        );
        let update = core.dispatch(update_ok).await.expect("正常更新应成功");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        // 注意：成功的 dispatch 不会自动清除之前设置的 errorMessage，故不检查 is_null
        assert_eq!(state["records"][0]["amountCents"], 2000);
        assert_eq!(state["records"][0]["note"], "更新备注");
    });
}

#[test]
fn test_record_update_not_found() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent = r#"{"type":"record_update","id":99999,"amount_cents":100,"record_type":null,"category_id":null,"note":null}"#.to_string();
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(
            !state["ui"]["errorMessage"].is_null(),
            "更新不存在的记录应失败"
        );
    });
}

#[test]
fn test_record_delete() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let category_id = state["categories"][0]["id"].as_i64().unwrap();

        let create_intent = format!(
            r#"{{"type":"record_create","amount_cents":100,"record_type":"expense","category_id":{category_id},"note":"","timestamp":null}}"#
        );
        core.dispatch(create_intent).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let record_id = state["records"][0]["id"].as_i64().unwrap();
        assert_eq!(state["records"].as_array().unwrap().len(), 1);

        let delete_intent = format!(r#"{{"type":"record_delete","id":{record_id}}}"#);
        core.dispatch(delete_intent).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        assert!(state["ui"]["errorMessage"].is_null());
        assert!(state["records"].as_array().unwrap().is_empty());
    });
}

#[test]
fn test_record_delete_not_found() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent = r#"{"type":"record_delete","id":99999}"#.to_string();
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(
            !state["ui"]["errorMessage"].is_null(),
            "删除不存在的记录应失败"
        );
    });
}

#[test]
fn test_pagination_bounds() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        // 页大小为 0 应返回业务错误
        let intent = r#"{"type":"record_list","page":0,"page_size":0}"#.to_string();
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(!state["ui"]["errorMessage"].is_null(), "页大小为0应失败");

        // 页大小超过上限应返回业务错误
        let intent = r#"{"type":"record_list","page":0,"page_size":1001}"#.to_string();
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(
            !state["ui"]["errorMessage"].is_null(),
            "页大小超过上限应失败"
        );
    });
}

#[test]
fn test_record_list_pagination() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let category_id = state["categories"][0]["id"].as_i64().unwrap();

        // 创建 5 条记录
        for i in 1..=5 {
            let intent = format!(
                r#"{{"type":"record_create","amount_cents":{}00,"record_type":"expense","category_id":{},"note":"记录{}","timestamp":{}}}"#,
                i, category_id, i, i * 1000
            );
            core.dispatch(intent).await.unwrap();
        }

        // 查询第 1 页，每页 2 条
        let intent = r#"{"type":"record_list","page":0,"page_size":2}"#.to_string();
        let update = core.dispatch(intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let records = state["records"].as_array().unwrap();
        assert_eq!(records.len(), 2);
        // 按 created_at 降序，最新的是第 5 条
        assert_eq!(records[0]["note"], "记录5");
        assert_eq!(records[1]["note"], "记录4");

        // 查询第 2 页
        let intent = r#"{"type":"record_list","page":1,"page_size":2}"#.to_string();
        let update = core.dispatch(intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let records = state["records"].as_array().unwrap();
        assert_eq!(records.len(), 2);
        assert_eq!(records[0]["note"], "记录3");
        assert_eq!(records[1]["note"], "记录2");
    });
}

#[test]
fn test_record_get() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let category_id = state["categories"][0]["id"].as_i64().unwrap();

        let create_intent = format!(
            r#"{{"type":"record_create","amount_cents":500,"record_type":"expense","category_id":{category_id},"note":"测试获取","timestamp":null}}"#
        );
        core.dispatch(create_intent).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let record_id = state["records"][0]["id"].as_i64().unwrap();

        let get_intent = format!(r#"{{"type":"record_get","id":{record_id}}}"#);
        let update = core.dispatch(get_intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null());
        assert_eq!(state["ui"]["selectedRecordId"], record_id);
    });
}

#[test]
fn test_record_get_not_found() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent = r#"{"type":"record_get","id":99999}"#.to_string();
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(
            !state["ui"]["errorMessage"].is_null(),
            "获取不存在的记录应失败"
        );
    });
}

#[test]
fn test_record_list_by_month() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let category_id = state["categories"][0]["id"].as_i64().unwrap();

        // 创建 3 条记录：2 条在 2026-01，1 条在 2026-02
        // 2026-01-15 00:00:00 UTC = 1768435200
        // 2026-02-15 00:00:00 UTC = 1771113600
        let jan_15_2026 = 1768435200i64;
        let feb_15_2026 = 1771113600i64;

        for i in 1..=2 {
            let intent = format!(
                r#"{{"type":"record_create","amount_cents":{}00,"record_type":"expense","category_id":{},"note":"一月记录{}","timestamp":{}}}"#,
                i, category_id, i, jan_15_2026 + i * 3600
            );
            core.dispatch(intent).await.unwrap();
        }

        let intent = format!(
            r#"{{"type":"record_create","amount_cents":300,"record_type":"expense","category_id":{},"note":"二月记录","timestamp":{}}}"#,
            category_id, feb_15_2026
        );
        core.dispatch(intent).await.unwrap();

        // 查询 2026-01
        let intent = r#"{"type":"record_list_by_month","year_month":"2026-01"}"#.to_string();
        let update = core.dispatch(intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let records = state["records"].as_array().unwrap();
        assert_eq!(records.len(), 2, "2026-01 应返回 2 条记录");
        assert_eq!(records[0]["note"], "一月记录2");
        assert_eq!(records[1]["note"], "一月记录1");

        // 查询 2026-02
        let intent = r#"{"type":"record_list_by_month","year_month":"2026-02"}"#.to_string();
        let update = core.dispatch(intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let records = state["records"].as_array().unwrap();
        assert_eq!(records.len(), 1, "2026-02 应返回 1 条记录");
        assert_eq!(records[0]["note"], "二月记录");

        // 查询无数据的月份
        let intent = r#"{"type":"record_list_by_month","year_month":"2025-12"}"#.to_string();
        let update = core.dispatch(intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let records = state["records"].as_array().unwrap();
        assert!(records.is_empty(), "2025-12 应返回 0 条记录");
    });
}
