mod common;

/// 覆盖 `dispatch_category.rs` 中尚未触发的边界分支：
/// - update 时 name/icon 为空字符串
/// - update 时 id 不存在
/// - update 时 parent_id 指向已归档分类
/// - update 时 parent_id 指向二级分类（多级嵌套）
/// - update 时 parent_id 类型不匹配
/// - create 时 parent_id 指向已归档分类
/// - create 时 parent_id 指向二级分类
/// - archive / unarchive 时 id 不存在
/// - update 时描述超长
use serde_json::Value;

fn get_category_by_name(state: &Value, name: &str) -> Option<Value> {
    state["categories"]
        .as_array()
        .unwrap()
        .iter()
        .find(|c| c["name"] == name)
        .cloned()
}

#[test]
fn test_category_update_empty_name_fails() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: Value = serde_json::from_str(&snapshot).unwrap();
        let preset_id = state["categories"][0]["id"].as_i64().unwrap();

        let intent = format!(
            r#"{{"type":"category_update","id":{preset_id},"name":"","clear_parent_id":false}}"#
        );
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: Value = serde_json::from_str(&update.state_json).unwrap();
        let err = state["ui"]["errorMessage"].as_str().unwrap();
        assert!(
            err.contains("分类名称不能为空"),
            "期望错误包含'分类名称不能为空'，实际: {err}"
        );
    });
}

#[test]
fn test_category_update_empty_icon_fails() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: Value = serde_json::from_str(&snapshot).unwrap();
        let preset_id = state["categories"][0]["id"].as_i64().unwrap();

        let intent = format!(
            r#"{{"type":"category_update","id":{preset_id},"icon_name":"","clear_parent_id":false}}"#
        );
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: Value = serde_json::from_str(&update.state_json).unwrap();
        let err = state["ui"]["errorMessage"].as_str().unwrap();
        assert!(
            err.contains("图标名称不能为空"),
            "期望错误包含'图标名称不能为空'，实际: {err}"
        );
    });
}

#[test]
fn test_category_update_not_found_fails() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent =
            r#"{"type":"category_update","id":99999,"name":"不存在","clear_parent_id":false}"#
                .to_string();
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: Value = serde_json::from_str(&update.state_json).unwrap();
        let err = state["ui"]["errorMessage"].as_str().unwrap();
        assert!(
            err.contains("分类不存在"),
            "期望错误包含'分类不存在'，实际: {err}"
        );
    });
}

#[test]
fn test_category_update_description_too_long_fails() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: Value = serde_json::from_str(&snapshot).unwrap();
        let preset_id = state["categories"][0]["id"].as_i64().unwrap();

        let long_desc = "x".repeat(201);
        let intent = format!(
            r#"{{"type":"category_update","id":{preset_id},"description":"{long_desc}","clear_parent_id":false}}"#
        );
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: Value = serde_json::from_str(&update.state_json).unwrap();
        let err = state["ui"]["errorMessage"].as_str().unwrap();
        assert!(
            err.contains("描述长度不能超过"),
            "期望错误包含'描述长度不能超过'，实际: {err}"
        );
    });
}

#[test]
fn test_category_update_parent_archived_fails() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        // 创建一级分类并归档
        let create = r#"{"type":"category_create","name":"待归档父","category_type":"expense","icon_name":"star"}"#.to_string();
        core.dispatch(create).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: Value = serde_json::from_str(&snapshot).unwrap();
        let archived_id = get_category_by_name(&state, "待归档父").unwrap()["id"]
            .as_i64()
            .unwrap();

        core.dispatch(format!(r#"{{"type":"category_archive","id":{archived_id}}}"#))
            .await
            .unwrap();

        // 创建另一个一级分类，尝试将其 parent_id 设为已归档分类
        let create2 = r#"{"type":"category_create","name":"子候选","category_type":"expense","icon_name":"child"}"#.to_string();
        core.dispatch(create2).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: Value = serde_json::from_str(&snapshot).unwrap();
        let child_candidate_id = get_category_by_name(&state, "子候选").unwrap()["id"]
            .as_i64()
            .unwrap();

        let intent = format!(
            r#"{{"type":"category_update","id":{child_candidate_id},"parent_id":{archived_id},"clear_parent_id":false}}"#
        );
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: Value = serde_json::from_str(&update.state_json).unwrap();
        let err = state["ui"]["errorMessage"].as_str().unwrap();
        assert!(
            err.contains("父分类已归档"),
            "期望错误包含'父分类已归档'，实际: {err}"
        );
    });
}

#[test]
fn test_category_update_parent_is_subcategory_fails() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        // 使用预设的"餐饮"作为一级分类，创建其子分类
        let snapshot = core.snapshot_json().await.unwrap();
        let state: Value = serde_json::from_str(&snapshot).unwrap();
        let catering_id = get_category_by_name(&state, "餐饮").unwrap()["id"]
            .as_i64()
            .unwrap();

        let child_intent = format!(
            r#"{{"type":"category_create","name":"测试子","category_type":"expense","icon_name":"child","parent_id":{catering_id}}}"#
        );
        core.dispatch(child_intent).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: Value = serde_json::from_str(&snapshot).unwrap();
        let sub_id = get_category_by_name(&state, "测试子").unwrap()["id"]
            .as_i64()
            .unwrap();

        // 创建另一个一级分类，尝试将其 parent_id 设为二级分类
        let create2 =
            r#"{"type":"category_create","name":"另一级","category_type":"expense","icon_name":"star"}"#.to_string();
        core.dispatch(create2).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: Value = serde_json::from_str(&snapshot).unwrap();
        let another_id = get_category_by_name(&state, "另一级").unwrap()["id"]
            .as_i64()
            .unwrap();

        let intent = format!(
            r#"{{"type":"category_update","id":{another_id},"parent_id":{sub_id},"clear_parent_id":false}}"#
        );
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: Value = serde_json::from_str(&update.state_json).unwrap();
        let err = state["ui"]["errorMessage"].as_str().unwrap();
        assert!(
            err.contains("仅支持单一层级"),
            "期望错误包含'仅支持单一层级'，实际: {err}"
        );
    });
}

#[test]
fn test_category_update_parent_type_mismatch_fails() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        // 创建支出分类
        let create =
            r#"{"type":"category_create","name":"支出子","category_type":"expense","icon_name":"star"}"#.to_string();
        core.dispatch(create).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: Value = serde_json::from_str(&snapshot).unwrap();
        let expense_id = get_category_by_name(&state, "支出子").unwrap()["id"]
            .as_i64()
            .unwrap();

        // 创建收入分类，尝试将其 parent_id 设为支出分类
        let create2 =
            r#"{"type":"category_create","name":"收入父候选","category_type":"income","icon_name":"payments"}"#.to_string();
        core.dispatch(create2).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: Value = serde_json::from_str(&snapshot).unwrap();
        let income_id = get_category_by_name(&state, "收入父候选").unwrap()["id"]
            .as_i64()
            .unwrap();

        let intent = format!(
            r#"{{"type":"category_update","id":{income_id},"parent_id":{expense_id},"clear_parent_id":false}}"#
        );
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: Value = serde_json::from_str(&update.state_json).unwrap();
        let err = state["ui"]["errorMessage"].as_str().unwrap();
        assert!(
            err.contains("类型不一致"),
            "期望错误包含'类型不一致'，实际: {err}"
        );
    });
}

#[test]
fn test_category_create_parent_archived_fails() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        // 创建并归档一级分类
        let create =
            r#"{"type":"category_create","name":"归档父","category_type":"expense","icon_name":"star"}"#.to_string();
        core.dispatch(create).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: Value = serde_json::from_str(&snapshot).unwrap();
        let archived_id = get_category_by_name(&state, "归档父").unwrap()["id"]
            .as_i64()
            .unwrap();

        core.dispatch(format!(r#"{{"type":"category_archive","id":{archived_id}}}"#))
            .await
            .unwrap();

        // 尝试以已归档分类为父创建子分类
        let intent = format!(
            r#"{{"type":"category_create","name":"子","category_type":"expense","icon_name":"child","parent_id":{archived_id}}}"#
        );
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: Value = serde_json::from_str(&update.state_json).unwrap();
        let err = state["ui"]["errorMessage"].as_str().unwrap();
        assert!(
            err.contains("父分类已归档"),
            "期望错误包含'父分类已归档'，实际: {err}"
        );
    });
}

#[test]
fn test_category_create_parent_is_subcategory_fails() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: Value = serde_json::from_str(&snapshot).unwrap();
        let catering_id = get_category_by_name(&state, "餐饮").unwrap()["id"]
            .as_i64()
            .unwrap();

        // 先创建一个二级分类
        let child_intent = format!(
            r#"{{"type":"category_create","name":"子","category_type":"expense","icon_name":"child","parent_id":{catering_id}}}"#
        );
        core.dispatch(child_intent).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: Value = serde_json::from_str(&snapshot).unwrap();
        let sub_id = get_category_by_name(&state, "子").unwrap()["id"]
            .as_i64()
            .unwrap();

        // 尝试以二级分类为父再创建子分类（多级嵌套）
        let intent = format!(
            r#"{{"type":"category_create","name":"孙","category_type":"expense","icon_name":"baby","parent_id":{sub_id}}}"#
        );
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: Value = serde_json::from_str(&update.state_json).unwrap();
        let err = state["ui"]["errorMessage"].as_str().unwrap();
        assert!(
            err.contains("仅支持单一层级"),
            "期望错误包含'仅支持单一层级'，实际: {err}"
        );
    });
}

#[test]
fn test_category_archive_not_found_fails() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent = r#"{"type":"category_archive","id":99999}"#.to_string();
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: Value = serde_json::from_str(&update.state_json).unwrap();
        let err = state["ui"]["errorMessage"].as_str().unwrap();
        assert!(
            err.contains("分类不存在"),
            "期望错误包含'分类不存在'，实际: {err}"
        );
    });
}

#[test]
fn test_category_unarchive_not_found_fails() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent = r#"{"type":"category_unarchive","id":99999}"#.to_string();
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: Value = serde_json::from_str(&update.state_json).unwrap();
        let err = state["ui"]["errorMessage"].as_str().unwrap();
        assert!(
            err.contains("分类不存在"),
            "期望错误包含'分类不存在'，实际: {err}"
        );
    });
}
