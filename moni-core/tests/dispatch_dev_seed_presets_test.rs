mod common;

/// `DevSeedPresets` 意图应填充预设分类并刷新到状态。
#[test]
fn test_dev_seed_presets_populates_state_categories() {
    // 使用未经 setup_core_with_presets 的"裸"实例，避免初始预设干扰
    let core = common::setup_core();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        // 初始应没有任何分类
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        assert!(
            state["categories"].as_array().unwrap().is_empty(),
            "裸实例不应有分类"
        );

        // 派发 DevSeedPresets
        let intent = r#"{"type":"dev_seed_presets"}"#.to_string();
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");

        // 应产生 show_snackbar 副作用
        assert!(
            update.effects.iter().any(|e| e.kind == "show_snackbar"),
            "应产生 show_snackbar 副作用"
        );

        // 状态中应已写入分类
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let categories = state["categories"].as_array().unwrap();
        assert!(!categories.is_empty(), "DevSeedPresets 后应填充分类");

        // 应同时存在收入与支出分类（覆盖典型预设）
        let has_expense = categories
            .iter()
            .any(|c| c["categoryType"] == "expense" || c["category_type"] == "expense");
        let has_income = categories
            .iter()
            .any(|c| c["categoryType"] == "income" || c["category_type"] == "income");
        assert!(has_expense, "预设应包含支出分类");
        assert!(has_income, "预设应包含收入分类");

        // 错误信息应为空
        assert!(
            state["ui"]["errorMessage"].is_null(),
            "成功后不应有 errorMessage"
        );
    });
}

/// 重复执行 `DevSeedPresets` 应当幂等（不会重复插入分类）。
#[test]
fn test_dev_seed_presets_is_idempotent() {
    let core = common::setup_core();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent = r#"{"type":"dev_seed_presets"}"#.to_string();

        let update1 = core.dispatch(intent.clone()).await.unwrap();
        let state1: serde_json::Value = serde_json::from_str(&update1.state_json).unwrap();
        let count_first = state1["categories"].as_array().unwrap().len();

        let update2 = core.dispatch(intent).await.unwrap();
        let state2: serde_json::Value = serde_json::from_str(&update2.state_json).unwrap();
        let count_second = state2["categories"].as_array().unwrap().len();

        assert_eq!(
            count_first, count_second,
            "重复 seed 应幂等：第一次 {count_first} 条 / 第二次 {count_second} 条"
        );
    });
}
