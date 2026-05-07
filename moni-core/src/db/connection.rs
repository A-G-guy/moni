use rusqlite::Connection;

/// 打开指定路径的 `SQLite` 数据库连接。
/// 自动启用外键约束与 WAL 模式。
pub fn open_connection(db_path: &str) -> Result<Connection, rusqlite::Error> {
    let conn = Connection::open(db_path)?;
    conn.execute_batch("PRAGMA foreign_keys = ON; PRAGMA journal_mode = WAL;")?;
    Ok(conn)
}

/// 打开内存中的 `SQLite` 连接（用于测试）。
/// 自动启用外键约束。
pub fn open_in_memory() -> Result<Connection, rusqlite::Error> {
    let conn = Connection::open_in_memory()?;
    conn.execute_batch("PRAGMA foreign_keys = ON;")?;
    Ok(conn)
}
