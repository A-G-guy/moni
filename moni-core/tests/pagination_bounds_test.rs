use moni_core::MoniCore;

/// H9: 验证分页边界安全——超大页码不会导致崩溃或全表扫描。
#[test]
fn test_pagination_extreme_page() {
    let core = MoniCore::new();
    let rt = tokio::runtime::Runtime::new().expect("测试 Runtime 创建失败");
    rt.block_on(async {
        core.initialize_with_db(":memory:".to_string())
            .await
            .expect("初始化失败");
        core.dispatch(r#"{"type":"dev_seed_presets"}"#.to_string())
            .await
            .expect("填充预设分类失败");

        // 超大页码应安全返回错误（CoreUpdate 中 errorMessage 非空）
        let result = core
            .dispatch(r#"{"type":"record_list","page":100001,"page_size":20}"#.to_string())
            .await
            .expect("dispatch 不应失败");
        let state: serde_json::Value =
            serde_json::from_str(&result.state_json).expect("状态解析失败");
        assert!(
            state["ui"]["errorMessage"].is_string(),
            "超大页码应触发错误"
        );

        // 正常页码应正常工作
        let result = core
            .dispatch(r#"{"type":"record_list","page":0,"page_size":20}"#.to_string())
            .await;
        assert!(result.is_ok(), "正常页码应正常工作");
    });
}

/// C13: 验证预算计算在极端金额下不 panic。
#[test]
fn test_budget_calculation_no_panic_with_extreme_values() {
    let core = MoniCore::new();
    let rt = tokio::runtime::Runtime::new().expect("测试 Runtime 创建失败");
    rt.block_on(async {
        core.initialize_with_db(":memory:".to_string())
            .await
            .expect("初始化失败");
        core.dispatch(r#"{"type":"dev_seed_presets"}"#.to_string())
            .await
            .expect("填充预设分类失败");

        // 设置总预算并记录一笔远超预算的支出
        let current_month = chrono::Local::now().format("%Y-%m").to_string();
        core.dispatch(
            format!(
                r#"{{"type":"budget_upsert","category_id":null,"amount_cents":100,"year_month":"{}","scope":"this_month"}}"#,
                current_month
            ),
        )
        .await
        .expect("设置总预算失败");

        // 创建一笔远超预算的记录
        core.dispatch(
            r#"{"type":"record_create","amount_cents":1000000,"record_type":"expense","category_id":1,"note":"超支测试"}"#
                .to_string(),
        )
        .await
        .expect("创建记录失败");

        // 查询预算列表——不应 panic
        let result = core
            .dispatch(
                format!(
                    r#"{{"type":"budget_list","year_month":"{}"}}"#,
                    current_month
                ),
            )
            .await;
        assert!(
            result.is_ok(),
            "超支场景下预算查询不应 panic"
        );
    });
}
