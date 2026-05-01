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
        assert_eq!(categories.len(), moni_core::shared::constants::PRESET_CATEGORY_COUNT);
        assert!(categories.iter().all(|c| c["isPreset"].as_bool().unwrap()));
    });
}

#[test]
fn test_category_create_success() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent = r##"{"type":"category_create","name":"自定义分类","category_type":"expense","icon_name":"star","color_hex":"#AABBCC"}"##.to_string();
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null(), "不应有错误");

        let categories = state["categories"].as_array().unwrap();
        assert_eq!(categories.len(), moni_core::shared::constants::PRESET_CATEGORY_COUNT + 1);

        let custom = categories.iter().find(|c| c["name"] == "自定义分类").unwrap();
        assert_eq!(custom["iconName"], "star");
        assert_eq!(custom["colorHex"], "#AABBCC");
        assert!(!custom["isPreset"].as_bool().unwrap());
    });
}

#[test]
fn test_category_create_empty_name() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent = r##"{"type":"category_create","name":"","category_type":"expense","icon_name":"star","color_hex":"#AABBCC"}"##.to_string();
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
        let intent = r##"{"type":"category_create","name":"测试","category_type":"expense","icon_name":"","color_hex":"#AABBCC"}"##.to_string();
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(
            !state["ui"]["errorMessage"].is_null(),
            "空图标应返回业务错误"
        );
    });
}

#[test]
fn test_category_create_invalid_color() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        // 缺少 # 前缀
        let intent = r#"{"type":"category_create","name":"测试","category_type":"expense","icon_name":"star","color_hex":"AABBCC"}"#.to_string();
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(
            !state["ui"]["errorMessage"].is_null(),
            "缺少#的颜色格式应失败"
        );

        // 长度不对
        let intent = r##"{"type":"category_create","name":"测试","category_type":"expense","icon_name":"star","color_hex":"#AABBC"}"##.to_string();
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(
            !state["ui"]["errorMessage"].is_null(),
            "长度不对的颜色格式应失败"
        );

        // 包含非十六进制字符
        let intent = r##"{"type":"category_create","name":"测试","category_type":"expense","icon_name":"star","color_hex":"#AABBCCZZ"}"##.to_string();
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(
            !state["ui"]["errorMessage"].is_null(),
            "非法十六进制字符应失败"
        );
    });
}

#[test]
fn test_category_delete_custom_success() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        // 先创建自定义分类
        let intent = r##"{"type":"category_create","name":"待删除","category_type":"expense","icon_name":"star","color_hex":"#AABBCC"}"##.to_string();
        core.dispatch(intent).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let custom_id = state["categories"]
            .as_array()
            .unwrap()
            .iter()
            .find(|c| c["name"] == "待删除")
            .unwrap()["id"]
            .as_i64()
            .unwrap();

        // 删除自定义分类
        let delete_intent = format!(r#"{{"type":"category_delete","id":{}}}"#, custom_id);
        let update = core.dispatch(delete_intent).await.expect("删除应成功");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null());

        let categories = state["categories"].as_array().unwrap();
        assert!(!categories.iter().any(|c| c["name"] == "待删除"));
        assert_eq!(categories.len(), moni_core::shared::constants::PRESET_CATEGORY_COUNT);
    });
}

#[test]
fn test_category_delete_preset_fails() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let preset_id = state["categories"][0]["id"].as_i64().unwrap();

        let delete_intent = format!(r#"{{"type":"category_delete","id":{}}}"#, preset_id);
        let update = core.dispatch(delete_intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(
            !state["ui"]["errorMessage"].is_null(),
            "删除预设分类应失败"
        );
    });
}

#[test]
fn test_category_delete_in_use_fails() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        // 创建自定义分类
        let intent = r##"{"type":"category_create","name":"使用中","category_type":"expense","icon_name":"star","color_hex":"#AABBCC"}"##.to_string();
        core.dispatch(intent).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let custom_id = state["categories"]
            .as_array()
            .unwrap()
            .iter()
            .find(|c| c["name"] == "使用中")
            .unwrap()["id"]
            .as_i64()
            .unwrap();

        // 使用该分类创建记录
        let create_record = format!(
            r#"{{"type":"record_create","amount_cents":100,"record_type":"expense","category_id":{},"note":"","timestamp":null}}"#,
            custom_id
        );
        core.dispatch(create_record).await.unwrap();

        // 尝试删除正在使用的分类
        let delete_intent = format!(r#"{{"type":"category_delete","id":{}}}"#, custom_id);
        let update = core.dispatch(delete_intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(
            !state["ui"]["errorMessage"].is_null(),
            "删除正在使用的分类应失败"
        );
    });
}

#[test]
fn test_category_delete_not_found() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent = r#"{"type":"category_delete","id":99999}"#.to_string();
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(
            !state["ui"]["errorMessage"].is_null(),
            "删除不存在的分类应失败"
        );
    });
}
