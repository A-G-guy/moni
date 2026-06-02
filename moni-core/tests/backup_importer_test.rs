//! 备份恢复器补充测试。
//!
//! 覆盖范围：
//! - `validate_restored_db` 的 budgets 行数校验：匹配、不匹配、跳过（None）。

use moni_core::db::schema::init_schema;
use moni_core::domain::backup::importer::validate_restored_db;

/// budgets 行数匹配时应通过校验。
#[test]
fn test_validate_restored_db_budgets_match_succeeds() {
    let tmp_dir = tempfile::TempDir::new().unwrap();
    let db_path = tmp_dir.path().join("test.db");
    let conn = rusqlite::Connection::open(&db_path).unwrap();
    init_schema(&conn).unwrap();

    // 先插入分类（满足外键约束）
    conn.execute(
        "INSERT INTO categories (name, category_type, icon_name) VALUES (?1, ?2, ?3)",
        rusqlite::params!["餐饮", "expense", "food"],
    )
    .unwrap();

    // 插入 1 条 budget
    conn.execute(
        "INSERT INTO budgets (year_month, category_id, amount_cents) VALUES (?1, ?2, ?3)",
        rusqlite::params!["2026-05", 1, 10000],
    )
    .unwrap();

    drop(conn);

    let result = validate_restored_db(db_path.to_str().unwrap(), 0, 1, Some(1));
    assert!(result.is_ok(), "budgets 行数匹配时应通过，实际: {result:?}");
}

/// budgets 行数不匹配时应返回错误。
#[test]
fn test_validate_restored_db_budgets_mismatch_fails() {
    let tmp_dir = tempfile::TempDir::new().unwrap();
    let db_path = tmp_dir.path().join("test.db");
    let conn = rusqlite::Connection::open(&db_path).unwrap();
    init_schema(&conn).unwrap();

    // 先插入分类（满足外键约束）
    conn.execute(
        "INSERT INTO categories (name, category_type, icon_name) VALUES (?1, ?2, ?3)",
        rusqlite::params!["餐饮", "expense", "food"],
    )
    .unwrap();

    // 插入 1 条 budget，但期望 5 条
    conn.execute(
        "INSERT INTO budgets (year_month, category_id, amount_cents) VALUES (?1, ?2, ?3)",
        rusqlite::params!["2026-05", 1, 10000],
    )
    .unwrap();

    drop(conn);

    let result = validate_restored_db(db_path.to_str().unwrap(), 0, 1, Some(5));
    assert!(result.is_err(), "budgets 行数不匹配时应失败");
    let err_msg = format!("{:?}", result.unwrap_err());
    assert!(
        err_msg.contains("budgets"),
        "错误信息应包含 budgets，实际: {err_msg}"
    );
}

/// budget_count 为 None 时应跳过 budgets 校验（兼容旧备份）。
#[test]
fn test_validate_restored_db_budget_none_skips_check() {
    let tmp_dir = tempfile::TempDir::new().unwrap();
    let db_path = tmp_dir.path().join("test.db");
    let conn = rusqlite::Connection::open(&db_path).unwrap();
    init_schema(&conn).unwrap();

    // 先插入分类（满足外键约束）
    conn.execute(
        "INSERT INTO categories (name, category_type, icon_name) VALUES (?1, ?2, ?3)",
        rusqlite::params!["餐饮", "expense", "food"],
    )
    .unwrap();

    // 插入 budgets，但 expected 为 None
    conn.execute(
        "INSERT INTO budgets (year_month, category_id, amount_cents) VALUES (?1, ?2, ?3)",
        rusqlite::params!["2026-05", 1, 10000],
    )
    .unwrap();

    drop(conn);

    let result = validate_restored_db(db_path.to_str().unwrap(), 0, 1, None);
    assert!(
        result.is_ok(),
        "budget_count 为 None 时应跳过校验并直接通过"
    );
}

/// records 或 categories 不匹配时仍应先于 budgets 校验失败。
#[test]
fn test_validate_restored_db_records_mismatch_fails_before_budgets() {
    let tmp_dir = tempfile::TempDir::new().unwrap();
    let db_path = tmp_dir.path().join("test.db");
    let conn = rusqlite::Connection::open(&db_path).unwrap();
    init_schema(&conn).unwrap();

    drop(conn);

    // records 期望 5 但实际 0，应先失败
    let result = validate_restored_db(db_path.to_str().unwrap(), 5, 0, Some(0));
    assert!(result.is_err());
    let err_msg = format!("{:?}", result.unwrap_err());
    assert!(
        err_msg.contains("records"),
        "records 不匹配时应先返回 records 错误，实际: {err_msg}"
    );
}

/// 完整场景：含 records/categories/budgets 全部匹配时通过。
#[test]
fn test_validate_restored_db_all_counts_match_succeeds() {
    let tmp_dir = tempfile::TempDir::new().unwrap();
    let db_path = tmp_dir.path().join("test.db");
    let conn = rusqlite::Connection::open(&db_path).unwrap();
    init_schema(&conn).unwrap();

    // 插入 1 个分类
    conn.execute(
        "INSERT INTO categories (name, category_type, icon_name) VALUES (?1, ?2, ?3)",
        rusqlite::params!["餐饮", "expense", "food"],
    )
    .unwrap();
    conn.execute(
        "INSERT INTO records (amount_cents, record_type, category_id, note, created_at) VALUES (?1, ?2, ?3, ?4, ?5)",
        rusqlite::params![1000, "expense", 1, "午餐", chrono::Local::now().timestamp()],
    ).unwrap();
    conn.execute(
        "INSERT INTO records (amount_cents, record_type, category_id, note, created_at) VALUES (?1, ?2, ?3, ?4, ?5)",
        rusqlite::params![2000, "expense", 1, "晚餐", chrono::Local::now().timestamp()],
    ).unwrap();

    drop(conn);

    let result = validate_restored_db(db_path.to_str().unwrap(), 2, 1, Some(0));
    assert!(result.is_ok(), "全部计数匹配时应通过");
}
