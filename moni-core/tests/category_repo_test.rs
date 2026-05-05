use moni_contracts::record::RecordType;
use moni_core::db::category_repo;
use moni_core::db::connection::open_in_memory;
use moni_core::db::schema::init_schema;

fn setup() -> rusqlite::Connection {
    let conn = open_in_memory().unwrap();
    init_schema(&conn).unwrap();
    conn
}

#[test]
fn test_insert_and_get() {
    let conn = setup();
    let id = category_repo::insert(
        &conn,
        "测试分类",
        None,
        RecordType::Expense,
        "test",
        1,
        false,
    )
    .unwrap();
    assert!(id > 0);

    let cat = category_repo::get_by_id(&conn, id).unwrap().unwrap();
    assert_eq!(cat.name, "测试分类");
    assert_eq!(cat.description, None);
    assert_eq!(cat.category_type, RecordType::Expense);
    assert!(!cat.is_preset);
    assert!(cat.is_active());
}

#[test]
fn test_insert_with_description() {
    let conn = setup();
    let id = category_repo::insert(
        &conn,
        "测试分类",
        Some("描述内容"),
        RecordType::Income,
        "test",
        1,
        false,
    )
    .unwrap();

    let cat = category_repo::get_by_id(&conn, id).unwrap().unwrap();
    assert_eq!(cat.description, Some("描述内容".to_string()));
}

#[test]
fn test_list_all_ordering() {
    let conn = setup();
    category_repo::insert(&conn, "分类B", None, RecordType::Expense, "b", 2, false).unwrap();
    category_repo::insert(&conn, "分类A", None, RecordType::Expense, "a", 1, false).unwrap();

    let list = category_repo::list_all(&conn).unwrap();
    assert_eq!(list.len(), 2);
    assert_eq!(list[0].name, "分类A");
    assert_eq!(list[1].name, "分类B");
}

#[test]
fn test_list_active_excludes_archived() {
    let conn = setup();
    let id =
        category_repo::insert(&conn, "活跃分类", None, RecordType::Expense, "a", 1, false).unwrap();
    category_repo::archive(&conn, id).unwrap();

    let all = category_repo::list_all(&conn).unwrap();
    let active = category_repo::list_active(&conn).unwrap();

    assert_eq!(all.len(), 1);
    assert_eq!(active.len(), 0);
}

#[test]
fn test_seed_presets() {
    let conn = setup();
    category_repo::seed_presets(&conn).unwrap();
    let list = category_repo::list_all(&conn).unwrap();
    assert_eq!(
        list.len(),
        moni_core::shared::constants::PRESET_CATEGORY_COUNT
    );
    assert!(list.iter().all(|c| c.is_preset));
    assert!(
        list.iter()
            .all(moni_contracts::category::Category::is_active)
    );
}

#[test]
fn test_seed_presets_idempotent() {
    let conn = setup();
    category_repo::seed_presets(&conn).unwrap();
    category_repo::seed_presets(&conn).unwrap();
    let list = category_repo::list_all(&conn).unwrap();
    assert_eq!(
        list.len(),
        moni_core::shared::constants::PRESET_CATEGORY_COUNT
    );
}

#[test]
fn test_update_custom_ok() {
    let conn = setup();
    let id =
        category_repo::insert(&conn, "原名", None, RecordType::Expense, "x", 1, false).unwrap();
    let affected =
        category_repo::update(&conn, id, Some("新名"), Some("新描述"), Some("new_icon")).unwrap();
    assert_eq!(affected, 1);

    let cat = category_repo::get_by_id(&conn, id).unwrap().unwrap();
    assert_eq!(cat.name, "新名");
    assert_eq!(cat.description, Some("新描述".to_string()));
    assert_eq!(cat.icon_name, "new_icon");
}

#[test]
fn test_update_preset_ok() {
    let conn = setup();
    category_repo::seed_presets(&conn).unwrap();
    let presets = category_repo::list_all(&conn).unwrap();
    let affected = category_repo::update(&conn, presets[0].id, Some("改名"), None, None).unwrap();
    assert_eq!(affected, 1);

    let cat = category_repo::get_by_id(&conn, presets[0].id).unwrap().unwrap();
    assert_eq!(cat.name, "改名");
}

#[test]
fn test_archive_and_unarchive() {
    let conn = setup();
    let id =
        category_repo::insert(&conn, "归档测试", None, RecordType::Expense, "x", 1, false).unwrap();

    assert!(
        category_repo::get_by_id(&conn, id)
            .unwrap()
            .unwrap()
            .is_active()
    );

    let affected = category_repo::archive(&conn, id).unwrap();
    assert_eq!(affected, 1);
    let cat = category_repo::get_by_id(&conn, id).unwrap().unwrap();
    assert!(!cat.is_active());
    assert!(cat.archived_at.is_some());

    let affected = category_repo::unarchive(&conn, id).unwrap();
    assert_eq!(affected, 1);
    let cat = category_repo::get_by_id(&conn, id).unwrap().unwrap();
    assert!(cat.is_active());
    assert!(cat.archived_at.is_none());
}

#[test]
fn test_is_in_use() {
    let conn = setup();
    let cat_id =
        category_repo::insert(&conn, "分类", None, RecordType::Expense, "x", 1, false).unwrap();
    assert!(!category_repo::is_in_use(&conn, cat_id).unwrap());

    conn.execute(
        "INSERT INTO records (amount_cents, record_type, category_id, note, created_at, updated_at)
         VALUES (100, 'expense', ?1, '', 0, 0)",
        [cat_id],
    )
    .unwrap();
    assert!(category_repo::is_in_use(&conn, cat_id).unwrap());
}
