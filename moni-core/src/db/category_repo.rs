use rusqlite::{Connection, OptionalExtension, Row};

use moni_contracts::category::Category;
use moni_contracts::record::RecordType;
use moni_contracts::types::CategoryId;

fn map_category(row: &Row) -> Result<Category, rusqlite::Error> {
    Ok(Category {
        id: row.get("id")?,
        name: row.get("name")?,
        description: row.get("description")?,
        category_type: match row.get::<_, String>("category_type")?.as_str() {
            "income" => RecordType::Income,
            _ => RecordType::Expense,
        },
        icon_name: row.get("icon_name")?,
        sort_order: row.get("sort_order")?,
        is_preset: row.get::<_, i32>("is_preset")? != 0,
        archived_at: row.get("archived_at")?,
        created_at: row.get("created_at")?,
        updated_at: row.get("updated_at")?,
    })
}

/// 插入新分类。
pub fn insert(
    conn: &Connection,
    name: &str,
    description: Option<&str>,
    category_type: RecordType,
    icon_name: &str,
    sort_order: i32,
    is_preset: bool,
) -> Result<CategoryId, rusqlite::Error> {
    let now = chrono::Utc::now().timestamp();
    conn.execute(
        "INSERT INTO categories (name, description, category_type, icon_name, sort_order, is_preset, created_at, updated_at)
         VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8)",
        (
            name,
            description,
            match category_type {
                RecordType::Income => "income",
                RecordType::Expense => "expense",
            },
            icon_name,
            sort_order,
            i32::from(is_preset),
            now,
            now,
        ),
    )?;
    Ok(conn.last_insert_rowid())
}

/// 根据 ID 查询分类。
pub fn get_by_id(conn: &Connection, id: CategoryId) -> Result<Option<Category>, rusqlite::Error> {
    conn.query_row("SELECT * FROM categories WHERE id = ?1", [id], map_category)
        .optional()
}

/// 查询所有分类（含归档项），按 `sort_order` 排序。
pub fn list_all(conn: &Connection) -> Result<Vec<Category>, rusqlite::Error> {
    let mut stmt =
        conn.prepare("SELECT * FROM categories ORDER BY sort_order ASC, created_at ASC")?;
    let rows = stmt.query_map([], map_category)?;
    rows.collect()
}

/// 仅查询活跃分类（未归档），按 `sort_order` 排序。
pub fn list_active(conn: &Connection) -> Result<Vec<Category>, rusqlite::Error> {
    let mut stmt = conn.prepare(
        "SELECT * FROM categories WHERE archived_at IS NULL ORDER BY sort_order ASC, created_at ASC"
    )?;
    let rows = stmt.query_map([], map_category)?;
    rows.collect()
}

/// 更新分类（仅允许更新自定义分类的 name / description / `icon_name`）。
pub fn update(
    conn: &Connection,
    id: CategoryId,
    name: Option<&str>,
    description: Option<&str>,
    icon_name: Option<&str>,
) -> Result<usize, rusqlite::Error> {
    let now = chrono::Utc::now().timestamp();
    conn.execute(
        "UPDATE categories SET
            name = COALESCE(?2, name),
            description = COALESCE(?3, description),
            icon_name = COALESCE(?4, icon_name),
            updated_at = ?5
         WHERE id = ?1",
        (id, name, description, icon_name, now),
    )
}

/// 归档分类（设置 `archived_at` 为当前时间戳）。
pub fn archive(conn: &Connection, id: CategoryId) -> Result<usize, rusqlite::Error> {
    let now = chrono::Utc::now().timestamp();
    conn.execute(
        "UPDATE categories SET archived_at = ?2, updated_at = ?3 WHERE id = ?1",
        (id, now, now),
    )
}

/// 取消归档分类（将 `archived_at` 设为 NULL）。
pub fn unarchive(conn: &Connection, id: CategoryId) -> Result<usize, rusqlite::Error> {
    let now = chrono::Utc::now().timestamp();
    conn.execute(
        "UPDATE categories SET archived_at = NULL, updated_at = ?2 WHERE id = ?1",
        (id, now),
    )
}

/// 检查分类是否被记录引用。
pub fn is_in_use(conn: &Connection, id: CategoryId) -> Result<bool, rusqlite::Error> {
    let count: i64 = conn.query_row(
        "SELECT COUNT(*) FROM records WHERE category_id = ?1",
        [id],
        |row| row.get(0),
    )?;
    Ok(count > 0)
}

/// 插入预设分类（仅在表为空时执行）。
pub fn seed_presets(conn: &Connection) -> Result<(), rusqlite::Error> {
    let count: i64 = conn.query_row("SELECT COUNT(*) FROM categories", [], |row| row.get(0))?;
    if count > 0 {
        return Ok(());
    }

    let presets = [
        ("餐饮", RecordType::Expense, "restaurant", 1),
        ("交通", RecordType::Expense, "directions_car", 2),
        ("购物", RecordType::Expense, "shopping_bag", 3),
        ("娱乐", RecordType::Expense, "sports_esports", 4),
        ("居住", RecordType::Expense, "home", 5),
        ("医疗", RecordType::Expense, "local_hospital", 6),
        ("教育", RecordType::Expense, "school", 7),
        ("其他支出", RecordType::Expense, "more_horiz", 8),
        ("工资", RecordType::Income, "payments", 9),
        ("奖金", RecordType::Income, "redeem", 10),
        ("投资", RecordType::Income, "trending_up", 11),
        ("其他收入", RecordType::Income, "more_horiz", 12),
    ];

    for (name, ty, icon, order) in presets {
        insert(conn, name, None, ty, icon, order, true)?;
    }
    Ok(())
}
