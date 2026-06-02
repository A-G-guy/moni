use rusqlite::{Connection, OptionalExtension, Row};

use moni_contracts::budget::{Budget, BudgetPeriodType};
use moni_contracts::types::{AmountCents, BudgetId, CategoryId};

fn map_budget(row: &Row) -> Result<Budget, rusqlite::Error> {
    let period_type_str: String = row.get("period_type")?;
    let period_type = match period_type_str.as_str() {
        "monthly" => BudgetPeriodType::Monthly,
        other => {
            log::warn!(
                "数据库中存在未知的预算周期类型: {other}，id={}",
                row.get::<_, i64>("id")?
            );
            return Err(rusqlite::Error::InvalidColumnType(
                0,
                "period_type".to_string(),
                rusqlite::types::Type::Text,
            ));
        }
    };
    let year_month: Option<String> = row.get("year_month")?;
    Ok(Budget {
        id: row.get("id")?,
        category_id: row.get("category_id")?,
        amount_cents: row.get("amount_cents")?,
        year_month,
        period_type,
        created_at: row.get("created_at")?,
        updated_at: row.get("updated_at")?,
    })
}

/// 插入或更新预算。
///
/// `year_month` 为 `None` 时操作模板，为 `Some("YYYY-MM")` 时操作月度快照。
/// SQLite UNIQUE 对 NULL 不视为相等，且 ON CONFLICT 无法引用 COALESCE 表达式索引，
/// 因此快照和模板均采用 SELECT-then-UPDATE/INSERT 模式，idx_budgets_unique 索引在异常路径下阻止重复。
pub fn upsert(
    conn: &Connection,
    category_id: Option<CategoryId>,
    year_month: Option<&str>,
    amount_cents: AmountCents,
) -> Result<BudgetId, rusqlite::Error> {
    let now = chrono::Utc::now().timestamp();

    // 快照（year_month 非 NULL）：SQLite UNIQUE 对 NULL 不视为相等，需手动处理
    if let Some(ym) = year_month {
        let existing: Option<BudgetId> = conn
            .query_row(
                "SELECT id FROM budgets WHERE category_id IS ?1 AND year_month = ?2",
                (category_id, ym),
                |row| row.get(0),
            )
            .optional()?;
        if let Some(id) = existing {
            conn.execute(
                "UPDATE budgets SET amount_cents = ?1, updated_at = ?2 WHERE id = ?3",
                (amount_cents, now, id),
            )?;
            return Ok(id);
        }
        conn.execute(
            "INSERT INTO budgets (category_id, amount_cents, year_month, period_type, created_at, updated_at)
             VALUES (?1, ?2, ?3, ?4, ?5, ?6)",
            (category_id, amount_cents, ym, "monthly", now, now),
        )?;
        return Ok(conn.last_insert_rowid());
    }

    // 模板（year_month = NULL）：SQLite UNIQUE 对 NULL 不视为相等，需要手动处理
    let existing: Option<BudgetId> = conn
        .query_row(
            "SELECT id FROM budgets WHERE category_id IS ?1 AND year_month IS NULL",
            [category_id],
            |row| row.get(0),
        )
        .optional()?;
    if let Some(id) = existing {
        conn.execute(
            "UPDATE budgets SET amount_cents = ?1, updated_at = ?2 WHERE id = ?3",
            (amount_cents, now, id),
        )?;
        Ok(id)
    } else {
        conn.execute(
            "INSERT INTO budgets (category_id, amount_cents, year_month, period_type, created_at, updated_at)
             VALUES (?1, ?2, ?3, ?4, ?5, ?6)",
            (category_id, amount_cents, Option::<&str>::None, "monthly", now, now),
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

/// 查询某分类的预算模板（year_month = NULL）。
pub fn get_template(
    conn: &Connection,
    category_id: Option<CategoryId>,
) -> Result<Option<Budget>, rusqlite::Error> {
    conn.query_row(
        "SELECT * FROM budgets WHERE category_id IS ?1 AND year_month IS NULL",
        [category_id],
        map_budget,
    )
    .optional()
}

/// 查询某分类在某月的有效预算。
/// 先查快照，没有则查模板。
pub fn get_for_month(
    conn: &Connection,
    category_id: Option<CategoryId>,
    year_month: &str,
) -> Result<Option<Budget>, rusqlite::Error> {
    // 1. 查该月快照
    let snapshot = conn
        .query_row(
            "SELECT * FROM budgets WHERE category_id IS ?1 AND year_month = ?2",
            (category_id, year_month),
            map_budget,
        )
        .optional()?;
    if snapshot.is_some() {
        return Ok(snapshot);
    }
    // 2. 查模板
    get_template(conn, category_id)
}

/// 查询某月的所有有效预算（快照优先，模板兜底）。
pub fn list_for_month(conn: &Connection, year_month: &str) -> Result<Vec<Budget>, rusqlite::Error> {
    let mut stmt = conn.prepare(
        "SELECT b.* FROM budgets b
         WHERE b.year_month IS NULL
           AND NOT EXISTS (
               SELECT 1 FROM budgets s
               WHERE s.year_month = ?1
                 AND ((s.category_id IS NULL AND b.category_id IS NULL) OR s.category_id = b.category_id)
           )
         UNION ALL
         SELECT * FROM budgets WHERE year_month = ?1
         ORDER BY category_id NULLS FIRST",
    )?;
    let rows = stmt.query_map([year_month], map_budget)?;
    rows.collect()
}

/// 查询所有预算（含模板和快照）。
pub fn list_all(conn: &Connection) -> Result<Vec<Budget>, rusqlite::Error> {
    let mut stmt = conn.prepare(
        "SELECT * FROM budgets ORDER BY category_id NULLS FIRST, year_month NULLS FIRST",
    )?;
    let rows = stmt.query_map([], map_budget)?;
    rows.collect()
}

/// 删除预算（按 ID）。
pub fn delete(conn: &Connection, id: BudgetId) -> Result<usize, rusqlite::Error> {
    conn.execute("DELETE FROM budgets WHERE id = ?1", [id])
}

/// 删除某分类从指定月份开始的所有快照。
pub fn delete_snapshots_from(
    conn: &Connection,
    category_id: Option<CategoryId>,
    from_year_month: &str,
) -> Result<usize, rusqlite::Error> {
    conn.execute(
        "DELETE FROM budgets WHERE category_id IS ?1 AND year_month >= ?2",
        (category_id, from_year_month),
    )
}

/// 检查某分类在某月是否有快照。
pub fn has_snapshot_for_month(
    conn: &Connection,
    category_id: Option<CategoryId>,
    year_month: &str,
) -> Result<bool, rusqlite::Error> {
    let count: i64 = conn.query_row(
        "SELECT COUNT(*) FROM budgets WHERE category_id IS ?1 AND year_month = ?2",
        (category_id, year_month),
        |row| row.get(0),
    )?;
    Ok(count > 0)
}

/// 删除某分类的预算模板。
pub fn delete_template(
    conn: &Connection,
    category_id: Option<CategoryId>,
) -> Result<usize, rusqlite::Error> {
    conn.execute(
        "DELETE FROM budgets WHERE category_id IS ?1 AND year_month IS NULL",
        [category_id],
    )
}
