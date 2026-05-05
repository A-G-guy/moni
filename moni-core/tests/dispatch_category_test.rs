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
        assert!(categories.iter().all(|c| c["isPreset"].as_bool().unwrap()));
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
        assert!(!custom["isPreset"].as_bool().unwrap());
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
            r#"{{"type":"category_update","id":{custom_id},"name":"已更新","description":"新描述","icon_name":"new_icon"}}"#
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
            format!(r#"{{"type":"category_update","id":{preset_id},"name":"改名"}}"#);
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
