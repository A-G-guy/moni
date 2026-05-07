use rusqlite::{Connection, OptionalExtension};

/// 查询单个设置项。
pub fn get(conn: &Connection, key: &str) -> Result<Option<String>, rusqlite::Error> {
    conn.query_row(
        "SELECT value FROM settings WHERE key = ?1",
        [key],
        |row| row.get::<_, String>(0),
    )
    .optional()
}

/// 插入或更新设置项（UPSERT）。
pub fn set(conn: &Connection, key: &str, value: &str) -> Result<usize, rusqlite::Error> {
    conn.execute(
        "INSERT INTO settings (key, value) VALUES (?1, ?2)
         ON CONFLICT(key) DO UPDATE SET value = excluded.value",
        (key, value),
    )
}

/// 加载所有设置项。
pub fn load_all(conn: &Connection) -> Result<std::collections::HashMap<String, String>, rusqlite::Error> {
    let mut stmt = conn.prepare("SELECT key, value FROM settings")?;
    let rows = stmt.query_map([], |row| {
        Ok((row.get::<_, String>(0)?, row.get::<_, String>(1)?))
    })?;
    rows.collect()
}
