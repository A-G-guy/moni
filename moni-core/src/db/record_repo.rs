use rusqlite::{Connection, OptionalExtension, Row};

use moni_contracts::record::Record;
use moni_contracts::record::RecordType;
use moni_contracts::types::{AmountCents, CategoryId, RecordId, TimestampSec};

fn map_record(row: &Row) -> Result<Record, rusqlite::Error> {
    Ok(Record {
        id: row.get("id")?,
        amount_cents: row.get("amount_cents")?,
        record_type: match row.get::<_, String>("record_type")?.as_str() {
            "income" => RecordType::Income,
            "expense" => RecordType::Expense,
            other => {
                log::warn!("数据库中存在未知的记录类型: {other}, id={}", row.get::<_, i64>("id")?);
                return Err(rusqlite::Error::InvalidColumnType(
                    0,
                    "record_type".to_string(),
                    rusqlite::types::Type::Text,
                ));
            }
        },
        category_id: row.get("category_id")?,
        parent_category_id: row.get("parent_category_id")?,
        note: row.get("note")?,
        created_at: row.get("created_at")?,
        updated_at: row.get("updated_at")?,
    })
}

use crate::shared::types::year_month_from_timestamp;

/// 插入新记录。
pub fn insert(
    conn: &Connection,
    amount_cents: AmountCents,
    record_type: RecordType,
    category_id: i64,
    parent_category_id: Option<CategoryId>,
    note: &str,
    timestamp: Option<TimestampSec>,
) -> Result<RecordId, rusqlite::Error> {
    let now = chrono::Utc::now().timestamp();
    let created_at = timestamp.unwrap_or(now);
    let year_month = year_month_from_timestamp(created_at);

    conn.execute(
        "INSERT INTO records (amount_cents, record_type, category_id, parent_category_id, note, created_at, updated_at, year_month)
         VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8)",
        (
            amount_cents,
            match record_type {
                RecordType::Income => "income",
                RecordType::Expense => "expense",
            },
            category_id,
            parent_category_id,
            note,
            created_at,
            now,
            year_month,
        ),
    )?;
    Ok(conn.last_insert_rowid())
}

/// 根据 ID 查询记录。
pub fn get_by_id(conn: &Connection, id: RecordId) -> Result<Option<Record>, rusqlite::Error> {
    conn.query_row("SELECT * FROM records WHERE id = ?1", [id], map_record)
        .optional()
}

/// 分页查询记录，按 `created_at` 降序。
pub fn list_paginated(
    conn: &Connection,
    page: u32,
    page_size: u32,
) -> Result<Vec<Record>, rusqlite::Error> {
    let offset = i64::from(page)
        .checked_mul(i64::from(page_size))
        .unwrap_or(i64::MAX);
    // OFFSET 溢出时直接返回空结果，避免 SQLite 全表扫描
    if offset == i64::MAX {
        return Ok(Vec::new());
    }
    let limit = i64::from(page_size);
    let mut stmt =
        conn.prepare("SELECT * FROM records ORDER BY created_at DESC LIMIT ?1 OFFSET ?2")?;
    let rows = stmt.query_map([limit, offset], map_record)?;
    rows.collect()
}

/// 查询指定日期范围内的记录（包含边界）。
pub fn list_by_date_range(
    conn: &Connection,
    start: TimestampSec,
    end: TimestampSec,
) -> Result<Vec<Record>, rusqlite::Error> {
    let mut stmt = conn.prepare(
        "SELECT * FROM records WHERE created_at >= ?1 AND created_at <= ?2 ORDER BY created_at DESC"
    )?;
    let rows = stmt.query_map([start, end], map_record)?;
    rows.collect()
}

/// 更新记录。
pub fn update(
    conn: &Connection,
    id: RecordId,
    amount_cents: Option<AmountCents>,
    record_type: Option<RecordType>,
    category_id: Option<i64>,
    parent_category_id: Option<Option<CategoryId>>,
    note: Option<&str>,
) -> Result<usize, rusqlite::Error> {
    let now = chrono::Utc::now().timestamp();
    let type_str = record_type.map(|t| match t {
        RecordType::Income => "income",
        RecordType::Expense => "expense",
    });
    match parent_category_id {
        Some(pid) => conn.execute(
            "UPDATE records SET amount_cents = COALESCE(?2, amount_cents), record_type = COALESCE(?3, record_type), category_id = COALESCE(?4, category_id), parent_category_id = ?5, note = COALESCE(?6, note), updated_at = ?7 WHERE id = ?1",
            (id, amount_cents, type_str, category_id, pid, note, now),
        ),
        None => conn.execute(
            "UPDATE records SET amount_cents = COALESCE(?2, amount_cents), record_type = COALESCE(?3, record_type), category_id = COALESCE(?4, category_id), note = COALESCE(?5, note), updated_at = ?6 WHERE id = ?1",
            (id, amount_cents, type_str, category_id, note, now),
        ),
    }
}

/// 删除记录。
pub fn delete(conn: &Connection, id: RecordId) -> Result<usize, rusqlite::Error> {
    conn.execute("DELETE FROM records WHERE id = ?1", [id])
}

/// 查询指定年月的记录（yyyy-MM 格式）。
/// 使用 `strftime` 动态计算本地时区年月，避免依赖持久化列值的一致性。
pub fn list_by_year_month(
    conn: &Connection,
    year_month: &str,
) -> Result<Vec<Record>, rusqlite::Error> {
    let mut stmt = conn.prepare(
        "SELECT * FROM records
         WHERE strftime('%Y-%m', datetime(created_at, 'unixepoch', 'localtime')) = ?1
         ORDER BY created_at DESC"
    )?;
    let rows = stmt.query_map([year_month], map_record)?;
    rows.collect()
}

/// 按月聚合收入和支出。
/// 使用 `strftime` 动态计算本地时区年月，避免依赖持久化列值的一致性。
pub fn monthly_aggregates(
    conn: &Connection,
    months: u8,
) -> Result<Vec<(String, AmountCents, AmountCents)>, rusqlite::Error> {
    let mut stmt = conn.prepare(
        "SELECT
            strftime('%Y-%m', datetime(created_at, 'unixepoch', 'localtime')) as ym,
            SUM(CASE WHEN record_type = 'income' THEN amount_cents ELSE 0 END) as income,
            SUM(CASE WHEN record_type = 'expense' THEN amount_cents ELSE 0 END) as expense
         FROM records
         GROUP BY ym
         HAVING ym IS NOT NULL
         ORDER BY ym DESC
         LIMIT ?1",
    )?;
    let rows = stmt.query_map([i64::from(months)], |row| {
        Ok((
            row.get::<_, String>(0)?,
            row.get::<_, Option<AmountCents>>(1)?.unwrap_or(0),
            row.get::<_, Option<AmountCents>>(2)?.unwrap_or(0),
        ))
    })?;
    let mut result: Vec<(String, AmountCents, AmountCents)> = rows.collect::<Result<_, _>>()?;
    // DESC 查询后反转，保持 UI 时间轴从左到右（旧到新）
    result.reverse();
    Ok(result)
}

/// 按分类聚合指定月份的支出（最细粒度，按 category_id 分组）。
/// 使用 `strftime` 动态计算本地时区年月，避免依赖持久化列值的一致性。
pub fn category_aggregates(
    conn: &Connection,
    year_month: &str,
) -> Result<Vec<(CategoryId, String, AmountCents)>, rusqlite::Error> {
    let mut stmt = conn.prepare(
        "SELECT
            c.id,
            c.name,
            SUM(r.amount_cents) as total
         FROM records r
         JOIN categories c ON r.category_id = c.id
         WHERE r.record_type = 'expense'
           AND strftime('%Y-%m', datetime(r.created_at, 'unixepoch', 'localtime')) = ?1
         GROUP BY c.id
         ORDER BY total DESC",
    )?;
    let rows = stmt.query_map([year_month], |row| {
        Ok((
            row.get::<_, i64>(0)?,
            row.get::<_, String>(1)?,
            row.get::<_, Option<AmountCents>>(2)?.unwrap_or(0),
        ))
    })?;
    rows.collect()
}

/// 按一级分类聚合指定月份的支出（基于账单固化时的 parent_category_id）。
/// 使用 `strftime` 动态计算本地时区年月，避免依赖持久化列值的一致性。
pub fn category_aggregates_by_parent(
    conn: &Connection,
    year_month: &str,
) -> Result<Vec<(CategoryId, String, AmountCents)>, rusqlite::Error> {
    let mut stmt = conn.prepare(
        "SELECT
            COALESCE(p.id, c.id) as category_id,
            COALESCE(p.name, c.name) as category_name,
            SUM(r.amount_cents) as total
         FROM records r
         JOIN categories c ON r.category_id = c.id
         LEFT JOIN categories p ON r.parent_category_id = p.id
         WHERE r.record_type = 'expense'
           AND strftime('%Y-%m', datetime(r.created_at, 'unixepoch', 'localtime')) = ?1
         GROUP BY COALESCE(p.id, c.id)
         ORDER BY total DESC",
    )?;
    let rows = stmt.query_map([year_month], |row| {
        Ok((
            row.get::<_, i64>(0)?,
            row.get::<_, String>(1)?,
            row.get::<_, Option<AmountCents>>(2)?.unwrap_or(0),
        ))
    })?;
    rows.collect()
}
