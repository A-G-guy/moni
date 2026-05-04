mod common;

use moni_core::models::intent::{CoreIntent, MockPreset};

/// 测试清空所有数据后数据库回到初始状态。
#[test]
fn test_dev_clear_all_data() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().expect("测试 Runtime 创建失败");

    // 先创建一条记录
    let create_result = rt.block_on(async {
        core.dispatch(serde_json::to_string(&CoreIntent::CategoryList).unwrap())
            .await
    });
    assert!(create_result.is_ok());

    let state_json = create_result.unwrap().state_json;
    let state: serde_json::Value = serde_json::from_str(&state_json).unwrap();
    let categories = state["categories"].as_array().unwrap();
    assert!(!categories.is_empty(), "初始化后应有预设分类");

    // 执行清空
    let clear_result = rt.block_on(async {
        core.dispatch(serde_json::to_string(&CoreIntent::DevClearAllData).unwrap())
            .await
    });
    assert!(clear_result.is_ok());

    let state_json = clear_result.unwrap().state_json;
    let state: serde_json::Value = serde_json::from_str(&state_json).unwrap();
    let records = state["records"].as_array().unwrap();
    let categories = state["categories"].as_array().unwrap();

    assert!(records.is_empty(), "清空后记录应为空");
    assert!(!categories.is_empty(), "清空后仍应有预设分类");
}

/// 测试生成 Mock 数据。
#[test]
fn test_dev_generate_mock_data() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().expect("测试 Runtime 创建失败");

    let result = rt.block_on(async {
        core.dispatch(
            serde_json::to_string(&CoreIntent::DevGenerateMockData {
                count: 10,
                preset: MockPreset::Normal,
            })
            .unwrap(),
        )
        .await
    });
    assert!(result.is_ok());

    let state_json = result.unwrap().state_json;
    let state: serde_json::Value = serde_json::from_str(&state_json).unwrap();
    let records = state["records"].as_array().unwrap();

    assert_eq!(records.len(), 10, "应生成 10 条记录");
}

/// 测试生成 Stress Mock 数据。
#[test]
fn test_dev_generate_mock_data_stress() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().expect("测试 Runtime 创建失败");

    let result = rt.block_on(async {
        core.dispatch(
            serde_json::to_string(&CoreIntent::DevGenerateMockData {
                count: 20,
                preset: MockPreset::Stress,
            })
            .unwrap(),
        )
        .await
    });
    assert!(result.is_ok());

    let state_json = result.unwrap().state_json;
    let state: serde_json::Value = serde_json::from_str(&state_json).unwrap();
    let records = state["records"].as_array().unwrap();

    assert_eq!(records.len(), 20, "应生成 20 条记录");

    // 验证 stress 数据特性：至少有一条超长备注
    let has_long_note = records
        .iter()
        .any(|r| r["note"].as_str().unwrap_or("").len() > 100);
    assert!(has_long_note, "stress 预设应包含超长备注");
}

/// 测试没有分类时生成 Mock 数据应返回错误状态。
#[test]
fn test_dev_generate_mock_data_no_categories() {
    let core = common::setup_core();
    let rt = tokio::runtime::Runtime::new().expect("测试 Runtime 创建失败");

    let result = rt.block_on(async {
        core.dispatch(
            serde_json::to_string(&CoreIntent::DevGenerateMockData {
                count: 10,
                preset: MockPreset::Normal,
            })
            .unwrap(),
        )
        .await
    });

    assert!(result.is_ok());
    let state_json = result.unwrap().state_json;
    let state: serde_json::Value = serde_json::from_str(&state_json).unwrap();
    let error_message = state["ui"]["errorMessage"].as_str();
    assert!(error_message.is_some(), "没有分类时应设置错误信息");
    assert!(
        error_message.unwrap().contains("没有可用分类"),
        "错误信息应提示没有分类"
    );
}
