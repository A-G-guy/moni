use moni_contracts::category::Category;
use moni_contracts::record::{Record, RecordType};

use moni_core::dto::category_dto::category_list_to_dto;
use moni_core::dto::{
    CategoryDto, RecordDayGroup, RecordDto, group_records_by_date, record_list_to_dto,
};

fn sample_category(id: i64, name: &str) -> Category {
    Category {
        id,
        name: name.to_string(),
        description: None,
        category_type: RecordType::Expense,
        icon_name: "star".to_string(),
        sort_order: 0,
        parent_id: None,
        archived_at: None,
        created_at: 1000,
        updated_at: 1000,
    }
}

fn sample_record(id: i64, category_id: i64, amount: i64, ty: RecordType, ts: i64) -> Record {
    Record {
        id,
        amount_cents: amount,
        record_type: ty,
        category_id,
        parent_category_id: None,
        note: format!("note-{id}"),
        created_at: ts,
        updated_at: ts,
    }
}

#[test]
fn test_category_list_to_dto_preserves_fields() {
    let categories = vec![
        sample_category(1, "餐饮"),
        Category {
            description: Some("早餐说明".to_string()),
            parent_id: Some(1),
            ..sample_category(2, "早餐")
        },
    ];
    let dtos = category_list_to_dto(&categories);

    assert_eq!(dtos.len(), 2);
    assert_eq!(dtos[0].id, 1);
    assert_eq!(dtos[0].name, "餐饮");
    assert!(dtos[0].description.is_none());
    assert!(dtos[0].parent_id.is_none());

    assert_eq!(dtos[1].id, 2);
    assert_eq!(dtos[1].name, "早餐");
    assert_eq!(dtos[1].description.as_deref(), Some("早餐说明"));
    assert_eq!(dtos[1].parent_id, Some(1));
}

#[test]
fn test_record_list_to_dto_with_known_category() {
    let cats = vec![CategoryDto::from_category(&sample_category(1, "餐饮"))];
    let records = vec![sample_record(10, 1, 1000, RecordType::Expense, 100)];

    let dtos = record_list_to_dto(&records, &cats);
    assert_eq!(dtos.len(), 1);
    assert_eq!(dtos[0].category_name, "餐饮");
    assert_eq!(dtos[0].amount_cents, 1000);
}

#[test]
fn test_record_list_to_dto_unknown_category_falls_back() {
    let cats: Vec<CategoryDto> = Vec::new();
    let records = vec![sample_record(10, 999, 500, RecordType::Income, 100)];

    let dtos = record_list_to_dto(&records, &cats);
    assert_eq!(dtos.len(), 1);
    assert_eq!(
        dtos[0].category_name, "未知分类",
        "找不到分类时应回退到'未知分类'"
    );
    assert_eq!(dtos[0].record_type, RecordType::Income);
}

#[test]
fn test_group_records_by_date_sums_income_and_expense() {
    // 构造同一天的混合记录与不同天的记录
    // 2026-05-06 00:00:00 UTC = 1778457600
    let day_a_morning = 1_778_457_600;
    let day_a_evening = 1_778_457_600 + 12 * 3600;
    let day_b = 1_778_457_600 + 86_400;

    let cats = vec![CategoryDto::from_category(&sample_category(1, "餐饮"))];
    let records = vec![
        sample_record(1, 1, 1000, RecordType::Expense, day_a_morning),
        sample_record(2, 1, 5000, RecordType::Income, day_a_evening),
        sample_record(3, 1, 2000, RecordType::Expense, day_b),
    ];
    let dtos = record_list_to_dto(&records, &cats);

    let groups: Vec<RecordDayGroup> = group_records_by_date(&dtos);
    assert_eq!(groups.len(), 2, "应分成两天");

    // 按倒序排列：最新的天在前
    assert!(groups[0].date > groups[1].date);
    assert_eq!(groups[0].records.len(), 1);
    assert_eq!(groups[0].expense_cents, 2000);
    assert_eq!(groups[0].income_cents, 0);

    assert_eq!(groups[1].records.len(), 2);
    assert_eq!(groups[1].expense_cents, 1000);
    assert_eq!(groups[1].income_cents, 5000);
}

#[test]
fn test_group_records_by_date_handles_empty() {
    let groups = group_records_by_date(&[]);
    assert!(groups.is_empty(), "空输入应返回空分组");
}

#[test]
fn test_record_dto_serde_camel_case() {
    let cats = vec![CategoryDto::from_category(&sample_category(1, "餐饮"))];
    let mut record = sample_record(10, 1, 1000, RecordType::Expense, 100);
    record.parent_category_id = Some(7);
    let records = vec![record];
    let dto: RecordDto = record_list_to_dto(&records, &cats)
        .into_iter()
        .next()
        .unwrap();
    let json = serde_json::to_value(&dto).unwrap();
    assert_eq!(json["amountCents"], 1000);
    assert_eq!(json["categoryId"], 1);
    assert_eq!(json["parentCategoryId"], 7);
    assert_eq!(json["categoryName"], "餐饮");
    assert_eq!(json["createdAt"], 100);
}
