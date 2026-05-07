use rusqlite::{Connection, OptionalExtension, Row};

use moni_contracts::budget::{Budget, BudgetPeriodType};
use moni_contracts::types::{AmountCents, BudgetId, CategoryId};

fn map_budget(row: &Row) -> Result<Budget, rusqlite::Error> {
    let period_type_str: String = row.get("period_type")?;
    let period_type = match period_type_str.as_str() {
        "monthly" => BudgetPeriodType::Monthly,
        other => {
            log::warn!("数据库中存在未知的预算周期类型: {other}，id={}", row.get::<_, i64>("id")?);
            return Err(rusqlite::Error::InvalidColumnType(
                0,
                "period_type".to_string(),
                rusqlite::types::Type::Text,
            ));
        }
    };
    Ok(Budget {
        id: row.get("id")?,
        category_id: row.get("category_id")?,
        amount_cents: row.get("amount_cents")?,
        period_type,
        created_at: row.get("created_at")?,
        updated_at: row.get("updated_at")?,
    })
}

/// 插入或更新预算。
/// 同一 category_id 只能有一条预算记录，已存在则更新金额。
pub fn upsert(
    conn: &Connection,
    category_id: Option<CategoryId>,
    amount_cents: AmountCents,
) -> Result<BudgetId, rusqlite::Error> {
    let now = chrono::Utc::now().timestamp();

    // 先检查是否已存在
    let existing: Option<BudgetId> = conn.query_row(
        "SELECT id FROM budgets WHERE category_id IS ?1",
        [category_id],
        |row| row.get(0),
    ).optional()?;

    if let Some(id) = existing {
        // 更新
        conn.execute(
            "UPDATE budgets SET amount_cents = ?2, updated_at = ?3 WHERE id = ?1",
            (id, amount_cents, now),
        )?;
        Ok(id)
    } else {
        // 插入
        conn.execute(
            "INSERT INTO budgets (category_id, amount_cents, period_type, created_at, updated_at)
             VALUES (?1, ?2, ?3, ?4, ?5)",
            (category_id, amount_cents, "monthly", now, now),
        )?;
        Ok(conn.last_insert_rowid())
    }
}

/// 根据 ID 查询预算。
pub fn get_by_id(conn: &Connection, id: BudgetId) -> Result<Option<Budget>, rusqlite::Error> {
    conn.query_row("SELECT * FROM budgets WHERE id = ?1", [id], map_budget)
        .optional()
}

/// 根据 category_id 查询预算（含 NULL = 总预算）。
pub fn get_by_category_id(
    conn: &Connection,
    category_id: Option<CategoryId>,
) -> Result<Option<Budget>, rusqlite::Error> {
    conn.query_row(
        "SELECT * FROM budgets WHERE category_id IS ?1",
        [category_id],
        map_budget,
    )
    .optional()
}

/// 查询所有预算。
pub fn list_all(conn: &Connection) -> Result<Vec<Budget>, rusqlite::Error> {
    let mut stmt = conn.prepare("SELECT * FROM budgets ORDER BY category_id NULLS FIRST, id")?;
    let rows = stmt.query_map([], map_budget)?;
    rows.collect()
}

/// 删除预算。
pub fn delete(conn: &Connection, id: BudgetId) -> Result<usize, rusqlite::Error> {
    conn.execute("DELETE FROM budgets WHERE id = ?1", [id])
}
