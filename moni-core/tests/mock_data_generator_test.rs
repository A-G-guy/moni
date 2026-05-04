use moni_contracts::category::Category;
use moni_contracts::record::RecordType;
use moni_core::domain::dev::mock_data_generator;
use moni_core::models::intent::MockPreset;

fn create_test_categories() -> Vec<Category> {
    vec![
        Category {
            id: 1,
            name: "餐饮".to_string(),
            category_type: RecordType::Expense,
            icon_name: "restaurant".to_string(),
            color_hex: "#FF6B6B".to_string(),
            sort_order: 1,
            is_preset: true,
            created_at: 0,
            updated_at: 0,
        },
        Category {
            id: 2,
            name: "工资".to_string(),
            category_type: RecordType::Income,
            icon_name: "payments".to_string(),
            color_hex: "#00B894".to_string(),
            sort_order: 9,
            is_preset: true,
            created_at: 0,
            updated_at: 0,
        },
    ]
}

/// 测试正常预设生成。
#[test]
fn test_generate_normal() {
    let categories = create_test_categories();
    let records = mock_data_generator::generate(&categories, 50, MockPreset::Normal
    ).expect("生成应成功");

    assert_eq!(records.len(), 50);

    for record in &records {
        assert!(record.amount_cents > 0);
        assert!(
            categories.iter().any(|c| c.id == record.category_id),
            "category_id 应在可用分类中"
        );
    }
}

/// 测试 Stress 预设生成。
#[test]
fn test_generate_stress() {
    let categories = create_test_categories();
    let records = mock_data_generator::generate(
        &categories, 20, MockPreset::Stress
    ).expect("生成应成功");

    assert_eq!(records.len(), 20);

    // 验证 stress 特性
    let has_large_amount = records.iter().any(|r| r.amount_cents == 99999999);
    let has_tiny_amount = records.iter().any(|r| r.amount_cents == 1);
    let has_long_note = records.iter().any(|r| r.note.len() > 100);
    let has_empty_note = records.iter().any(|r| r.note.is_empty());

    assert!(has_large_amount, "应包含超大金额");
    assert!(has_tiny_amount, "应包含极小金额");
    assert!(has_long_note, "应包含超长备注");
    assert!(has_empty_note, "应包含空备注");
}

/// 测试空分类列表应返回错误。
#[test]
fn test_generate_empty_categories() {
    let categories: Vec<Category> = vec![];
    let result = mock_data_generator::generate(&categories, 10, MockPreset::Normal
    );

    assert!(result.is_err(), "空分类应返回错误");
}

/// 测试生成记录按时间降序排列。
#[test]
fn test_generate_sorted_descending() {
    let categories = create_test_categories();
    let records = mock_data_generator::generate(
        &categories, 30, MockPreset::Normal
    ).expect("生成应成功");

    for i in 1..records.len() {
        assert!(
            records[i - 1].created_at >= records[i].created_at,
            "记录应按 created_at 降序排列"
        );
    }
}

/// 测试 MockPreset 序列化反序列化。
#[test]
fn test_mock_preset_serde() {
    let normal = serde_json::to_string(&MockPreset::Normal).unwrap();
    assert_eq!(normal, "\"normal\"");

    let stress = serde_json::to_string(&MockPreset::Stress).unwrap();
    assert_eq!(stress, "\"stress\"");

    let parsed: MockPreset = serde_json::from_str("\"normal\"").unwrap();
    assert!(matches!(parsed, MockPreset::Normal));

    let parsed: MockPreset = serde_json::from_str("\"stress\"").unwrap();
    assert!(matches!(parsed, MockPreset::Stress));
}
