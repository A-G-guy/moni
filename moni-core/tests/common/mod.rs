use moni_core::MoniCore;

/// 创建并初始化一个使用内存数据库的 `MoniCore` 实例，用于测试。
#[allow(dead_code)]
pub fn setup_core() -> MoniCore {
    let core = MoniCore::new();
    let rt = tokio::runtime::Runtime::new().expect("测试 Runtime 创建失败");
    rt.block_on(async {
        core.initialize().await.expect("初始化失败");
    });
    core
}

/// 创建并初始化一个使用内存数据库的 `MoniCore` 实例，并预置分类数据。
#[allow(dead_code)]
pub fn setup_core_with_presets() -> MoniCore {
    let core = MoniCore::new();
    let rt = tokio::runtime::Runtime::new().expect("测试 Runtime 创建失败");
    rt.block_on(async {
        core.initialize_with_db(":memory:".to_string())
            .await
            .expect("初始化失败");
        core.dispatch(r#"{"type":"dev_seed_presets"}"#.to_string())
            .await
            .expect("填充预设分类失败");
    });
    core
}
