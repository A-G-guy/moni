use moni_contracts::category::Category;
use moni_contracts::record::Record;
use moni_contracts::record::RecordType;

#[test]
fn test_generate_csv() {
    let records = vec![Record {
        id: 1,
        amount_cents: 1234,
        record_type: RecordType::Expense,
        category_id: 1,
        note: "午餐".to_string(),
        created_at: 1000,
        updated_at: 1000,
    }];
    let categories = vec![Category {
        id: 1,
        name: "餐饮".to_string(),
        description: Some("日常餐饮消费".to_string()),
        category_type: RecordType::Expense,
        icon_name: "restaurant".to_string(),
        sort_order: 1,
        is_preset: true,
        archived_at: None,
        created_at: 0,
        updated_at: 0,
    }];

    let csv = moni_core::domain::export::csv_generator::generate(&records, &categories);
    assert!(csv.contains("午餐"));
    assert!(csv.contains("餐饮"));
    assert!(csv.contains("支出"));
    assert!(csv.contains("日常餐饮消费"));
}
