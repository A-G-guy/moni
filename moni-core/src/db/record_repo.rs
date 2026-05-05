use chrono::Datelike;
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
            _ => RecordType::Expense,
        },
        category_id: row.get("category_id")?,
        note: row.get("note")?,
        created_at: row.get("created_at")?,
        updated_at: row.get("updated_at")?,
    })
}

/// 插入新记录。
pub fn insert(
    conn: &Connection,
    amount_cents: AmountCents,
    record_type: RecordType,
    category_id: i64,
    note: &str,
    timestamp: Option<TimestampSec>,
) -> Result<RecordId, rusqlite::Error> {
    let now = chrono::Utc::now().timestamp();
    let created_at = timestamp.unwrap_or(now);
    conn.execute(
        "INSERT INTO records (amount_cents, record_type, category_id, note, created_at, updated_at)
         VALUES (?1, ?2, ?3, ?4, ?5, ?6)",
        (
            amount_cents,
            match record_type {
                RecordType::Income => "income",
                RecordType::Expense => "expense",
            },
            category_id,
            note,
            created_at,
            now,
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
    note: Option<&str>,
) -> Result<usize, rusqlite::Error> {
    let now = chrono::Utc::now().timestamp();
    conn.execute(
        "UPDATE records SET
            amount_cents = COALESCE(?2, amount_cents),
            record_type = COALESCE(?3, record_type),
            category_id = COALESCE(?4, category_id),
            note = COALESCE(?5, note),
            updated_at = ?6
         WHERE id = ?1",
        (
            id,
            amount_cents,
            record_type.map(|t| match t {
                RecordType::Income => "income",
                RecordType::Expense => "expense",
            }),
            category_id,
            note,
            now,
        ),
    )
}

/// 删除记录。
pub fn delete(conn: &Connection, id: RecordId) -> Result<usize, rusqlite::Error> {
    conn.execute("DELETE FROM records WHERE id = ?1", [id])
}

/// 查询指定年月的记录（yyyy-MM 格式）。
pub fn list_by_year_month(
    conn: &Connection,
    year_month: &str,
) -> Result<Vec<Record>, rusqlite::Error> {
    let mut stmt = conn.prepare(
        "SELECT * FROM records
         WHERE strftime('%Y-%m', datetime(created_at, 'unixepoch')) = ?1
         ORDER BY created_at DESC"
    )?;
    let rows = stmt.query_map([year_month], map_record)?;
    rows.collect()
}

/// 按月聚合收入和支出。
pub fn monthly_aggregates(
    conn: &Connection,
    months: u8,
) -> Result<Vec<(String, AmountCents, AmountCents)>, rusqlite::Error> {
    let now = chrono::Utc::now();
    let start = now - chrono::Months::new(u32::from(months) + 1);
    let start_date = chrono::NaiveDate::from_ymd_opt(start.year(), start.month(), 1)
        .ok_or_else(|| rusqlite::Error::InvalidParameterName("无效的日期参数".to_string()))?;
    let start_datetime = start_date
        .and_hms_opt(0, 0, 0)
        .ok_or_else(|| rusqlite::Error::InvalidParameterName("无效的时间参数".to_string()))?;
    let start_timestamp = start_datetime.and_utc().timestamp();

    let mut stmt = conn.prepare(
        "SELECT
            strftime('%Y-%m', datetime(created_at, 'unixepoch')) as year_month,
            SUM(CASE WHEN record_type = 'income' THEN amount_cents ELSE 0 END) as income,
            SUM(CASE WHEN record_type = 'expense' THEN amount_cents ELSE 0 END) as expense
         FROM records
         WHERE created_at >= ?1
         GROUP BY year_month
         ORDER BY year_month ASC
         LIMIT ?2",
    )?;
    let rows = stmt.query_map((start_timestamp, i64::from(months)), |row| {
        Ok((
            row.get::<_, String>(0)?,
            row.get::<_, Option<AmountCents>>(1)?.unwrap_or(0),
            row.get::<_, Option<AmountCents>>(2)?.unwrap_or(0),
        ))
    })?;
    rows.collect()
}

/// 按分类聚合指定月份的支出。
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
           AND strftime('%Y-%m', datetime(r.created_at, 'unixepoch')) = ?1
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
