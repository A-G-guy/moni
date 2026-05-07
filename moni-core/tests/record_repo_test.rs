use moni_contracts::record::RecordType;
use moni_core::db::category_repo;
use moni_core::db::connection::open_in_memory;
use moni_core::db::record_repo;
use moni_core::db::schema::init_schema;

fn setup() -> rusqlite::Connection {
    let conn = open_in_memory().unwrap();
    init_schema(&conn).unwrap();
    conn
}

fn create_category(conn: &rusqlite::Connection, name: &str, ty: RecordType) -> i64 {
    category_repo::insert(conn, name, None, ty, "icon", 1, None).unwrap()
}

#[test]
fn test_insert_and_get() {
    let conn = setup();
    let cat_id = create_category(&conn, "餐饮", RecordType::Expense);
    let id = record_repo::insert(&conn, 1234, RecordType::Expense, cat_id, None, "午餐", None).unwrap();
    assert!(id > 0);

    let rec = record_repo::get_by_id(&conn, id).unwrap().unwrap();
    assert_eq!(rec.amount_cents, 1234);
    assert_eq!(rec.record_type, RecordType::Expense);
    assert_eq!(rec.note, "午餐");
}

#[test]
fn test_list_paginated_ordering() {
    let conn = setup();
    let cat_id = create_category(&conn, "餐饮", RecordType::Expense);
    record_repo::insert(&conn, 100, RecordType::Expense, cat_id, None, "", Some(1000)).unwrap();
    record_repo::insert(&conn, 200, RecordType::Expense, cat_id, None, "", Some(2000)).unwrap();
    record_repo::insert(&conn, 300, RecordType::Expense, cat_id, None, "", Some(1500)).unwrap();

    let list = record_repo::list_paginated(&conn, 0, 10).unwrap();
    assert_eq!(list.len(), 3);
    assert_eq!(list[0].amount_cents, 200); // created_at=2000, newest
    assert_eq!(list[1].amount_cents, 300); // created_at=1500
    assert_eq!(list[2].amount_cents, 100); // created_at=1000
}

#[test]
fn test_list_paginated_paging() {
    let conn = setup();
    let cat_id = create_category(&conn, "餐饮", RecordType::Expense);
    for i in 0..5_i32 {
        record_repo::insert(
            &conn,
            i64::from(i + 1) * 100,
            RecordType::Expense,
            cat_id,
            None,
            "",
            Some(i64::from(i) * 1000),
        )
        .unwrap();
    }

    let page0 = record_repo::list_paginated(&conn, 0, 2).unwrap();
    assert_eq!(page0.len(), 2);

    let page1 = record_repo::list_paginated(&conn, 1, 2).unwrap();
    assert_eq!(page1.len(), 2);

    let page2 = record_repo::list_paginated(&conn, 2, 2).unwrap();
    assert_eq!(page2.len(), 1);
}

#[test]
fn test_update() {
    let conn = setup();
    let cat_id = create_category(&conn, "餐饮", RecordType::Expense);
    let id = record_repo::insert(&conn, 100, RecordType::Expense, cat_id, None, "旧备注", None).unwrap();

    record_repo::update(&conn, id, Some(200), None, None, None, Some("新备注")).unwrap();
    let rec = record_repo::get_by_id(&conn, id).unwrap().unwrap();
    assert_eq!(rec.amount_cents, 200);
    assert_eq!(rec.note, "新备注");
}

#[test]
fn test_delete() {
    let conn = setup();
    let cat_id = create_category(&conn, "餐饮", RecordType::Expense);
    let id = record_repo::insert(&conn, 100, RecordType::Expense, cat_id, None, "", None).unwrap();
    record_repo::delete(&conn, id).unwrap();
    assert!(record_repo::get_by_id(&conn, id).unwrap().is_none());
}

#[test]
fn test_list_by_date_range() {
    let conn = setup();
    let cat_id = create_category(&conn, "餐饮", RecordType::Expense);
    record_repo::insert(&conn, 100, RecordType::Expense, cat_id, None, "", Some(1000)).unwrap();
    record_repo::insert(&conn, 200, RecordType::Expense, cat_id, None, "", Some(2000)).unwrap();
    record_repo::insert(&conn, 300, RecordType::Expense, cat_id, None, "", Some(3000)).unwrap();

    let list = record_repo::list_by_date_range(&conn, 1500, 2500).unwrap();
    assert_eq!(list.len(), 1);
    assert_eq!(list[0].amount_cents, 200);
}

#[test]
fn test_category_aggregates() {
    let conn = setup();
    let cat_id = create_category(&conn, "餐饮", RecordType::Expense);
    // 2024-01-01 00:00:00 UTC = 1704067200
    record_repo::insert(&conn, 5000, RecordType::Expense, cat_id, None, "午餐", Some(1704067200),
    ).unwrap();
    record_repo::insert(
        &conn, 3000, RecordType::Expense, cat_id, None, "晚餐", Some(1704153600),
    ).unwrap();

    let agg = record_repo::category_aggregates(&conn, "2024-01"
    ).unwrap();
    assert_eq!(agg.len(), 1);
    assert_eq!(agg[0].0, cat_id);
    assert_eq!(agg[0].1, "餐饮");
    assert_eq!(agg[0].2, 8000);
}

#[test]
fn test_category_aggregates_by_parent() {
    let conn = setup();
    let parent_id = category_repo::insert(
        &conn, "餐饮", None, RecordType::Expense, "restaurant", 1, None,
    ).unwrap();
    let child_id = category_repo::insert(
        &conn, "早餐", None, RecordType::Expense, "bakery", 2, Some(parent_id),
    ).unwrap();
    let other_id = category_repo::insert(
        &conn, "交通", None, RecordType::Expense, "car", 3, None,
    ).unwrap();

    // 2024-01-01 00:00:00 UTC = 1704067200
    record_repo::insert(
        &conn, 5000, RecordType::Expense, parent_id, None, "正餐", Some(1704067200),
    ).unwrap();
    record_repo::insert(
        &conn, 2000, RecordType::Expense, child_id, Some(parent_id), "早餐", Some(1704153600),
    ).unwrap();
    record_repo::insert(
        &conn, 3000, RecordType::Expense, other_id, None, "地铁", Some(1704240000),
    ).unwrap();

    let agg = record_repo::category_aggregates_by_parent(
        &conn, "2024-01"
    ).unwrap();
    // 应合并为 2 条：餐饮(5000+2000=7000)、交通(3000)
    assert_eq!(agg.len(), 2);
    let dining = agg.iter().find(|a| a.0 == parent_id).unwrap();
    assert_eq!(dining.1, "餐饮");
    assert_eq!(dining.2, 7000);

    let transport = agg.iter().find(|a| a.0 == other_id).unwrap();
    assert_eq!(transport.1, "交通");
    assert_eq!(transport.2, 3000);
}
