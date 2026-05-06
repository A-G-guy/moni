mod common;

use moni_contracts::record::RecordType;
use moni_core::db::category_repo;
use moni_core::db::connection::open_in_memory;
use moni_core::db::schema::init_schema;

fn setup_conn() -> rusqlite::Connection {
    let conn = open_in_memory().unwrap();
    init_schema(&conn).unwrap();
    conn
}

/// 通过 dispatch 创建分类后，使用 snapshot 验证 insert 结果正确写入状态。
#[test]
fn test_create_category_snapshot_verifies_insert() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent = r#"{"type":"category_create","name":"快照验证","description":"测试描述","category_type":"expense","icon_name":"verified"}"#.to_string();
        let update = core.dispatch(intent).await.expect("dispatch 不应失败");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null(), "不应有错误");

        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let categories = state["categories"].as_array().unwrap();

        let custom = categories.iter().find(|c| c["name"] == "快照验证").unwrap();
        assert_eq!(custom["iconName"], "verified");
        assert_eq!(custom["description"], "测试描述");
        assert!(custom["archivedAt"].is_null());
        assert!(custom["parentId"].is_null());
    });
}

/// 仅更新 icon_name 后，snapshot 应反映变更（覆盖 update 正常路径）。
#[test]
fn test_update_icon_only_snapshot() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent = r#"{"type":"category_create","name":"仅改图标","category_type":"expense","icon_name":"old_icon"}"#.to_string();
        core.dispatch(intent).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let custom_id = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "仅改图标").unwrap()["id"]
            .as_i64().unwrap();

        let update_intent = format!(
            r#"{{"type":"category_update","id":{custom_id},"icon_name":"new_icon","clear_parent_id":false}}"#
        );
        let update = core.dispatch(update_intent).await.expect("更新应成功");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null());

        let cat = state["categories"].as_array().unwrap()
            .iter().find(|c| c["id"] == custom_id).unwrap();
        assert_eq!(cat["iconName"], "new_icon");
        assert_eq!(cat["name"], "仅改图标");
    });
}

/// 仅更新 description 后，snapshot 应反映变更（覆盖 update 正常路径）。
#[test]
fn test_update_description_only_snapshot() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent = r#"{"type":"category_create","name":"仅改描述","category_type":"expense","icon_name":"star"}"#.to_string();
        core.dispatch(intent).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let custom_id = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "仅改描述").unwrap()["id"]
            .as_i64().unwrap();

        let update_intent = format!(
            r#"{{"type":"category_update","id":{custom_id},"description":"新描述内容","clear_parent_id":false}}"#
        );
        let update = core.dispatch(update_intent).await.expect("更新应成功");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null());

        let cat = state["categories"].as_array().unwrap()
            .iter().find(|c| c["id"] == custom_id).unwrap();
        assert_eq!(cat["description"], "新描述内容");
    });
}

/// 归档分类后，category_list 应包含该归档项（覆盖 list_all 正常路径）。
#[test]
fn test_archive_then_list_all_includes_archived() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent = r#"{"type":"category_create","name":"待归档","category_type":"expense","icon_name":"star"}"#.to_string();
        core.dispatch(intent).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let custom_id = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "待归档").unwrap()["id"]
            .as_i64().unwrap();

        core.dispatch(format!(r#"{{"type":"category_archive","id":{custom_id}}}"#)).await.unwrap();

        let intent = r#"{"type":"category_list"}"#.to_string();
        let update = core.dispatch(intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let categories = state["categories"].as_array().unwrap();

        let archived = categories.iter().find(|c| c["id"] == custom_id).unwrap();
        assert!(!archived["archivedAt"].is_null(), "归档后 list_all 应包含 archivedAt");
    });
}

/// 归档后恢复，category_list 中的分类应重新变为活跃（覆盖 archive + unarchive + list_all）。
#[test]
fn test_archive_unarchive_cycle_snapshot() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent = r#"{"type":"category_create","name":"归档循环","category_type":"expense","icon_name":"star"}"#.to_string();
        core.dispatch(intent).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let custom_id = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "归档循环").unwrap()["id"]
            .as_i64().unwrap();

        // 归档
        core.dispatch(format!(r#"{{"type":"category_archive","id":{custom_id}}}"#)).await.unwrap();
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let cat = state["categories"].as_array().unwrap()
            .iter().find(|c| c["id"] == custom_id).unwrap();
        assert!(!cat["archivedAt"].is_null());

        // 恢复
        core.dispatch(format!(r#"{{"type":"category_unarchive","id":{custom_id}}}"#)).await.unwrap();
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let cat = state["categories"].as_array().unwrap()
            .iter().find(|c| c["id"] == custom_id).unwrap();
        assert!(cat["archivedAt"].is_null());
    });
}

/// 创建二级分类后，通过 snapshot 验证 parent_id 正确写入（覆盖 insert 带子分类路径）。
#[test]
fn test_create_sub_category_snapshot() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let parent_id = state["categories"]
            .as_array().unwrap()
            .iter().find(|c| c["name"] == "餐饮").unwrap()["id"]
            .as_i64().unwrap();

        let intent = format!(
            r#"{{"type":"category_create","name":"子分类快照","category_type":"expense","icon_name":"child","parent_id":{parent_id}}}"#
        );
        let update = core.dispatch(intent).await.expect("创建子分类应成功");
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null());

        let cat = state["categories"].as_array().unwrap()
            .iter().find(|c| c["name"] == "子分类快照").unwrap();
        assert_eq!(cat["parentId"], parent_id);
    });
}

// ============================================================================
// 以下函数无 dispatch 入口，直接调用 repo 函数覆盖正常路径。
// ============================================================================

/// list_active 应排除已归档分类。
#[test]
fn test_list_active_excludes_archived_direct() {
    let conn = setup_conn();
    let id = category_repo::insert(&conn, "活跃与归档", None, RecordType::Expense, "star", 1, None).unwrap();

    let active_before = category_repo::list_active(&conn).unwrap();
    assert!(active_before.iter().any(|c| c.id == id));

    category_repo::archive(&conn, id).unwrap();

    let active_after = category_repo::list_active(&conn).unwrap();
    assert!(!active_after.iter().any(|c| c.id == id));
}

/// list_by_parent 应返回指定父分类下的所有子分类。
#[test]
fn test_list_by_parent_returns_children_direct() {
    let conn = setup_conn();
    let parent_id = category_repo::insert(&conn, "父分类", None, RecordType::Expense, "parent", 1, None).unwrap();
    let _child_a = category_repo::insert(&conn, "子A", None, RecordType::Expense, "a", 1, Some(parent_id)).unwrap();
    let _child_b = category_repo::insert(&conn, "子B", None, RecordType::Expense, "b", 2, Some(parent_id)).unwrap();

    let children = category_repo::list_by_parent(&conn, parent_id).unwrap();
    assert_eq!(children.len(), 2);
    assert!(children.iter().any(|c| c.name == "子A"));
    assert!(children.iter().any(|c| c.name == "子B"));
}

/// list_by_parent 对无子分类的父分类应返回空列表。
#[test]
fn test_list_by_parent_empty_direct() {
    let conn = setup_conn();
    let parent_id = category_repo::insert(&conn, "空父", None, RecordType::Expense, "empty", 1, None).unwrap();

    let children = category_repo::list_by_parent(&conn, parent_id).unwrap();
    assert!(children.is_empty());
}

/// has_children 在有子分类时返回 true，无子分类时返回 false。
#[test]
fn test_has_children_true_and_false_direct() {
    let conn = setup_conn();
    let parent_id = category_repo::insert(&conn, "无子父", None, RecordType::Expense, "star", 1, None).unwrap();
    assert!(!category_repo::has_children(&conn, parent_id).unwrap());

    let _child_id = category_repo::insert(&conn, "子", None, RecordType::Expense, "child", 1, Some(parent_id)).unwrap();
    assert!(category_repo::has_children(&conn, parent_id).unwrap());
}

/// is_in_use 在有记录引用时返回 true，无记录引用时返回 false。
#[test]
fn test_is_in_use_with_and_without_record_direct() {
    let conn = setup_conn();
    let cat_id = category_repo::insert(&conn, "使用测试", None, RecordType::Expense, "star", 1, None).unwrap();
    assert!(!category_repo::is_in_use(&conn, cat_id).unwrap());

    conn.execute(
        "INSERT INTO records (amount_cents, record_type, category_id, note, created_at, updated_at) VALUES (100, 'expense', ?1, '', 0, 0)",
        [cat_id],
    ).unwrap();
    assert!(category_repo::is_in_use(&conn, cat_id).unwrap());
}
