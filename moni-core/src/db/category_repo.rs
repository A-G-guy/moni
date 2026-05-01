use rusqlite::{Connection, OptionalExtension, Row};

use moni_contracts::category::Category;
use moni_contracts::record::RecordType;
use moni_contracts::types::CategoryId;

fn map_category(row: &Row) -> Result<Category, rusqlite::Error> {
    Ok(Category {
        id: row.get("id")?,
        name: row.get("name")?,
        category_type: match row.get::<_, String>("category_type")?.as_str() {
            "income" => RecordType::Income,
            _ => RecordType::Expense,
        },
        icon_name: row.get("icon_name")?,
        color_hex: row.get("color_hex")?,
        sort_order: row.get("sort_order")?,
        is_preset: row.get::<_, i32>("is_preset")? != 0,
        created_at: row.get("created_at")?,
        updated_at: row.get("updated_at")?,
    })
}

/// 插入新分类。
pub fn insert(
    conn: &Connection,
    name: &str,
    category_type: RecordType,
    icon_name: &str,
    color_hex: &str,
    sort_order: i32,
    is_preset: bool,
) -> Result<CategoryId, rusqlite::Error> {
    let now = chrono::Utc::now().timestamp();
    conn.execute(
        "INSERT INTO categories (name, category_type, icon_name, color_hex, sort_order, is_preset, created_at, updated_at)
         VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8)",
        (
            name,
            match category_type {
                RecordType::Income => "income",
                RecordType::Expense => "expense",
            },
            icon_name,
            color_hex,
            sort_order,
            if is_preset { 1 } else { 0 },
            now,
            now,
        ),
    )?;
    Ok(conn.last_insert_rowid())
}

/// 根据 ID 查询分类。
pub fn get_by_id(conn: &Connection, id: CategoryId) -> Result<Option<Category>, rusqlite::Error> {
    conn.query_row(
        "SELECT * FROM categories WHERE id = ?1",
        [id],
        map_category,
    )
    .optional()
}

/// 查询所有分类，按 sort_order 排序。
pub fn list_all(conn: &Connection) -> Result<Vec<Category>, rusqlite::Error> {
    let mut stmt = conn.prepare(
        "SELECT * FROM categories ORDER BY sort_order ASC, created_at ASC"
    )?;
    let rows = stmt.query_map([], map_category)?;
    rows.collect()
}

/// 更新分类（仅允许更新自定义分类的 name/icon/color）。
pub fn update(
    conn: &Connection,
    id: CategoryId,
    name: Option<&str>,
    icon_name: Option<&str>,
    color_hex: Option<&str>,
) -> Result<usize, rusqlite::Error> {
    let now = chrono::Utc::now().timestamp();
    conn.execute(
        "UPDATE categories SET
            name = COALESCE(?2, name),
            icon_name = COALESCE(?3, icon_name),
            color_hex = COALESCE(?4, color_hex),
            updated_at = ?5
         WHERE id = ?1 AND is_preset = 0",
        (id, name, icon_name, color_hex, now),
    )
}

/// 删除分类（仅自定义分类可删除）。
pub fn delete(conn: &Connection, id: CategoryId) -> Result<usize, rusqlite::Error> {
    conn.execute(
        "DELETE FROM categories WHERE id = ?1 AND is_preset = 0",
        [id],
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
    let count: i64 = conn.query_row(
        "SELECT COUNT(*) FROM categories",
        [],
        |row| row.get(0),
    )?;
    if count > 0 {
        return Ok(());
    }

    let presets = [
        ("餐饮", RecordType::Expense, "restaurant", "#FF6B6B", 1),
        ("交通", RecordType::Expense, "directions_car", "#4ECDC4", 2),
        ("购物", RecordType::Expense, "shopping_bag", "#45B7D1", 3),
        ("娱乐", RecordType::Expense, "sports_esports", "#96CEB4", 4),
        ("居住", RecordType::Expense, "home", "#FFEAA7", 5),
        ("医疗", RecordType::Expense, "local_hospital", "#DDA0DD", 6),
        ("教育", RecordType::Expense, "school", "#98D8C8", 7),
        ("其他支出", RecordType::Expense, "more_horiz", "#B2BEC3", 8),
        ("工资", RecordType::Income, "payments", "#00B894", 9),
        ("奖金", RecordType::Income, "card_giftcard", "#00CEC9", 10),
        ("投资", RecordType::Income, "trending_up", "#FDCB6E", 11),
        ("其他收入", RecordType::Income, "more_horiz", "#B2BEC3", 12),
    ];

    for (name, ty, icon, color, order) in presets {
        insert(conn, name, ty, icon, color, order, true)?;
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::db::connection::open_in_memory;
    use crate::db::schema::init_schema;

    fn setup() -> Connection {
        let conn = open_in_memory().unwrap();
        init_schema(&conn).unwrap();
        conn
    }

    #[test]
    fn test_insert_and_get() {
        let conn = setup();
        let id = insert(&conn, "测试分类", RecordType::Expense, "test", "#000000", 1, false).unwrap();
        assert!(id > 0);

        let cat = get_by_id(&conn, id).unwrap().unwrap();
        assert_eq!(cat.name, "测试分类");
        assert_eq!(cat.category_type, RecordType::Expense);
        assert!(!cat.is_preset);
    }

    #[test]
    fn test_list_all_ordering() {
        let conn = setup();
        insert(&conn, "分类B", RecordType::Expense, "b", "#000", 2, false).unwrap();
        insert(&conn, "分类A", RecordType::Expense, "a", "#000", 1, false).unwrap();

        let list = list_all(&conn).unwrap();
        assert_eq!(list.len(), 2);
        assert_eq!(list[0].name, "分类A");
        assert_eq!(list[1].name, "分类B");
    }

    #[test]
    fn test_seed_presets() {
        let conn = setup();
        seed_presets(&conn).unwrap();
        let list = list_all(&conn).unwrap();
        assert_eq!(list.len(), crate::shared::constants::PRESET_CATEGORY_COUNT);
        assert!(list.iter().all(|c| c.is_preset));
    }

    #[test]
    fn test_seed_presets_idempotent() {
        let conn = setup();
        seed_presets(&conn).unwrap();
        seed_presets(&conn).unwrap();
        let list = list_all(&conn).unwrap();
        assert_eq!(list.len(), crate::shared::constants::PRESET_CATEGORY_COUNT);
    }

    #[test]
    fn test_delete_preset_fails() {
        let conn = setup();
        seed_presets(&conn).unwrap();
        let presets = list_all(&conn).unwrap();
        let affected = delete(&conn, presets[0].id).unwrap();
        assert_eq!(affected, 0);
    }

    #[test]
    fn test_delete_custom_ok() {
        let conn = setup();
        let id = insert(&conn, "自定义", RecordType::Expense, "x", "#000", 1, false).unwrap();
        let affected = delete(&conn, id).unwrap();
        assert_eq!(affected, 1);
        assert!(get_by_id(&conn, id).unwrap().is_none());
    }

    #[test]
    fn test_is_in_use() {
        let conn = setup();
        let cat_id = insert(&conn, "分类", RecordType::Expense, "x", "#000", 1, false).unwrap();
        assert!(!is_in_use(&conn, cat_id).unwrap());

        conn.execute(
            "INSERT INTO records (amount_cents, record_type, category_id, note, created_at, updated_at)
             VALUES (100, 'expense', ?1, '', 0, 0)",
            [cat_id],
        ).unwrap();
        assert!(is_in_use(&conn, cat_id).unwrap());
    }
}
