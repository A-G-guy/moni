use moni_core::MoniCore;
use moni_core::core::AppCoreRuntime;
use moni_core::models::state::AppState;
use moni_core::models::effects::CoreEffect;

/// 验证 `MoniCore` 的 Debug 实现可被格式化（占位字符串，不暴露内部状态）。
#[test]
fn test_moni_core_debug_format_does_not_panic() {
    let core = MoniCore::new();
    let debug_str = format!("{core:?}");
    assert!(debug_str.contains("MoniCore"), "Debug 输出应包含类型名，实际: {debug_str}");
}

/// 验证 `MoniCore::default()` 与 `MoniCore::new()` 行为一致。
#[test]
fn test_moni_core_default_yields_initial_state() {
    let core_a: MoniCore = Default::default();
    let core_b = MoniCore::new();

    let rt = tokio::runtime::Runtime::new().unwrap();
    rt.block_on(async {
        let snapshot_a = core_a.snapshot_json().await.unwrap();
        let snapshot_b = core_b.snapshot_json().await.unwrap();

        let state_a: serde_json::Value = serde_json::from_str(&snapshot_a).unwrap();
        let state_b: serde_json::Value = serde_json::from_str(&snapshot_b).unwrap();

        assert_eq!(state_a["ui"]["activeTab"], state_b["ui"]["activeTab"]);
        assert_eq!(state_a["records"], state_b["records"]);
        assert_eq!(state_a["categories"], state_b["categories"]);
    });
}

/// 验证 `AppCoreRuntime` 的 Debug 实现：暴露 state 字段、隐藏 conn。
#[test]
fn test_app_core_runtime_debug_format() {
    let runtime = AppCoreRuntime {
        state: AppState::default(),
        conn: rusqlite::Connection::open_in_memory().expect("内存数据库创建失败"),
    };

    let debug_str = format!("{runtime:?}");
    assert!(debug_str.contains("AppCoreRuntime"), "Debug 输出应包含类型名");
    assert!(debug_str.contains("state"), "Debug 输出应包含 state 字段");
    // 不应泄漏 SQLite Connection 的内部表示
    assert!(
        debug_str.contains(".."),
        "应使用 finish_non_exhaustive 隐藏 conn，实际: {debug_str}"
    );
}

/// 验证 `AppCoreRuntime::finish` 返回的 CoreUpdate 包含 state 与 effects。
#[test]
fn test_app_core_runtime_finish_packs_state_and_effects() {
    let runtime = AppCoreRuntime {
        state: AppState::default(),
        conn: rusqlite::Connection::open_in_memory().unwrap(),
    };

    let effects = vec![CoreEffect {
        kind: "test_effect".to_string(),
        payload_json: r#"{"k":"v"}"#.to_string(),
    }];
    let update = runtime.finish(effects).expect("finish 应成功");

    assert_eq!(update.effects.len(), 1, "应保留传入的 effect");
    assert_eq!(update.effects[0].kind, "test_effect");
    let state: serde_json::Value =
        serde_json::from_str(&update.state_json).expect("state_json 应可解析");
    assert!(state.is_object(), "序列化结果应为对象");
}

