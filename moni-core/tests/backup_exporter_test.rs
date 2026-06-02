//! 备份导出器补充测试。
//!
//! 覆盖范围：
//! - `collect_stats` 的 `settings_count` 动态计算：空 JSON、多键 JSON、无效 JSON。
//! - `collect_stats` 的 `budget_count` 从数据库正确读取。

use moni_core::db::schema::init_schema;
use moni_core::domain::backup::exporter::collect_stats;

/// 空 settings JSON 应返回 settings_count = 0。
#[test]
fn test_collect_stats_empty_settings_json() {
    let conn = rusqlite::Connection::open_in_memory().expect("内存数据库创建失败");
    init_schema(&conn).expect("init_schema 应成功");

    let stats = collect_stats(&conn, "{}").expect("collect_stats 应成功");
    assert_eq!(stats.settings_count, 0, "空 JSON 的 settings_count 应为 0");
}

/// 含多个键的 settings JSON 应正确计数。
#[test]
fn test_collect_stats_multiple_keys_counts_correctly() {
    let conn = rusqlite::Connection::open_in_memory().expect("内存数据库创建失败");
    init_schema(&conn).expect("init_schema 应成功");

    let settings_json = r#"{"schema":1,"currency_symbol":"¥","theme_mode":"dark"}"#;
    let stats = collect_stats(&conn, settings_json).expect("collect_stats 应成功");
    assert_eq!(stats.settings_count, 3, "3 个键的 settings_count 应为 3");
}

/// 无效 JSON 应优雅回退到 settings_count = 0。
#[test]
fn test_collect_stats_invalid_json_fallbacks_to_zero() {
    let conn = rusqlite::Connection::open_in_memory().expect("内存数据库创建失败");
    init_schema(&conn).expect("init_schema 应成功");

    let stats = collect_stats(&conn, "not valid json").expect("collect_stats 应成功");
    assert_eq!(
        stats.settings_count, 0,
        "无效 JSON 的 settings_count 应回退为 0"
    );
}

/// 非对象 JSON（如数组）应回退到 settings_count = 0。
#[test]
fn test_collect_stats_array_json_fallbacks_to_zero() {
    let conn = rusqlite::Connection::open_in_memory().expect("内存数据库创建失败");
    init_schema(&conn).expect("init_schema 应成功");

    let stats = collect_stats(&conn, "[1, 2, 3]").expect("collect_stats 应成功");
    assert_eq!(
        stats.settings_count, 0,
        "数组 JSON 的 settings_count 应回退为 0"
    );
}

/// budget_count 应从数据库 budgets 表正确读取。
#[test]
fn test_collect_stats_budget_count_from_db() {
    let conn = rusqlite::Connection::open_in_memory().expect("内存数据库创建失败");
    init_schema(&conn).expect("init_schema 应成功");

    // 先插入 2 个分类（budgets.category_id 有外键约束）
    conn.execute(
        "INSERT INTO categories (name, category_type, icon_name) VALUES (?1, ?2, ?3)",
        rusqlite::params!["餐饮", "expense", "food"],
    )
    .unwrap();
    conn.execute(
        "INSERT INTO categories (name, category_type, icon_name) VALUES (?1, ?2, ?3)",
        rusqlite::params!["交通", "expense", "bus"],
    )
    .unwrap();

    // 插入 2 个预算
    conn.execute(
        "INSERT INTO budgets (year_month, category_id, amount_cents) VALUES (?1, ?2, ?3)",
        rusqlite::params!["2026-05", 1, 10000],
    )
    .unwrap();
    conn.execute(
        "INSERT INTO budgets (year_month, category_id, amount_cents) VALUES (?1, ?2, ?3)",
        rusqlite::params!["2026-05", 2, 20000],
    )
    .unwrap();

    let stats = collect_stats(&conn, "{}").expect("collect_stats 应成功");
    assert_eq!(stats.budget_count, Some(2), "budget_count 应为 2");
}

/// 空数据库时所有计数应为 0。
#[test]
fn test_collect_stats_empty_db_all_zero() {
    let conn = rusqlite::Connection::open_in_memory().expect("内存数据库创建失败");
    init_schema(&conn).expect("init_schema 应成功");

    let stats = collect_stats(&conn, "{}").expect("collect_stats 应成功");
    assert_eq!(stats.record_count, 0);
    assert_eq!(stats.category_count, 0);
    assert_eq!(stats.budget_count, Some(0));
    assert_eq!(stats.settings_count, 0);
}
