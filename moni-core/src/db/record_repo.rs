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

/// 分页查询记录，按 created_at 降序。
pub fn list_paginated(
    conn: &Connection,
    page: u32,
    page_size: u32,
) -> Result<Vec<Record>, rusqlite::Error> {
    let offset = page as i64 * page_size as i64;
    let limit = page_size as i64;
    let mut stmt = conn.prepare(
        "SELECT * FROM records ORDER BY created_at DESC LIMIT ?1 OFFSET ?2"
    )?;
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

/// 按月聚合收入和支出。
pub fn monthly_aggregates(
    conn: &Connection,
    months: u8,
) -> Result<Vec<(String, AmountCents, AmountCents)>, rusqlite::Error> {
    let mut stmt = conn.prepare(
        "SELECT
            strftime('%Y-%m', datetime(created_at, 'unixepoch')) as year_month,
            SUM(CASE WHEN record_type = 'income' THEN amount_cents ELSE 0 END) as income,
            SUM(CASE WHEN record_type = 'expense' THEN amount_cents ELSE 0 END) as expense
         FROM records
         WHERE created_at >= strftime('%s', date('now', '-?1 months', 'start of month'))
         GROUP BY year_month
         ORDER BY year_month ASC
         LIMIT ?2"
    )?;
    let rows = stmt.query_map(
        (months as i64 + 1, months as i64),
        |row| {
            Ok((
                row.get::<_, String>(0)?,
                row.get::<_, Option<AmountCents>>(1)?.unwrap_or(0),
                row.get::<_, Option<AmountCents>>(2)?.unwrap_or(0),
            ))
        },
    )?;
    rows.collect()
}

/// 按分类聚合指定月份的支出。
pub fn category_aggregates(
    conn: &Connection,
    year_month: &str,
) -> Result<Vec<(CategoryId, String, String, AmountCents)>, rusqlite::Error> {
    let mut stmt = conn.prepare(
        "SELECT
            c.id,
            c.name,
            c.color_hex,
            SUM(r.amount_cents) as total
         FROM records r
         JOIN categories c ON r.category_id = c.id
         WHERE r.record_type = 'expense'
           AND strftime('%Y-%m', datetime(r.created_at, 'unixepoch')) = ?1
         GROUP BY c.id
         ORDER BY total DESC"
    )?;
    let rows = stmt.query_map([year_month], |row| {
        Ok((
            row.get::<_, i64>(0)?,
            row.get::<_, String>(1)?,
            row.get::<_, String>(2)?,
            row.get::<_, Option<AmountCents>>(3)?.unwrap_or(0),
        ))
    })?;
    rows.collect()
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::db::category_repo;
    use crate::db::connection::open_in_memory;
    use crate::db::schema::init_schema;

    fn setup() -> Connection {
        let conn = open_in_memory().unwrap();
        init_schema(&conn).unwrap();
        conn
    }

    fn create_category(conn: &Connection, name: &str, ty: RecordType) -> i64 {
        category_repo::insert(conn, name, ty, "icon", "#000", 1, false).unwrap()
    }

    #[test]
    fn test_insert_and_get() {
        let conn = setup();
        let cat_id = create_category(&conn, "餐饮", RecordType::Expense);
        let id = insert(&conn, 1234, RecordType::Expense, cat_id, "午餐", None).unwrap();
        assert!(id > 0);

        let rec = get_by_id(&conn, id).unwrap().unwrap();
        assert_eq!(rec.amount_cents, 1234);
        assert_eq!(rec.record_type, RecordType::Expense);
        assert_eq!(rec.note, "午餐");
    }

    #[test]
    fn test_list_paginated_ordering() {
        let conn = setup();
        let cat_id = create_category(&conn, "餐饮", RecordType::Expense);
        insert(&conn, 100, RecordType::Expense, cat_id, "", Some(1000)).unwrap();
        insert(&conn, 200, RecordType::Expense, cat_id, "", Some(2000)).unwrap();
        insert(&conn, 300, RecordType::Expense, cat_id, "", Some(1500)).unwrap();

        let list = list_paginated(&conn, 0, 10).unwrap();
        assert_eq!(list.len(), 3);
        assert_eq!(list[0].amount_cents, 200); // created_at=2000, newest
        assert_eq!(list[1].amount_cents, 300); // created_at=1500
        assert_eq!(list[2].amount_cents, 100); // created_at=1000
    }

    #[test]
    fn test_list_paginated_paging() {
        let conn = setup();
        let cat_id = create_category(&conn, "餐饮", RecordType::Expense);
        for i in 0..5 {
            insert(&conn, (i + 1) as i64 * 100, RecordType::Expense, cat_id, "", Some(i as i64 * 1000)).unwrap();
        }

        let page0 = list_paginated(&conn, 0, 2).unwrap();
        assert_eq!(page0.len(), 2);

        let page1 = list_paginated(&conn, 1, 2).unwrap();
        assert_eq!(page1.len(), 2);

        let page2 = list_paginated(&conn, 2, 2).unwrap();
        assert_eq!(page2.len(), 1);
    }

    #[test]
    fn test_update() {
        let conn = setup();
        let cat_id = create_category(&conn, "餐饮", RecordType::Expense);
        let id = insert(&conn, 100, RecordType::Expense, cat_id, "旧备注", None).unwrap();

        update(&conn, id, Some(200), None, None, Some("新备注")).unwrap();
        let rec = get_by_id(&conn, id).unwrap().unwrap();
        assert_eq!(rec.amount_cents, 200);
        assert_eq!(rec.note, "新备注");
    }

    #[test]
    fn test_delete() {
        let conn = setup();
        let cat_id = create_category(&conn, "餐饮", RecordType::Expense);
        let id = insert(&conn, 100, RecordType::Expense, cat_id, "", None).unwrap();
        delete(&conn, id).unwrap();
        assert!(get_by_id(&conn, id).unwrap().is_none());
    }

    #[test]
    fn test_list_by_date_range() {
        let conn = setup();
        let cat_id = create_category(&conn, "餐饮", RecordType::Expense);
        insert(&conn, 100, RecordType::Expense, cat_id, "", Some(1000)).unwrap();
        insert(&conn, 200, RecordType::Expense, cat_id, "", Some(2000)).unwrap();
        insert(&conn, 300, RecordType::Expense, cat_id, "", Some(3000)).unwrap();

        let list = list_by_date_range(&conn, 1500, 2500).unwrap();
        assert_eq!(list.len(), 1);
        assert_eq!(list[0].amount_cents, 200);
    }
}
