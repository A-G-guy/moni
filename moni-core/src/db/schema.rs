use rusqlite::Connection;

/// 当前数据库 schema 版本号。
/// 每次 schema 发生非向后兼容的变更时同步递增。
pub const CURRENT_SCHEMA_VERSION: u32 = 3;

const SCHEMA_SQL: &str = "
CREATE TABLE IF NOT EXISTS categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    category_type TEXT NOT NULL CHECK(category_type IN ('income', 'expense')),
    icon_name TEXT NOT NULL DEFAULT 'help',
    color_hex TEXT NOT NULL DEFAULT '#808080',
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_preset INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
);

CREATE TABLE IF NOT EXISTS records (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    amount_cents INTEGER NOT NULL CHECK(amount_cents > 0),
    record_type TEXT NOT NULL CHECK(record_type IN ('income', 'expense')),
    category_id INTEGER NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    note TEXT NOT NULL DEFAULT '',
    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
);

CREATE INDEX IF NOT EXISTS idx_records_created_at ON records(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_records_category ON records(category_id);

CREATE TABLE IF NOT EXISTS budgets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    category_id INTEGER NULL REFERENCES categories(id) ON DELETE CASCADE,
    amount_cents INTEGER NOT NULL CHECK(amount_cents > 0),
    year_month TEXT NULL,
    period_type TEXT NOT NULL DEFAULT 'monthly' CHECK(period_type IN ('monthly')),
    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
);

CREATE INDEX IF NOT EXISTS idx_budgets_category ON budgets(category_id);
";

/// 执行数据库 Schema 初始化与幂等迁移。
pub fn init_schema(conn: &Connection) -> Result<(), rusqlite::Error> {
    conn.execute_batch(SCHEMA_SQL)?;

    // 检查并添加 description 列（2026-05-04 迁移）
    let has_description: i64 = conn.query_row(
        "SELECT COUNT(*) FROM pragma_table_info('categories') WHERE name = 'description'",
        [],
        |row| row.get(0),
    )?;
    if has_description == 0 {
        conn.execute(
            "ALTER TABLE categories ADD COLUMN description TEXT NULL",
            [],
        )?;
    }

    // 检查并添加 archived_at 列（2026-05-04 迁移）
    let has_archived_at: i64 = conn.query_row(
        "SELECT COUNT(*) FROM pragma_table_info('categories') WHERE name = 'archived_at'",
        [],
        |row| row.get(0),
    )?;
    if has_archived_at == 0 {
        conn.execute(
            "ALTER TABLE categories ADD COLUMN archived_at INTEGER NULL",
            [],
        )?;
    }

    // 检查并添加 parent_id 列（2026-05-05 迁移）
    let has_parent_id: i64 = conn.query_row(
        "SELECT COUNT(*) FROM pragma_table_info('categories') WHERE name = 'parent_id'",
        [],
        |row| row.get(0),
    )?;
    if has_parent_id == 0 {
        conn.execute(
            "ALTER TABLE categories ADD COLUMN parent_id INTEGER NULL REFERENCES categories(id) ON DELETE RESTRICT",
            [],
        )?;
    }

    // 检查并添加 year_month 列（2026-05-07 迁移：预算月度快照）
    let has_year_month: i64 = conn.query_row(
        "SELECT COUNT(*) FROM pragma_table_info('budgets') WHERE name = 'year_month'",
        [],
        |row| row.get(0),
    )?;
    if has_year_month == 0 {
        conn.execute(
            "ALTER TABLE budgets ADD COLUMN year_month TEXT NULL",
            [],
        )?;
    }

    Ok(())
}
