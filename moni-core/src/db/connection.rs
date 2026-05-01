use rusqlite::Connection;

/// 打开指定路径的 SQLite 数据库连接。
pub fn open_connection(db_path: &str) -> Result<Connection, rusqlite::Error> {
    Connection::open(db_path)
}

/// 打开内存中的 SQLite 连接（用于测试）。
pub fn open_in_memory() -> Result<Connection, rusqlite::Error> {
    Connection::open_in_memory()
}
