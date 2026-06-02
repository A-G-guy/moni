use rusqlite::Connection;

/// 当前数据库 schema 版本号。
/// 每次 schema 发生非向后兼容的变更时同步递增。
pub const CURRENT_SCHEMA_VERSION: u32 = 8;

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
    parent_category_id INTEGER NULL REFERENCES categories(id) ON DELETE RESTRICT,
    note TEXT NOT NULL DEFAULT '',
    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    year_month TEXT NULL
);

CREATE INDEX IF NOT EXISTS idx_records_created_at ON records(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_records_category ON records(category_id);
CREATE INDEX IF NOT EXISTS idx_records_note ON records(note);
CREATE INDEX IF NOT EXISTS idx_records_type_date ON records(record_type, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_records_amount ON records(amount_cents);

CREATE TABLE IF NOT EXISTS budgets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    category_id INTEGER NULL REFERENCES categories(id) ON DELETE CASCADE,
    amount_cents INTEGER NOT NULL CHECK(amount_cents > 0),
    year_month TEXT NULL,
    period_type TEXT NOT NULL DEFAULT 'monthly' CHECK(period_type IN ('monthly')),
    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    UNIQUE(category_id, year_month)
);

CREATE INDEX IF NOT EXISTS idx_budgets_category ON budgets(category_id);
CREATE INDEX IF NOT EXISTS idx_budgets_category_ym ON budgets(category_id, year_month);

CREATE TABLE IF NOT EXISTS settings (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS chat_messages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id TEXT NOT NULL,
    message_type TEXT NOT NULL CHECK(message_type IN ('user_text', 'ai_card', 'ai_text', 'system')),
    content TEXT NOT NULL DEFAULT '',
    card_data_json TEXT NULL,
    card_status TEXT NULL CHECK(card_status IN ('draft', 'saved', 'expired')),
    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
);

CREATE INDEX IF NOT EXISTS idx_chat_messages_session_created ON chat_messages(session_id, created_at DESC);
";

/// 执行数据库 Schema 初始化与幂等迁移。
pub fn init_schema(conn: &Connection) -> Result<(), rusqlite::Error> {
    conn.execute_batch(SCHEMA_SQL)?;

    // v5 迁移：数据完整性加固（预算唯一性 + records year_month 持久化）
    let schema_version: i32 = conn.query_row("PRAGMA user_version", [], |row| row.get(0))?;
    if schema_version < CURRENT_SCHEMA_VERSION as i32 {
        // 对旧版本数据库执行列迁移（新数据库已在 SCHEMA_SQL 中包含所有列）
        migrate_old_columns(conn)?;
        if schema_version < 5 {
            init_schema_v5(conn)?;
        }
        if schema_version < 6 {
            init_schema_v6(conn)?;
        }
        if schema_version < 7 {
            init_schema_v7(conn)?;
        }
        if schema_version < 8 {
            init_schema_v8(conn)?;
        }
        conn.execute(
            &format!("PRAGMA user_version = {}", CURRENT_SCHEMA_VERSION),
            [],
        )?;
    }

    Ok(())
}

/// v5 迁移：
/// 1. 清理 budgets 表重复数据，保留 id 最大（即最新）的一条；
/// 2. 创建 COALESCE 唯一索引覆盖 NULL 值场景；
/// 3. 为 records 添加 year_month 列并回填历史数据；
/// 4. 创建 records 相关复合索引。
/// 迁移旧版本数据库缺失的列（仅在 schema_version < CURRENT_SCHEMA_VERSION 时执行）。
fn migrate_old_columns(conn: &Connection) -> Result<(), rusqlite::Error> {
    // description（2026-05-04 迁移）
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

    // archived_at（2026-05-04 迁移）
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

    // parent_id（2026-05-05 迁移）
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

    // records.year_month（2026-05-07 迁移：账单年月持久化）
    let has_records_year_month: i64 = conn.query_row(
        "SELECT COUNT(*) FROM pragma_table_info('records') WHERE name = 'year_month'",
        [],
        |row| row.get(0),
    )?;
    if has_records_year_month == 0 {
        conn.execute("ALTER TABLE records ADD COLUMN year_month TEXT NULL", [])?;
    }

    // budgets.year_month（2026-05-07 迁移：预算月度快照）
    let has_year_month: i64 = conn.query_row(
        "SELECT COUNT(*) FROM pragma_table_info('budgets') WHERE name = 'year_month'",
        [],
        |row| row.get(0),
    )?;
    if has_year_month == 0 {
        conn.execute("ALTER TABLE budgets ADD COLUMN year_month TEXT NULL", [])?;
    }

    // records.parent_category_id（2026-05-07 迁移：固化账单父级关系）
    let has_parent_category_id: i64 = conn.query_row(
        "SELECT COUNT(*) FROM pragma_table_info('records') WHERE name = 'parent_category_id'",
        [],
        |row| row.get(0),
    )?;
    if has_parent_category_id == 0 {
        conn.execute(
            "ALTER TABLE records ADD COLUMN parent_category_id INTEGER NULL REFERENCES categories(id) ON DELETE RESTRICT",
            [],
        )?;
        conn.execute(
            "UPDATE records SET parent_category_id = (SELECT parent_id FROM categories WHERE id = records.category_id) WHERE parent_category_id IS NULL",
            [],
        )?;
    }

    Ok(())
}

fn init_schema_v5(conn: &Connection) -> Result<(), rusqlite::Error> {
    // 1. 清理 budgets 重复数据（保留 id 最大的一条，id 在单线程写入场景下代表时间顺序）
    conn.execute(
        "DELETE FROM budgets
         WHERE id NOT IN (
             SELECT MAX(id)
             FROM budgets
             GROUP BY COALESCE(category_id, -1), COALESCE(year_month, '')
         )",
        [],
    )?;

    // 2. 创建唯一索引（覆盖已有表缺少 UNIQUE 约束、且 SQLite NULL 不被视为相等的情况）
    conn.execute(
        "CREATE UNIQUE INDEX IF NOT EXISTS idx_budgets_unique
         ON budgets(COALESCE(category_id, -1), COALESCE(year_month, ''))",
        [],
    )?;

    // 3. 回填 records 历史数据的 year_month（基于本地时区）
    // 注：year_month 列在 SCHEMA_SQL 中已包含，旧版本数据库通过 migrate_old_columns 添加
    conn.execute(
        "UPDATE records
         SET year_month = strftime('%Y-%m', datetime(created_at, 'unixepoch', 'localtime'))
         WHERE year_month IS NULL",
        [],
    )?;

    // 4. 创建 records 相关复合索引
    conn.execute(
        "CREATE INDEX IF NOT EXISTS idx_records_year_month ON records(year_month, record_type, category_id)",
        [],
    )?;
    conn.execute(
        "CREATE INDEX IF NOT EXISTS idx_records_parent_ym ON records(year_month, record_type, parent_category_id)",
        [],
    )?;

    // 5. 一致性校验：验证回填后所有 records 的 parent_category_id 均已赋值
    let null_parent_count: i64 = conn.query_row(
        "SELECT COUNT(*) FROM records WHERE parent_category_id IS NULL",
        [],
        |row| row.get(0),
    )?;
    if null_parent_count > 0 {
        log::warn!(
            "V5 迁移后仍有 {} 条记录的 parent_category_id 为 NULL",
            null_parent_count
        );
    }

    Ok(())
}

/// v6 迁移：
/// 1. 清理 budgets 表中 category_id IS NULL 的重复快照与模板；
/// 2. 使用本地时区重新回填所有 records.year_month。
fn init_schema_v6(conn: &Connection) -> Result<(), rusqlite::Error> {
    // 1. 清理总预算快照重复数据（保留 id 最大的一条）
    conn.execute(
        "DELETE FROM budgets
         WHERE id NOT IN (
             SELECT MAX(id)
             FROM budgets
             WHERE category_id IS NULL AND year_month IS NOT NULL
             GROUP BY year_month
         )
         AND category_id IS NULL AND year_month IS NOT NULL",
        [],
    )?;

    // 2. 清理总预算模板重复数据（保留 id 最大的一条）
    conn.execute(
        "DELETE FROM budgets
         WHERE id NOT IN (
             SELECT MAX(id)
             FROM budgets
             WHERE category_id IS NULL AND year_month IS NULL
         )
         AND category_id IS NULL AND year_month IS NULL",
        [],
    )?;

    // 3. 使用本地时区重新计算所有 records 的 year_month
    conn.execute(
        "UPDATE records
         SET year_month = strftime('%Y-%m', datetime(created_at, 'unixepoch', 'localtime'))
         WHERE year_month IS NOT NULL",
        [],
    )?;

    Ok(())
}

/// v7 迁移：
/// 为搜索功能创建新索引。
fn init_schema_v7(conn: &Connection) -> Result<(), rusqlite::Error> {
    conn.execute(
        "CREATE INDEX IF NOT EXISTS idx_records_note ON records(note)",
        [],
    )?;
    conn.execute(
        "CREATE INDEX IF NOT EXISTS idx_records_type_date ON records(record_type, created_at DESC)",
        [],
    )?;
    conn.execute(
        "CREATE INDEX IF NOT EXISTS idx_records_amount ON records(amount_cents)",
        [],
    )?;
    Ok(())
}

/// v8 迁移：
/// 创建 chat_messages 表，用于存储 AI 对话消息记录。
fn init_schema_v8(conn: &Connection) -> Result<(), rusqlite::Error> {
    conn.execute_batch(
        "CREATE TABLE IF NOT EXISTS chat_messages (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            session_id TEXT NOT NULL,
            message_type TEXT NOT NULL CHECK(message_type IN ('user_text', 'ai_card', 'ai_text', 'system')),
            content TEXT NOT NULL DEFAULT '',
            card_data_json TEXT NULL,
            card_status TEXT NULL CHECK(card_status IN ('draft', 'saved', 'expired')),
            created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
        );

        CREATE INDEX IF NOT EXISTS idx_chat_messages_session_created
            ON chat_messages(session_id, created_at DESC);",
    )?;
    Ok(())
}
