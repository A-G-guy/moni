use rusqlite::Connection;

use super::{clear_session, delete_by_id, get_by_session, insert, update_card_data, update_status};
use crate::db::schema;

#[test]
fn chat_crud_roundtrip() {
    let conn = Connection::open_in_memory().expect("db");
    schema::init_schema(&conn).expect("schema");

    let user_id = insert(&conn, "s1", "user_text", "午餐 35", None, None).expect("insert user");
    let card_id = insert(
        &conn,
        "s1",
        "ai_card",
        "",
        Some(r#"{"amount_cents":3500}"#),
        Some("draft"),
    )
    .expect("insert card");

    let rows = get_by_session(&conn, "s1", 10, 0).expect("list");
    assert_eq!(rows.len(), 2);
    assert_eq!(rows[0].id, user_id);
    assert_eq!(rows[1].id, card_id);
    let card = rows.iter().find(|row| row.id == card_id).expect("card");
    assert_eq!(card.card_status.as_deref(), Some("draft"));
    assert_eq!(
        card.card_data_json.as_deref(),
        Some(r#"{"amount_cents":3500}"#)
    );

    update_status(&conn, card_id, "saved").expect("update");
    update_card_data(&conn, card_id, r#"{"amount_cents":3600}"#).expect("update card data");
    let rows = get_by_session(&conn, "s1", 10, 0).expect("list after update");
    let card = rows.iter().find(|row| row.id == card_id).expect("card");
    assert_eq!(card.card_status.as_deref(), Some("saved"));
    assert_eq!(
        card.card_data_json.as_deref(),
        Some(r#"{"amount_cents":3600}"#)
    );

    delete_by_id(&conn, user_id).expect("delete");
    assert_eq!(get_by_session(&conn, "s1", 10, 0).expect("list").len(), 1);

    clear_session(&conn, "s1").expect("clear");
    assert!(get_by_session(&conn, "s1", 10, 0).expect("list").is_empty());
}

#[test]
fn chat_list_returns_latest_page_in_chronological_order() {
    let conn = Connection::open_in_memory().expect("db");
    schema::init_schema(&conn).expect("schema");

    let first = insert(&conn, "s1", "user_text", "第一条", None, None).expect("insert first");
    let second = insert(&conn, "s1", "ai_card", "第二条", None, None).expect("insert second");
    let third = insert(&conn, "s1", "ai_text", "第三条", None, None).expect("insert third");

    let rows = get_by_session(&conn, "s1", 2, 0).expect("list latest two");

    assert_eq!(
        rows.iter().map(|row| row.id).collect::<Vec<_>>(),
        vec![second, third]
    );
    assert!(!rows.iter().any(|row| row.id == first));
}
