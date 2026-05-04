mod common;

#[test]
fn test_settings_update_currency() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        assert_eq!(state["settings"]["currencySymbol"], "¥");

        let intent = r#"{"type":"settings_update_currency","symbol":"$"}"#.to_string();
        let update = core.dispatch(intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null());
        assert_eq!(state["settings"]["currencySymbol"], "$");
    });
}

#[test]
fn test_settings_export_csv() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let category_id = state["categories"][0]["id"].as_i64().unwrap();

        // 创建记录
        let intent = format!(
            r#"{{"type":"record_create","amount_cents":1234,"record_type":"expense","category_id":{category_id},"note":"午餐","timestamp":null}}"#
        );
        core.dispatch(intent).await.unwrap();

        // 导出 CSV
        let intent = r#"{"type":"settings_export_data","format":"csv"}"#.to_string();
        let update = core.dispatch(intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null());

        // 验证副作用
        let effects = update.effects;
        assert_eq!(effects.len(), 1);
        assert_eq!(effects[0].kind, "export_file");

        let payload: serde_json::Value = serde_json::from_str(&effects[0].payload_json).unwrap();
        assert_eq!(payload["format"], "csv");
        let content = payload["content"].as_str().unwrap();
        assert!(content.contains("\u{FEFF}")); // UTF-8 BOM
        assert!(content.contains("午餐"));
    });
}

#[test]
fn test_settings_export_json() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let category_id = state["categories"][0]["id"].as_i64().unwrap();

        // 创建记录
        let intent = format!(
            r#"{{"type":"record_create","amount_cents":5678,"record_type":"income","category_id":{category_id},"note":"工资","timestamp":null}}"#
        );
        core.dispatch(intent).await.unwrap();

        // 导出 JSON
        let intent = r#"{"type":"settings_export_data","format":"json"}"#.to_string();
        let update = core.dispatch(intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null());

        let effects = update.effects;
        assert_eq!(effects.len(), 1);
        assert_eq!(effects[0].kind, "export_file");

        let payload: serde_json::Value = serde_json::from_str(&effects[0].payload_json).unwrap();
        assert_eq!(payload["format"], "json");
        let content = payload["content"].as_str().unwrap();
        assert!(content.contains("工资"));
        assert!(content.contains("\"version\": \"1.0\""));
    });
}

#[test]
fn test_settings_export_empty_data() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        // 不创建记录，直接导出
        let intent = r#"{"type":"settings_export_data","format":"csv"}"#.to_string();
        let update = core.dispatch(intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null());

        let effects = update.effects;
        assert_eq!(effects.len(), 1);

        let payload: serde_json::Value = serde_json::from_str(&effects[0].payload_json).unwrap();
        let content = payload["content"].as_str().unwrap();
        // 空数据也应包含表头和分类信息
        assert!(content.contains("\u{FEFF}"));
    });
}
