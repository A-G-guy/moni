mod common;

#[test]
fn test_category_list_with_presets() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent = r#"{"type":"category_list"}"#.to_string();
        let update = core.dispatch(intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let categories = state["categories"].as_array().unwrap();
        assert_eq!(
            categories.len(),
            moni_core::shared::constants::PRESET_CATEGORY_COUNT
        );
        assert!(categories.iter().all(|c| c["archivedAt"].is_null()));
    });
}

#[test]
fn test_category_create_success() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent = r#"{"type":"category_create","name":"自定义分类","description":"描述内容","category_type":"expense","icon_name":"star"}"#.to_string();
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null(), "不应有错误");

        let categories = state["categories"].as_array().unwrap();
        assert_eq!(categories.len(), moni_core::shared::constants::PRESET_CATEGORY_COUNT + 1);

        let custom = categories.iter().find(|c| c["name"] == "自定义分类").unwrap();
        assert_eq!(custom["iconName"], "star");
        assert_eq!(custom["description"], "描述内容");
        assert!(custom["archivedAt"].is_null());
    });
}

#[test]
fn test_category_create_without_description() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent = r#"{"type":"category_create","name":"无描述分类","category_type":"income","icon_name":"payments"}"#.to_string();
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null());

        let custom = state["categories"].as_array().unwrap()
            .iter().find(|c| c["name"] == "无描述分类").unwrap();
        assert!(custom["description"].is_null());
    });
}

#[test]
fn test_category_create_empty_name() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent =
            r#"{"type":"category_create","name":"","category_type":"expense","icon_name":"star"}"#
                .to_string();
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(
            !state["ui"]["errorMessage"].is_null(),
            "空名称应返回业务错误"
        );
    });
}

#[test]
fn test_category_create_empty_icon() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent =
            r#"{"type":"category_create","name":"测试","category_type":"expense","icon_name":""}"#
                .to_string();
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(
            !state["ui"]["errorMessage"].is_null(),
            "空图标应返回业务错误"
        );
    });
}

#[test]
fn test_category_create_description_too_long() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let long_desc = "x".repeat(201);
        let intent = format!(
            r#"{{"type":"category_create","name":"测试","description":"{long_desc}","category_type":"expense","icon_name":"star"}}"#
        );
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(
            !state["ui"]["errorMessage"].is_null(),
            "超长描述应返回业务错误"
        );
    });
}

#[test]
fn test_category_update_success() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        // 先创建自定义分类
        let intent = r#"{"type":"category_create","name":"待更新","category_type":"expense","icon_name":"star"}"#.to_string();
        core.dispatch(intent).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let custom_id = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "待更新").unwrap()["id"]
            .as_i64().unwrap();

        // 更新分类
        let update_intent = format!(
            r#"{{"type":"category_update","id":{custom_id},"name":"已更新","description":"新描述","icon_name":"new_icon","clear_parent_id":false}}"#
        );
        let update = core.dispatch(update_intent).await.expect("更新应成功");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null());

        let cat = state["categories"].as_array().unwrap()
            .iter().find(|c| c["id"] == custom_id).unwrap();
        assert_eq!(cat["name"], "已更新");
        assert_eq!(cat["description"], "新描述");
        assert_eq!(cat["iconName"], "new_icon");
    });
}

#[test]
fn test_category_update_preset_ok() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let preset_id = state["categories"][0]["id"].as_i64().unwrap();

        let update_intent =
            format!(r#"{{"type":"category_update","id":{preset_id},"name":"改名","clear_parent_id":false}}"#);
        let update = core
            .dispatch(update_intent)
            .await
            .expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null(), "编辑预设分类应成功");

        let cat = state["categories"].as_array().unwrap()
            .iter().find(|c| c["id"] == preset_id).unwrap();
        assert_eq!(cat["name"], "改名");
    });
}

#[test]
fn test_category_archive_success() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        // 创建自定义分类
        let intent = r#"{"type":"category_create","name":"待归档","category_type":"expense","icon_name":"star"}"#.to_string();
        core.dispatch(intent).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let custom_id = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "待归档").unwrap()["id"]
            .as_i64().unwrap();

        // 归档
        let archive_intent = format!(r#"{{"type":"category_archive","id":{custom_id}}}"#);
        let update = core.dispatch(archive_intent).await.expect("归档应成功");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null());

        let cat = state["categories"].as_array().unwrap()
            .iter().find(|c| c["id"] == custom_id).unwrap();
        assert!(!cat["archivedAt"].is_null());
    });
}

#[test]
fn test_category_archive_already_archived_fails() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent = r#"{"type":"category_create","name":"重复归档","category_type":"expense","icon_name":"star"}"#.to_string();
        core.dispatch(intent).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let custom_id = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "重复归档").unwrap()["id"]
            .as_i64().unwrap();

        core.dispatch(format!(r#"{{"type":"category_archive","id":{custom_id}}}"#)).await.unwrap();
        let update = core.dispatch(format!(r#"{{"type":"category_archive","id":{custom_id}}}"#)).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(
            !state["ui"]["errorMessage"].is_null(),
            "重复归档应失败"
        );
    });
}

#[test]
fn test_category_unarchive_success() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent = r#"{"type":"category_create","name":"待恢复","category_type":"expense","icon_name":"star"}"#.to_string();
        core.dispatch(intent).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let custom_id = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "待恢复").unwrap()["id"]
            .as_i64().unwrap();

        core.dispatch(format!(r#"{{"type":"category_archive","id":{custom_id}}}"#)).await.unwrap();
        let update = core.dispatch(format!(r#"{{"type":"category_unarchive","id":{custom_id}}}"#)).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null());

        let cat = state["categories"].as_array().unwrap()
            .iter().find(|c| c["id"] == custom_id).unwrap();
        assert!(cat["archivedAt"].is_null());
    });
}

#[test]
fn test_category_unarchive_not_archived_fails() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent = r#"{"type":"category_create","name":"未归档","category_type":"expense","icon_name":"star"}"#.to_string();
        core.dispatch(intent).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let custom_id = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "未归档").unwrap()["id"]
            .as_i64().unwrap();

        let update = core.dispatch(format!(r#"{{"type":"category_unarchive","id":{custom_id}}}"#)).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(
            !state["ui"]["errorMessage"].is_null(),
            "取消未归档分类应失败"
        );
    });
}

#[test]
fn test_category_archive_in_use_ok() {
    // 归档应允许，即使分类被记录引用（历史记录需保留）
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent = r#"{"type":"category_create","name":"使用中","category_type":"expense","icon_name":"star"}"#.to_string();
        core.dispatch(intent).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let custom_id = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "使用中").unwrap()["id"]
            .as_i64().unwrap();

        // 使用该分类创建记录
        let create_record = format!(
            r#"{{"type":"record_create","amount_cents":100,"record_type":"expense","category_id":{custom_id},"note":"","timestamp":null}}"#
        );
        core.dispatch(create_record).await.unwrap();

        // 归档应成功（不再因使用中而拒绝）
        let archive_intent = format!(r#"{{"type":"category_archive","id":{custom_id}}}"#);
        let update = core.dispatch(archive_intent).await.expect("归档应成功");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null(), "使用中分类也应可归档");
    });
}

#[test]
fn test_category_create_with_parent_success() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let parent_id = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "餐饮").unwrap()["id"]
            .as_i64().unwrap();

        let intent = format!(
            r#"{{"type":"category_create","name":"宵夜","category_type":"expense","icon_name":"nightlife","parent_id":{parent_id}}}"#
        );
        let update = core.dispatch(intent).await.expect("创建二级分类应成功");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null(), "不应有错误");

        let cat = state["categories"].as_array().unwrap()
            .iter().find(|c| c["name"] == "宵夜").unwrap();
        assert_eq!(cat["parentId"], parent_id);
    });
}

#[test]
fn test_category_create_parent_type_mismatch_fails() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let expense_parent = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "餐饮").unwrap()["id"]
            .as_i64().unwrap();

        // 尝试用支出分类作为收入子分类的父分类
        let intent = format!(
            r#"{{"type":"category_create","name":"副业","category_type":"income","icon_name":"work","parent_id":{expense_parent}}}"#
        );
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(
            !state["ui"]["errorMessage"].is_null(),
            "类型不一致应返回业务错误"
        );
    });
}

#[test]
fn test_category_create_parent_not_found_fails() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent = r#"{"type":"category_create","name":"测试","category_type":"expense","icon_name":"star","parent_id":99999}"#.to_string();
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(
            !state["ui"]["errorMessage"].is_null(),
            "父分类不存在应返回业务错误"
        );
    });
}

#[test]
fn test_category_update_set_parent_success() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        // 先创建一级分类
        let intent = r#"{"type":"category_create","name":"待设父","category_type":"expense","icon_name":"star"}"#.to_string();
        core.dispatch(intent).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let custom_id = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "待设父").unwrap()["id"]
            .as_i64().unwrap();
        let parent_id = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "餐饮").unwrap()["id"]
            .as_i64().unwrap();

        // 设为二级分类
        let update_intent = format!(
            r#"{{"type":"category_update","id":{custom_id},"parent_id":{parent_id},"clear_parent_id":false}}"#
        );
        let update = core.dispatch(update_intent).await.expect("更新应成功");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null());

        let cat = state["categories"].as_array().unwrap()
            .iter().find(|c| c["id"] == custom_id).unwrap();
        assert_eq!(cat["parentId"], parent_id);
    });
}

#[test]
fn test_category_update_clear_parent_success() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let parent_id = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "餐饮").unwrap()["id"]
            .as_i64().unwrap();

        // 创建二级分类
        let intent = format!(
            r#"{{"type":"category_create","name":"原为二级","category_type":"expense","icon_name":"star","parent_id":{parent_id}}}"#
        );
        core.dispatch(intent).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let custom_id = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "原为二级").unwrap()["id"]
            .as_i64().unwrap();

        // 清除父分类（变回一级）
        let update_intent = format!(
            r#"{{"type":"category_update","id":{custom_id},"clear_parent_id":true}}"#
        );
        let update = core.dispatch(update_intent).await.expect("更新应成功");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null());

        let cat = state["categories"].as_array().unwrap()
            .iter().find(|c| c["id"] == custom_id).unwrap();
        assert!(cat["parentId"].is_null());
    });
}

#[test]
fn test_category_update_self_reference_fails() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent = r#"{"type":"category_create","name":"自引用","category_type":"expense","icon_name":"star"}"#.to_string();
        core.dispatch(intent).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let custom_id = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "自引用").unwrap()["id"]
            .as_i64().unwrap();

        let update_intent = format!(
            r#"{{"type":"category_update","id":{custom_id},"parent_id":{custom_id},"clear_parent_id":false}}"#
        );
        let update = core.dispatch(update_intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(
            !state["ui"]["errorMessage"].is_null(),
            "自引用应返回业务错误"
        );
    });
}

#[test]
fn test_category_update_set_parent_when_has_children_fails() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let parent_id = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "餐饮").unwrap()["id"]
            .as_i64().unwrap();

        // 创建一级分类
        let intent = r#"{"type":"category_create","name":"有子","category_type":"expense","icon_name":"star"}"#.to_string();
        core.dispatch(intent).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let has_child_id = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "有子").unwrap()["id"]
            .as_i64().unwrap();

        // 给它一个子分类
        let child_intent = format!(
            r#"{{"type":"category_create","name":"子","category_type":"expense","icon_name":"child","parent_id":{has_child_id}}}"#
        );
        core.dispatch(child_intent).await.unwrap();

        // 尝试将有子分类的分类设为二级分类
        let update_intent = format!(
            r#"{{"type":"category_update","id":{has_child_id},"parent_id":{parent_id},"clear_parent_id":false}}"#
        );
        let update = core.dispatch(update_intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(
            !state["ui"]["errorMessage"].is_null(),
            "有子分类的分类不能设为二级分类"
        );
    });
}

#[test]
fn test_category_archive_with_children_fails() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        // 创建一级分类
        let intent = r#"{"type":"category_create","name":"父","category_type":"expense","icon_name":"star"}"#.to_string();
        core.dispatch(intent).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let parent_id = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "父").unwrap()["id"]
            .as_i64().unwrap();

        // 给它一个子分类
        let child_intent = format!(
            r#"{{"type":"category_create","name":"子","category_type":"expense","icon_name":"child","parent_id":{parent_id}}}"#
        );
        core.dispatch(child_intent).await.unwrap();

        // 尝试归档父分类
        let archive_intent = format!(r#"{{"type":"category_archive","id":{parent_id}}}"#);
        let update = core.dispatch(archive_intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(
            !state["ui"]["errorMessage"].is_null(),
            "有子分类的分类不能归档"
        );
    });
}

#[test]
fn test_category_archive_without_children_ok() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        // 创建一级分类（无子分类）
        let intent = r#"{"type":"category_create","name":"无子","category_type":"expense","icon_name":"star"}"#.to_string();
        core.dispatch(intent).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let id = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "无子").unwrap()["id"]
            .as_i64().unwrap();

        // 归档应成功
        let archive_intent = format!(r#"{{"type":"category_archive","id":{id}}}"#);
        let update = core.dispatch(archive_intent).await.expect("归档应成功");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null(), "无子分类应可归档");
    });
}
