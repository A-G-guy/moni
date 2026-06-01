use rusqlite::{Connection, Row};
use serde::Serialize;

/// 聊天消息数据库行映射。
#[derive(Debug, Serialize)]
pub struct ChatMessageRow {
    pub id: i64,
    pub session_id: String,
    pub message_type: String,
    pub content: String,
    pub card_data_json: Option<String>,
    pub card_status: Option<String>,
    pub created_at: i64,
}

fn map_chat_message(row: &Row) -> Result<ChatMessageRow, rusqlite::Error> {
    Ok(ChatMessageRow {
        id: row.get("id")?,
        session_id: row.get("session_id")?,
        message_type: row.get("message_type")?,
        content: row.get("content")?,
        card_data_json: row.get("card_data_json")?,
        card_status: row.get("card_status")?,
        created_at: row.get("created_at")?,
    })
}

/// 插入新聊天消息。
pub fn insert(
    conn: &Connection,
    session_id: &str,
    message_type: &str,
    content: &str,
    card_data_json: Option<&str>,
    card_status: Option<&str>,
) -> Result<i64, rusqlite::Error> {
    let now = chrono::Utc::now().timestamp();

    conn.execute(
        "INSERT INTO chat_messages (session_id, message_type, content, card_data_json, card_status, created_at)
         VALUES (?1, ?2, ?3, ?4, ?5, ?6)",
        (session_id, message_type, content, card_data_json, card_status, now),
    )?;
    Ok(conn.last_insert_rowid())
}

/// 按会话 ID 分页查询消息，按 `created_at` 降序（最新消息在前）。
pub fn get_by_session(
    conn: &Connection,
    session_id: &str,
    limit: i64,
    offset: i64,
) -> Result<Vec<ChatMessageRow>, rusqlite::Error> {
    let mut stmt = conn.prepare(
        "SELECT * FROM chat_messages WHERE session_id = ?1 ORDER BY created_at DESC LIMIT ?2 OFFSET ?3"
    )?;
    let rows = stmt.query_map(
        rusqlite::params![session_id, limit, offset],
        map_chat_message,
    )?;
    rows.collect()
}

/// 更新消息卡片状态。
pub fn update_status(conn: &Connection, id: i64, card_status: &str) -> Result<(), rusqlite::Error> {
    let affected = conn.execute(
        "UPDATE chat_messages SET card_status = ?2 WHERE id = ?1",
        (id, card_status),
    )?;
    if affected == 0 {
        return Err(rusqlite::Error::QueryReturnedNoRows);
    }
    Ok(())
}

/// 更新消息卡片数据。
pub fn update_card_data(
    conn: &Connection,
    id: i64,
    card_data_json: &str,
) -> Result<(), rusqlite::Error> {
    let affected = conn.execute(
        "UPDATE chat_messages SET card_data_json = ?2 WHERE id = ?1",
        (id, card_data_json),
    )?;
    if affected == 0 {
        return Err(rusqlite::Error::QueryReturnedNoRows);
    }
    Ok(())
}

/// 根据 ID 删除单条消息。
pub fn delete_by_id(conn: &Connection, id: i64) -> Result<(), rusqlite::Error> {
    let affected = conn.execute("DELETE FROM chat_messages WHERE id = ?1", [id])?;
    if affected == 0 {
        return Err(rusqlite::Error::QueryReturnedNoRows);
    }
    Ok(())
}

/// 清空指定会话的所有消息。
pub fn clear_session(conn: &Connection, session_id: &str) -> Result<(), rusqlite::Error> {
    conn.execute(
        "DELETE FROM chat_messages WHERE session_id = ?1",
        [session_id],
    )?;
    Ok(())
}

#[cfg(test)]
#[path = "tests/chat_repo.rs"]
mod tests;
