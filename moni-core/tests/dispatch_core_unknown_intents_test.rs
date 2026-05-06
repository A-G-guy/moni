mod common;

/// 覆盖 `dispatch_core` 主分发器中的未知/非法意图解析路径与错误状态链路。
///
/// 测试范围：
/// - 完全无效 JSON（空串、片段、非 JSON）导致解析层直接返回 Err。
/// - 缺少 `type` 字段、type 类型错误、未知 type 导致解析层直接返回 Err。
/// - 已知 type 但缺少必需字段、字段类型错误、枚举值非法导致解析层直接返回 Err。
/// - 连续 dispatch 间状态正确累加。
/// - errorMessage 在业务错误后被写入，成功 dispatch 后保留，仅 DismissError 可清除。
/// - NavigateTo 在已有 errorMessage 时正确切换 tab 但不清理错误信息。

use common::setup_core_with_presets;

// ---------------------------------------------------------------------------
// 1. 完全空字符串
// ---------------------------------------------------------------------------
#[test]
fn test_dispatch_empty_string_fails() {
    let core = setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let result = core.dispatch("".to_string()).await;
        let err = result.expect_err("空字符串应触发解析错误");
        let msg = err.to_string();
        assert!(
            msg.contains("意图解析失败"),
            "错误文案应含'意图解析失败'，实际: {msg}"
        );
    });
}

// ---------------------------------------------------------------------------
// 2. JSON 片段（不完整）
// ---------------------------------------------------------------------------
#[test]
fn test_dispatch_incomplete_json_fails() {
    let core = setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let result = core.dispatch("{".to_string()).await;
        let err = result.expect_err("不完整 JSON 应触发解析错误");
        let msg = err.to_string();
        assert!(
            msg.contains("意图解析失败"),
            "错误文案应含'意图解析失败'，实际: {msg}"
        );
    });
}

// ---------------------------------------------------------------------------
// 3. 缺少 type 字段
// ---------------------------------------------------------------------------
#[test]
fn test_dispatch_missing_type_field_fails() {
    let core = setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let result = core.dispatch(r#"{"amount_cents":100}"#.to_string()).await;
        let err = result.expect_err("缺少 type 字段应触发解析错误");
        let msg = err.to_string();
        assert!(
            msg.contains("意图解析失败"),
            "错误文案应含'意图解析失败'，实际: {msg}"
        );
    });
}

// ---------------------------------------------------------------------------
// 4. type 字段类型错误（非字符串）
// ---------------------------------------------------------------------------
#[test]
fn test_dispatch_type_field_not_string_fails() {
    let core = setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let result = core.dispatch(r#"{"type":123}"#.to_string()).await;
        let err = result.expect_err("type 为数字应触发解析错误");
        let msg = err.to_string();
        assert!(
            msg.contains("意图解析失败"),
            "错误文案应含'意图解析失败'，实际: {msg}"
        );
    });
}

// ---------------------------------------------------------------------------
// 5. 未知 type 字符串
// ---------------------------------------------------------------------------
#[test]
fn test_dispatch_unknown_type_fails() {
    let core = setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let result = core
            .dispatch(r#"{"type":"unknown_intent_xyz"}"#.to_string())
            .await;
        let err = result.expect_err("未知 type 应触发解析错误");
        let msg = err.to_string();
        assert!(
            msg.contains("意图解析失败"),
            "错误文案应含'意图解析失败'，实际: {msg}"
        );
    });
}

// ---------------------------------------------------------------------------
// 6. 已知 type 但缺少必需字段（record_create 缺 amount_cents）
// ---------------------------------------------------------------------------
#[test]
fn test_dispatch_known_type_missing_required_field_fails() {
    let core = setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        // 缺少 amount_cents
        let result = core
            .dispatch(
                r#"{"type":"record_create","record_type":"expense","category_id":1,"note":"","timestamp":null}"#
                    .to_string(),
            )
            .await;
        let err = result.expect_err("缺少必需字段应触发解析错误");
        let msg = err.to_string();
        assert!(
            msg.contains("意图解析失败"),
            "错误文案应含'意图解析失败'，实际: {msg}"
        );
    });
}

// ---------------------------------------------------------------------------
// 7. 已知 type 但字段类型错误（amount_cents 为字符串）
// ---------------------------------------------------------------------------
#[test]
fn test_dispatch_known_type_wrong_field_type_fails() {
    let core = setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let result = core
            .dispatch(
                r#"{"type":"record_create","amount_cents":"not_a_number","record_type":"expense","category_id":1,"note":"","timestamp":null}"#
                    .to_string(),
            )
            .await;
        let err = result.expect_err("字段类型错误应触发解析错误");
        let msg = err.to_string();
        assert!(
            msg.contains("意图解析失败"),
            "错误文案应含'意图解析失败'，实际: {msg}"
        );
    });
}

// ---------------------------------------------------------------------------
// 8. 已知 type 但枚举值非法（record_type 不是 expense/income）
// ---------------------------------------------------------------------------
#[test]
fn test_dispatch_known_type_invalid_enum_value_fails() {
    let core = setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let result = core
            .dispatch(
                r#"{"type":"record_create","amount_cents":100,"record_type":"invalid_type","category_id":1,"note":"","timestamp":null}"#
                    .to_string(),
            )
            .await;
        let err = result.expect_err("非法枚举值应触发解析错误");
        let msg = err.to_string();
        assert!(
            msg.contains("意图解析失败"),
            "错误文案应含'意图解析失败'，实际: {msg}"
        );
    });
}

// ---------------------------------------------------------------------------
// 9. 连续 dispatch 间状态正确累加，且 errorMessage 保留到 DismissError
// ---------------------------------------------------------------------------
#[test]
fn test_consecutive_dispatch_state_accumulates_and_error_message_persists() {
    let core = setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let category_id = state["categories"][0]["id"].as_i64().unwrap();

        // 第一次：正常创建记录
        let intent = format!(
            r#"{{"type":"record_create","amount_cents":1000,"record_type":"expense","category_id":{category_id},"note":"第一条","timestamp":null}}"#
        );
        let update = core.dispatch(intent).await.expect("第一次创建应成功");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null(), "首次成功不应有错误");
        assert_eq!(state["records"].as_array().unwrap().len(), 1);

        // 第二次：业务错误（不存在的分类）
        let bad_intent = r#"{"type":"record_create","amount_cents":200,"record_type":"expense","category_id":999999,"note":"错误","timestamp":null}"#.to_string();
        let update = core.dispatch(bad_intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let err1 = state["ui"]["errorMessage"].as_str().unwrap();
        assert!(
            !err1.is_empty(),
            "业务错误后 errorMessage 应被写入"
        );
        // 记录数量应保持 1（创建失败不应新增）
        assert_eq!(state["records"].as_array().unwrap().len(), 1);

        // 第三次：再次正常创建，验证 errorMessage 在成功 dispatch 后保留
        let intent = format!(
            r#"{{"type":"record_create","amount_cents":3000,"record_type":"income","category_id":{category_id},"note":"第二条","timestamp":null}}"#
        );
        let update = core.dispatch(intent).await.expect("第二次创建应成功");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let err2 = state["ui"]["errorMessage"].as_str().unwrap();
        assert_eq!(
            err1, err2,
            "成功 dispatch 不应自动清除之前的 errorMessage"
        );
        assert_eq!(state["records"].as_array().unwrap().len(), 2);

        // 第四次：新的业务错误，验证 errorMessage 被覆盖
        let bad_intent2 = r#"{"type":"record_create","amount_cents":0,"record_type":"expense","category_id":1,"note":"","timestamp":null}"#.to_string();
        let update = core.dispatch(bad_intent2).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let err3 = state["ui"]["errorMessage"].as_str().unwrap();
        assert_ne!(
            err1, err3,
            "新的业务错误应覆盖之前的 errorMessage"
        );
        assert!(
            err3.contains("金额") || err3.contains("大于0"),
            "新错误文案应含金额校验信息，实际: {err3}"
        );

        // 第五次：DismissError 清除 errorMessage
        let dismiss = r#"{"type":"dismiss_error"}"#.to_string();
        let update = core.dispatch(dismiss).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(
            state["ui"]["errorMessage"].is_null(),
            "DismissError 后应清空错误信息"
        );
        // 记录数量仍为 2
        assert_eq!(state["records"].as_array().unwrap().len(), 2);
    });
}

// ---------------------------------------------------------------------------
// 10. NavigateTo 在已有 errorMessage 时切换 tab 但不清理错误信息
// ---------------------------------------------------------------------------
#[test]
fn test_navigate_to_preserves_error_message() {
    let core = setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        // 先制造一个错误
        let bad_intent = r#"{"type":"record_create","amount_cents":100,"record_type":"expense","category_id":999999,"note":"","timestamp":null}"#.to_string();
        let update = core.dispatch(bad_intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let err = state["ui"]["errorMessage"].as_str().unwrap();
        assert!(!err.is_empty(), "应已设置错误信息");
        assert_eq!(state["ui"]["activeTab"], "records");

        // NavigateTo 切换 tab
        let intent = r#"{"type":"navigate_to","screen":"settings"}"#.to_string();
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert_eq!(state["ui"]["activeTab"], "settings", "tab 应切换为 settings");
        let err_after = state["ui"]["errorMessage"].as_str().unwrap();
        assert_eq!(
            err, err_after,
            "NavigateTo 不应清除已有的 errorMessage"
        );
    });
}
