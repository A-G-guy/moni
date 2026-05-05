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
