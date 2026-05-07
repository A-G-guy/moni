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
            "expense" => RecordType::Expense,
            other => {
                log::warn!("数据库中存在未知的分类类型: {other}, id={}", row.get::<_, i64>("id")?);
                return Err(rusqlite::Error::InvalidColumnType(
                    0,
                    "category_type".to_string(),
                    rusqlite::types::Type::Text,
                ));
            }
        },
        icon_name: row.get("icon_name")?,
        sort_order: row.get("sort_order")?,
        parent_id: row.get("parent_id")?,
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
    parent_id: Option<CategoryId>,
) -> Result<CategoryId, rusqlite::Error> {
    let now = chrono::Utc::now().timestamp();
    conn.execute(
        "INSERT INTO categories (name, description, category_type, icon_name, sort_order, parent_id, created_at, updated_at)
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
            parent_id,
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

/// 查询所有分类（含归档项），一级分类在前，子分类紧跟父分类。
pub fn list_all(conn: &Connection) -> Result<Vec<Category>, rusqlite::Error> {
    let mut stmt = conn.prepare(
        "SELECT * FROM categories ORDER BY CASE WHEN parent_id IS NULL THEN 0 ELSE 1 END, parent_id ASC, sort_order ASC, created_at ASC"
    )?;
    let rows = stmt.query_map([], map_category)?;
    rows.collect()
}

/// 仅查询活跃分类（未归档），一级分类在前，子分类紧跟父分类。
pub fn list_active(conn: &Connection) -> Result<Vec<Category>, rusqlite::Error> {
    let mut stmt = conn.prepare(
        "SELECT * FROM categories WHERE archived_at IS NULL ORDER BY CASE WHEN parent_id IS NULL THEN 0 ELSE 1 END, parent_id ASC, sort_order ASC, created_at ASC"
    )?;
    let rows = stmt.query_map([], map_category)?;
    rows.collect()
}

/// 更新分类（允许更新 name / description / icon_name / parent_id）。
pub fn update(
    conn: &Connection,
    id: CategoryId,
    name: Option<&str>,
    description: Option<&str>,
    icon_name: Option<&str>,
    parent_id: Option<Option<CategoryId>>,
) -> Result<usize, rusqlite::Error> {
    let now = chrono::Utc::now().timestamp();
    match parent_id {
        Some(pid) => conn.execute(
            "UPDATE categories SET
                name = COALESCE(?2, name),
                description = COALESCE(?3, description),
                icon_name = COALESCE(?4, icon_name),
                parent_id = ?5,
                updated_at = ?6
             WHERE id = ?1",
            (id, name, description, icon_name, pid, now),
        ),
        None => conn.execute(
            "UPDATE categories SET
                name = COALESCE(?2, name),
                description = COALESCE(?3, description),
                icon_name = COALESCE(?4, icon_name),
                updated_at = ?5
             WHERE id = ?1",
            (id, name, description, icon_name, now),
        ),
    }
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

/// 查询指定父分类下的所有子分类。
pub fn list_by_parent(
    conn: &Connection,
    parent_id: CategoryId,
) -> Result<Vec<Category>, rusqlite::Error> {
    let mut stmt = conn.prepare(
        "SELECT * FROM categories WHERE parent_id = ?1 ORDER BY sort_order ASC, created_at ASC"
    )?;
    let rows = stmt.query_map([parent_id], map_category)?;
    rows.collect()
}

/// 检查分类是否有子分类。
pub fn has_children(conn: &Connection, id: CategoryId) -> Result<bool, rusqlite::Error> {
    let count: i64 = conn.query_row(
        "SELECT COUNT(*) FROM categories WHERE parent_id = ?1",
        [id],
        |row| row.get(0),
    )?;
    Ok(count > 0)
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

/// 按指定顺序批量更新分类的 sort_order。
/// ordered_ids 中的顺序即为新的排序顺序，sort_order 按 (index + 1) * 10 分配。
pub fn reorder(conn: &mut Connection, ordered_ids: &[CategoryId]) -> Result<(), rusqlite::Error> {
    let tx = conn.transaction()?;
    let now = chrono::Utc::now().timestamp();
    for (index, &id) in ordered_ids.iter().enumerate() {
        let sort_order = (index as i32 + 1) * 10;
        tx.execute(
            "UPDATE categories SET sort_order = ?1, updated_at = ?2 WHERE id = ?3",
            (sort_order, now, id),
        )?;
    }
    tx.commit()?;
    Ok(())
}

/// 插入预设分类（仅在表为空时执行）。
pub fn seed_presets(conn: &Connection) -> Result<(), rusqlite::Error> {
    let count: i64 = conn.query_row("SELECT COUNT(*) FROM categories", [], |row| row.get(0))?;
    if count > 0 {
        return Ok(());
    }

    // 一级分类
    let parents = [
        ("餐饮", RecordType::Expense, "restaurant", 1, None::<CategoryId>),
        ("交通", RecordType::Expense, "directions_car", 2, None),
        ("购物", RecordType::Expense, "shopping_bag", 3, None),
        ("娱乐", RecordType::Expense, "sports_esports", 4, None),
        ("居住", RecordType::Expense, "home", 5, None),
        ("医疗", RecordType::Expense, "local_hospital", 6, None),
        ("教育", RecordType::Expense, "school", 7, None),
        ("其他支出", RecordType::Expense, "more_horiz", 8, None),
        ("工资", RecordType::Income, "payments", 9, None),
        ("奖金", RecordType::Income, "redeem", 10, None),
        ("投资", RecordType::Income, "trending_up", 11, None),
        ("其他收入", RecordType::Income, "more_horiz", 12, None),
    ];

    let mut parent_ids: Vec<CategoryId> = Vec::with_capacity(parents.len());
    for (name, ty, icon, order, _) in &parents {
        let id = insert(conn, name, None, *ty, icon, *order, None)?;
        parent_ids.push(id);
    }

    // 二级分类示例
    let children = [
        ("早餐", RecordType::Expense, "bakery_dining", 101, Some(parent_ids[0])),
        ("外卖", RecordType::Expense, "delivery_dining", 102, Some(parent_ids[0])),
        ("地铁", RecordType::Expense, "subway", 103, Some(parent_ids[1])),
        ("打车", RecordType::Expense, "local_taxi", 104, Some(parent_ids[1])),
        ("网购", RecordType::Expense, "shopping_cart", 105, Some(parent_ids[2])),
        ("超市", RecordType::Expense, "storefront", 106, Some(parent_ids[2])),
    ];

    for (name, ty, icon, order, pid) in &children {
        insert(conn, name, None, *ty, icon, *order, *pid)?;
    }

    Ok(())
}
