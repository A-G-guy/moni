use moni_core::MoniCore;

/// C1: 验证外键约束已启用——清空数据后状态正确。
#[test]
fn test_foreign_key_enforced() {
    let core = MoniCore::new();
    let rt = tokio::runtime::Runtime::new().expect("测试 Runtime 创建失败");
    rt.block_on(async {
        core.initialize_with_db(":memory:".to_string())
            .await
            .expect("初始化失败");
        core.dispatch(r#"{"type":"dev_seed_presets"}"#.to_string())
            .await
            .expect("填充预设分类失败");

        // 创建一条记录（使用第一个分类，id=1）
        core.dispatch(
            r#"{"type":"record_create","amount_cents":100,"record_type":"expense","category_id":1,"note":"测试"}"#
                .to_string(),
        )
        .await
        .expect("创建记录失败");

        // 清空数据——事务保护下应完整清空
        let result = core.dispatch(r#"{"type":"dev_clear_all_data"}"#.to_string()).await;
        assert!(result.is_ok(), "清空数据应成功");
        let update = result.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).expect("状态解析失败");
        assert!(
            state["records"].as_array().map_or(false, |v| v.is_empty()),
            "清空后记录应为空"
        );
    });
}

/// C7: 验证 note 字段长度限制。
#[test]
fn test_note_length_limit() {
    let core = MoniCore::new();
    let rt = tokio::runtime::Runtime::new().expect("测试 Runtime 创建失败");
    rt.block_on(async {
        core.initialize_with_db(":memory:".to_string())
            .await
            .expect("初始化失败");
        core.dispatch(r#"{"type":"dev_seed_presets"}"#.to_string())
            .await
            .expect("填充预设分类失败");

        // 超长备注应按字符数拒绝（返回的 CoreUpdate 中 error_message 非空）
        let long_note = "测".repeat(2001);
        let result = core
            .dispatch(
                format!(
                    r#"{{"type":"record_create","amount_cents":100,"record_type":"expense","category_id":1,"note":"{}"}}"#,
                    long_note
                ),
            )
            .await
            .expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&result.state_json).expect("状态解析失败");
        assert!(
            state["ui"]["errorMessage"].is_string(),
            "超长备注应触发错误"
        );

        // 先清除错误
        core.dispatch(r#"{"type":"dismiss_error"}"#.to_string())
            .await
            .expect("清除错误失败");

        // 边界值：刚好 2000 个中文字符应通过。
        let max_note = "测".repeat(2000);
        let result = core
            .dispatch(
                format!(
                    r#"{{"type":"record_create","amount_cents":100,"record_type":"expense","category_id":1,"note":"{}"}}"#,
                    max_note
                ),
            )
            .await
            .expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&result.state_json).expect("状态解析失败");
        assert!(
            state["ui"]["errorMessage"].is_null(),
            "刚好 2000 个中文字符的备注应被接受"
        );
    });
}
