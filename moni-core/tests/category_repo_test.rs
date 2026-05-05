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
        None,
    )
    .unwrap();
    assert!(id > 0);

    let cat = category_repo::get_by_id(&conn, id).unwrap().unwrap();
    assert_eq!(cat.name, "测试分类");
    assert_eq!(cat.description, None);
    assert_eq!(cat.category_type, RecordType::Expense);
    assert!(cat.is_active());
    assert!(cat.parent_id.is_none());
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
        None,
    )
    .unwrap();

    let cat = category_repo::get_by_id(&conn, id).unwrap().unwrap();
    assert_eq!(cat.description, Some("描述内容".to_string()));
}

#[test]
fn test_list_all_ordering() {
    let conn = setup();
    category_repo::insert(&conn, "分类B", None, RecordType::Expense, "b", 2, None).unwrap();
    category_repo::insert(&conn, "分类A", None, RecordType::Expense, "a", 1, None).unwrap();

    let list = category_repo::list_all(&conn).unwrap();
    assert_eq!(list.len(), 2);
    assert_eq!(list[0].name, "分类A");
    assert_eq!(list[1].name, "分类B");
}

#[test]
fn test_list_active_excludes_archived() {
    let conn = setup();
    let id =
        category_repo::insert(&conn, "活跃分类", None, RecordType::Expense, "a", 1, None).unwrap();
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
        category_repo::insert(&conn, "原名", None, RecordType::Expense, "x", 1, None).unwrap();
    let affected =
        category_repo::update(&conn, id, Some("新名"), Some("新描述"), Some("new_icon"), None).unwrap();
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
    let affected = category_repo::update(&conn, presets[0].id, Some("改名"), None, None, None).unwrap();
    assert_eq!(affected, 1);

    let cat = category_repo::get_by_id(&conn, presets[0].id).unwrap().unwrap();
    assert_eq!(cat.name, "改名");
}

#[test]
fn test_archive_and_unarchive() {
    let conn = setup();
    let id =
        category_repo::insert(&conn, "归档测试", None, RecordType::Expense, "x", 1, None).unwrap();

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
        category_repo::insert(&conn, "分类", None, RecordType::Expense, "x", 1, None).unwrap();
    assert!(!category_repo::is_in_use(&conn, cat_id).unwrap());

    conn.execute(
        "INSERT INTO records (amount_cents, record_type, category_id, note, created_at, updated_at)
         VALUES (100, 'expense', ?1, '', 0, 0)",
        [cat_id],
    )
    .unwrap();
    assert!(category_repo::is_in_use(&conn, cat_id).unwrap());
}

#[test]
fn test_insert_sub_category() {
    let conn = setup();
    let parent_id = category_repo::insert(
        &conn, "父分类", None, RecordType::Expense, "parent", 1, None,
    ).unwrap();
    let child_id = category_repo::insert(
        &conn, "子分类", None, RecordType::Expense, "child", 2, Some(parent_id),
    ).unwrap();

    let child = category_repo::get_by_id(&conn, child_id).unwrap().unwrap();
    assert_eq!(child.parent_id, Some(parent_id));
    assert!(child.is_sub_category());
}

#[test]
fn test_list_by_parent() {
    let conn = setup();
    let parent_id = category_repo::insert(
        &conn, "父分类", None, RecordType::Expense, "parent", 1, None,
    ).unwrap();
    let _child_a = category_repo::insert(
        &conn, "子分类A", None, RecordType::Expense, "a", 1, Some(parent_id),
    ).unwrap();
    let _child_b = category_repo::insert(
        &conn, "子分类B", None, RecordType::Expense, "b", 2, Some(parent_id),
    ).unwrap();
    let _other = category_repo::insert(
        &conn, "其他分类", None, RecordType::Expense, "other", 3, None,
    ).unwrap();

    let children = category_repo::list_by_parent(&conn, parent_id).unwrap();
    assert_eq!(children.len(), 2);
    assert_eq!(children[0].name, "子分类A");
    assert_eq!(children[1].name, "子分类B");
}

#[test]
fn test_has_children() {
    let conn = setup();
    let parent_id = category_repo::insert(
        &conn, "父分类", None, RecordType::Expense, "parent", 1, None,
    ).unwrap();
    assert!(!category_repo::has_children(&conn, parent_id).unwrap());

    let _child_id = category_repo::insert(
        &conn, "子分类", None, RecordType::Expense, "child", 1, Some(parent_id),
    ).unwrap();
    assert!(category_repo::has_children(&conn, parent_id).unwrap());
}

#[test]
fn test_update_set_parent() {
    let conn = setup();
    let parent_id = category_repo::insert(
        &conn, "父分类", None, RecordType::Expense, "parent", 1, None,
    ).unwrap();
    let child_id = category_repo::insert(
        &conn, "原为一级", None, RecordType::Expense, "icon", 2, None,
    ).unwrap();

    let affected = category_repo::update(
        &conn, child_id, None, None, None, Some(Some(parent_id)),
    ).unwrap();
    assert_eq!(affected, 1);

    let cat = category_repo::get_by_id(&conn, child_id).unwrap().unwrap();
    assert_eq!(cat.parent_id, Some(parent_id));
}

#[test]
fn test_update_clear_parent() {
    let conn = setup();
    let parent_id = category_repo::insert(
        &conn, "父分类", None, RecordType::Expense, "parent", 1, None,
    ).unwrap();
    let child_id = category_repo::insert(
        &conn, "原为二级", None, RecordType::Expense, "icon", 2, Some(parent_id),
    ).unwrap();

    let affected = category_repo::update(
        &conn, child_id, None, None, None, Some(None),
    ).unwrap();
    assert_eq!(affected, 1);

    let cat = category_repo::get_by_id(&conn, child_id).unwrap().unwrap();
    assert!(cat.parent_id.is_none());
    assert!(!cat.is_sub_category());
}

#[test]
fn test_list_all_hierarchy_ordering() {
    let conn = setup();
    // 插入顺序：父B、父A、子A1、子B1
    let parent_b = category_repo::insert(
        &conn, "父B", None, RecordType::Expense, "b", 2, None,
    ).unwrap();
    let parent_a = category_repo::insert(
        &conn, "父A", None, RecordType::Expense, "a", 1, None,
    ).unwrap();
    let _child_a1 = category_repo::insert(
        &conn, "子A1", None, RecordType::Expense, "a1", 1, Some(parent_a),
    ).unwrap();
    let _child_b1 = category_repo::insert(
        &conn, "子B1", None, RecordType::Expense, "b1", 1, Some(parent_b),
    ).unwrap();

    let list = category_repo::list_all(&conn).unwrap();
    assert_eq!(list.len(), 4);
    // SQL 排序：一级分类在前（按 sort_order），子分类在后（按 parent_id、sort_order）
    assert_eq!(list[0].name, "父A");  // sort_order=1
    assert_eq!(list[1].name, "父B");  // sort_order=2
    assert_eq!(list[2].name, "子B1"); // parent_id=parent_b(1)
    assert_eq!(list[3].name, "子A1"); // parent_id=parent_a(2)
}

#[test]
fn test_seed_presets_has_sub_categories() {
    let conn = setup();
    category_repo::seed_presets(&conn).unwrap();
    let list = category_repo::list_all(&conn).unwrap();

    let has_children = list.iter().any(|c| c.parent_id.is_some());
    assert!(has_children, "预设分类应包含二级分类");

    let parent_names: Vec<_> = list
        .iter()
        .filter(|c| c.parent_id.is_none())
        .map(|c| c.name.as_str())
        .collect();
    assert!(parent_names.contains(&"餐饮"));
    assert!(parent_names.contains(&"交通"));

    let child_names: Vec<_> = list
        .iter()
        .filter(|c| c.parent_id.is_some())
        .map(|c| c.name.as_str())
        .collect();
    assert!(child_names.contains(&"早餐"));
    assert!(child_names.contains(&"地铁"));
}
