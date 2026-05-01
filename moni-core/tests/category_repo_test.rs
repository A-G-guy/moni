use moni_core::db::category_repo;
use moni_core::db::connection::open_in_memory;
use moni_core::db::schema::init_schema;
use moni_contracts::record::RecordType;

fn setup() -> rusqlite::Connection {
    let conn = open_in_memory().unwrap();
    init_schema(&conn).unwrap();
    conn
}

#[test]
fn test_insert_and_get() {
    let conn = setup();
    let id = category_repo::insert(&conn, "测试分类", RecordType::Expense, "test", "#000000", 1, false).unwrap();
    assert!(id > 0);

    let cat = category_repo::get_by_id(&conn, id).unwrap().unwrap();
    assert_eq!(cat.name, "测试分类");
    assert_eq!(cat.category_type, RecordType::Expense);
    assert!(!cat.is_preset);
}

#[test]
fn test_list_all_ordering() {
    let conn = setup();
    category_repo::insert(&conn, "分类B", RecordType::Expense, "b", "#000", 2, false).unwrap();
    category_repo::insert(&conn, "分类A", RecordType::Expense, "a", "#000", 1, false).unwrap();

    let list = category_repo::list_all(&conn).unwrap();
    assert_eq!(list.len(), 2);
    assert_eq!(list[0].name, "分类A");
    assert_eq!(list[1].name, "分类B");
}

#[test]
fn test_seed_presets() {
    let conn = setup();
    category_repo::seed_presets(&conn).unwrap();
    let list = category_repo::list_all(&conn).unwrap();
    assert_eq!(list.len(), moni_core::shared::constants::PRESET_CATEGORY_COUNT);
    assert!(list.iter().all(|c| c.is_preset));
}

#[test]
fn test_seed_presets_idempotent() {
    let conn = setup();
    category_repo::seed_presets(&conn).unwrap();
    category_repo::seed_presets(&conn).unwrap();
    let list = category_repo::list_all(&conn).unwrap();
    assert_eq!(list.len(), moni_core::shared::constants::PRESET_CATEGORY_COUNT);
}

#[test]
fn test_delete_preset_fails() {
    let conn = setup();
    category_repo::seed_presets(&conn).unwrap();
    let presets = category_repo::list_all(&conn).unwrap();
    let affected = category_repo::delete(&conn, presets[0].id).unwrap();
    assert_eq!(affected, 0);
}

#[test]
fn test_delete_custom_ok() {
    let conn = setup();
    let id = category_repo::insert(&conn, "自定义", RecordType::Expense, "x", "#000", 1, false).unwrap();
    let affected = category_repo::delete(&conn, id).unwrap();
    assert_eq!(affected, 1);
    assert!(category_repo::get_by_id(&conn, id).unwrap().is_none());
}

#[test]
fn test_is_in_use() {
    let conn = setup();
    let cat_id = category_repo::insert(&conn, "分类", RecordType::Expense, "x", "#000", 1, false).unwrap();
    assert!(!category_repo::is_in_use(&conn, cat_id).unwrap());

    conn.execute(
        "INSERT INTO records (amount_cents, record_type, category_id, note, created_at, updated_at)
         VALUES (100, 'expense', ?1, '', 0, 0)",
        [cat_id],
    ).unwrap();
    assert!(category_repo::is_in_use(&conn, cat_id).unwrap());
}
