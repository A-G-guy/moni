mod common;

use moni_contracts::record::RecordType;
use moni_core::db::category_repo;
use moni_core::db::connection::open_in_memory;
use moni_core::db::record_repo;
use moni_core::db::schema::init_schema;

/// 创建内存数据库连接并初始化 schema。
fn setup() -> rusqlite::Connection {
    let conn = open_in_memory().unwrap();
    init_schema(&conn).unwrap();
    conn
}

/// 创建分类并返回其 id。
fn create_category(conn: &rusqlite::Connection, name: &str, ty: RecordType) -> i64 {
    category_repo::insert(conn, name, None, ty, "icon", 1, None).unwrap()
}

// =============================================================================
// list_by_date_range 边界测试（直接调用 repo，无对应 dispatch intent）
// =============================================================================

#[test]
fn test_list_by_date_range_same_second() {
    let conn = setup();
    let cat_id = create_category(&conn, "餐饮", RecordType::Expense);

    let ts = 1704067200i64; // 2024-01-01 00:00:00 UTC
    record_repo::insert(&conn, 100, RecordType::Expense, cat_id, None, "同秒测试", Some(ts)).unwrap();

    let recs = record_repo::list_by_date_range(&conn, ts, ts).unwrap();
    assert_eq!(recs.len(), 1);
    assert_eq!(recs[0].amount_cents, 100);
}

#[test]
fn test_list_by_date_range_start_greater_than_end() {
    let conn = setup();

    // start > end 应返回空数组
    let recs = record_repo::list_by_date_range(&conn, 2000, 1000).unwrap();
    assert!(recs.is_empty(), "start > end 时应返回空");
}

#[test]
fn test_list_by_date_range_cross_year() {
    let conn = setup();
    let cat_id = create_category(&conn, "餐饮", RecordType::Expense);

    // 2023-12-31 23:59:00 UTC = 1704067140
    let dec_31 = 1704067140i64;
    // 2024-01-01 00:01:00 UTC = 1704067260
    let jan_01 = 1704067260i64;

    record_repo::insert(&conn, 100, RecordType::Expense, cat_id, None, "跨年-旧", Some(dec_31)).unwrap();
    record_repo::insert(&conn, 200, RecordType::Expense, cat_id, None, "跨年-新", Some(jan_01)).unwrap();

    let recs = record_repo::list_by_date_range(&conn, dec_31, jan_01).unwrap();
    assert_eq!(recs.len(), 2);
    // 按 created_at 降序
    assert_eq!(recs[0].amount_cents, 200);
    assert_eq!(recs[1].amount_cents, 100);
}

// =============================================================================
// list_by_year_month 边界月份测试（通过 dispatch record_list_by_month）
// =============================================================================

#[test]
fn test_list_by_year_month_january() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let category_id = state["categories"][0]["id"].as_i64().unwrap();

        // 2026-01-15 00:00:00 UTC = 1768435200
        let jan_15 = 1768435200i64;
        let intent = format!(
            r#"{{"type":"record_create","amount_cents":100,"record_type":"expense","category_id":{category_id},"note":"一月记录","timestamp":{jan_15}}}"#
        );
        core.dispatch(intent).await.unwrap();

        let intent = r#"{"type":"record_list_by_month","year_month":"2026-01"}"#.to_string();
        let update = core.dispatch(intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let records = state["records"].as_array().unwrap();
        assert_eq!(records.len(), 1);
        assert_eq!(records[0]["note"], "一月记录");
    });
}

#[test]
fn test_list_by_year_month_december() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let category_id = state["categories"][0]["id"].as_i64().unwrap();

        // 2025-12-31 12:00:00 UTC = 1767182400（避免本地时区跨到次年一月）
        let dec_31 = 1767182400i64;
        let intent = format!(
            r#"{{"type":"record_create","amount_cents":200,"record_type":"expense","category_id":{category_id},"note":"年末记录","timestamp":{dec_31}}}"#
        );
        core.dispatch(intent).await.unwrap();

        let intent = r#"{"type":"record_list_by_month","year_month":"2025-12"}"#.to_string();
        let update = core.dispatch(intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let records = state["records"].as_array().unwrap();
        assert_eq!(records.len(), 1);
        assert_eq!(records[0]["note"], "年末记录");
    });
}

#[test]
fn test_list_by_year_month_leap_year_february() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let category_id = state["categories"][0]["id"].as_i64().unwrap();

        // 2024-02-29 12:00:00 UTC (闰年) = 1709208000
        let feb_29 = 1709208000i64;
        let intent = format!(
            r#"{{"type":"record_create","amount_cents":300,"record_type":"expense","category_id":{category_id},"note":"闰年229","timestamp":{feb_29}}}"#
        );
        core.dispatch(intent).await.unwrap();

        let intent = r#"{"type":"record_list_by_month","year_month":"2024-02"}"#.to_string();
        let update = core.dispatch(intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let records = state["records"].as_array().unwrap();
        assert_eq!(records.len(), 1);
        assert_eq!(records[0]["note"], "闰年229");
    });
}

#[test]
fn test_list_by_year_month_empty_month() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        // 查询一个绝对没有数据的月份
        let intent = r#"{"type":"record_list_by_month","year_month":"1999-01"}"#.to_string();
        let update = core.dispatch(intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let records = state["records"].as_array().unwrap();
        assert!(records.is_empty(), "无数据月份应返回空数组");
    });
}

// =============================================================================
// monthly_aggregates 测试（通过 dispatch stats_monthly_summary）
// =============================================================================

#[test]
fn test_monthly_aggregates_multi_month() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let expense_cat = state["categories"]
            .as_array()
            .unwrap()
            .iter()
            .find(|c| c["categoryType"] == "expense")
            .unwrap()["id"]
            .as_i64()
            .unwrap();
        let income_cat = state["categories"]
            .as_array()
            .unwrap()
            .iter()
            .find(|c| c["categoryType"] == "income")
            .unwrap()["id"]
            .as_i64()
            .unwrap();

        // 2026-03-15 = 1773580800
        let mar_15 = 1773580800i64;
        // 2026-04-15 = 1776259200
        let apr_15 = 1776259200i64;

        let intent = format!(
            r#"{{"type":"record_create","amount_cents":5000,"record_type":"expense","category_id":{expense_cat},"note":"三月支出","timestamp":{mar_15}}}"#
        );
        core.dispatch(intent).await.unwrap();

        let intent = format!(
            r#"{{"type":"record_create","amount_cents":3000,"record_type":"income","category_id":{income_cat},"note":"三月收入","timestamp":{mar_15}}}"#
        );
        core.dispatch(intent).await.unwrap();

        let intent = format!(
            r#"{{"type":"record_create","amount_cents":8000,"record_type":"expense","category_id":{expense_cat},"note":"四月支出","timestamp":{apr_15}}}"#
        );
        core.dispatch(intent).await.unwrap();

        let intent = r#"{"type":"stats_monthly_summary","months":6}"#.to_string();
        let update = core.dispatch(intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let summaries = state["monthlySummaries"].as_array().unwrap();
        assert!(!summaries.is_empty());

        // 找到 2026-03 和 2026-04 的汇总
        let mar = summaries.iter().find(|s| s["yearMonth"] == "2026-03");
        let apr = summaries.iter().find(|s| s["yearMonth"] == "2026-04");

        assert!(mar.is_some(), "应包含 2026-03 的汇总");
        assert_eq!(mar.unwrap()["expenseCents"], 5000);
        assert_eq!(mar.unwrap()["incomeCents"], 3000);
        assert_eq!(mar.unwrap()["balanceCents"], -2000);

        assert!(apr.is_some(), "应包含 2026-04 的汇总");
        assert_eq!(apr.unwrap()["expenseCents"], 8000);
        assert_eq!(apr.unwrap()["incomeCents"], 0);
        assert_eq!(apr.unwrap()["balanceCents"], -8000);
    });
}

#[test]
fn test_monthly_aggregates_empty_data() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let intent = r#"{"type":"stats_monthly_summary","months":6}"#.to_string();
        let update = core.dispatch(intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let summaries = state["monthlySummaries"].as_array().unwrap();
        assert!(summaries.is_empty(), "无记录时月度汇总应为空");
    });
}

// =============================================================================
// category_aggregates 测试（通过 dispatch stats_category_breakdown）
// =============================================================================

#[test]
fn test_category_aggregates_cross_category() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let categories = state["categories"].as_array().unwrap();

        // 找到两个不同的支出分类
        let expense_cats: Vec<_> = categories
            .iter()
            .filter(|c| c["categoryType"] == "expense")
            .collect();
        assert!(
            expense_cats.len() >= 2,
            "需要至少两个支出分类进行跨分类测试"
        );

        let cat_a = expense_cats[0]["id"].as_i64().unwrap();
        let cat_b = expense_cats[1]["id"].as_i64().unwrap();

        let intent = format!(
            r#"{{"type":"record_create","amount_cents":6000,"record_type":"expense","category_id":{cat_a},"note":"分类A"}}"#
        );
        core.dispatch(intent).await.unwrap();

        let intent = format!(
            r#"{{"type":"record_create","amount_cents":4000,"record_type":"expense","category_id":{cat_b},"note":"分类B"}}"#
        );
        core.dispatch(intent).await.unwrap();

        let year_month = chrono::Local::now().format("%Y-%m").to_string();
        let intent = format!(
            r#"{{"type":"stats_category_breakdown","year_month":"{year_month}","aggregate_by_parent":false}}"#
        );
        let update = core.dispatch(intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let breakdown = state["currentMonthBreakdown"].as_array().unwrap();
        assert_eq!(breakdown.len(), 2, "应返回两个分类的聚合");

        let total: i64 = breakdown.iter().map(|b| b["amountCents"].as_i64().unwrap()).sum();
        assert_eq!(total, 10000);
    });
}

#[test]
fn test_category_aggregates_single_category() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let expense_cat = state["categories"]
            .as_array()
            .unwrap()
            .iter()
            .find(|c| c["categoryType"] == "expense")
            .unwrap()["id"]
            .as_i64()
            .unwrap();

        let intent = format!(
            r#"{{"type":"record_create","amount_cents":5000,"record_type":"expense","category_id":{expense_cat},"note":"单分类"}}"#
        );
        core.dispatch(intent).await.unwrap();

        let year_month = chrono::Local::now().format("%Y-%m").to_string();
        let intent = format!(
            r#"{{"type":"stats_category_breakdown","year_month":"{year_month}","aggregate_by_parent":false}}"#
        );
        let update = core.dispatch(intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let breakdown = state["currentMonthBreakdown"].as_array().unwrap();
        assert_eq!(breakdown.len(), 1);
        assert_eq!(breakdown[0]["amountCents"], 5000);
        assert_eq!(breakdown[0]["percentage"], 100.0);
    });
}

// =============================================================================
// category_aggregates_by_parent 测试（通过 dispatch stats_category_breakdown + aggregate_by_parent=true）
// =============================================================================

#[test]
fn test_category_aggregates_by_parent_merge() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let categories = state["categories"].as_array().unwrap();

        // 找到一个有子分类的父分类（预设数据包含餐饮->早餐/外卖等父子关系）
        let parent_cat = categories
            .iter()
            .find(|c| c["parentId"].is_null() && c["categoryType"] == "expense")
            .expect("预设数据应包含至少一个支出父分类");

        let parent_id = parent_cat["id"].as_i64().unwrap();
        let child_cat = categories
            .iter()
            .find(|c| c["parentId"] == parent_id)
            .expect("预设数据应包含至少一个子分类");

        let child_id = child_cat["id"].as_i64().unwrap();

        // 父分类记录
        let intent = format!(
            r#"{{"type":"record_create","amount_cents":5000,"record_type":"expense","category_id":{parent_id},"note":"父分类记录"}}"#
        );
        core.dispatch(intent).await.unwrap();

        // 子分类记录
        let intent = format!(
            r#"{{"type":"record_create","amount_cents":3000,"record_type":"expense","category_id":{child_id},"note":"子分类记录"}}"#
        );
        core.dispatch(intent).await.unwrap();

        let year_month = chrono::Local::now().format("%Y-%m").to_string();
        let intent = format!(
            r#"{{"type":"stats_category_breakdown","year_month":"{year_month}","aggregate_by_parent":true}}"#
        );
        let update = core.dispatch(intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let breakdown = state["currentMonthBreakdown"].as_array().unwrap();

        // 父子分类应合并为一条
        let parent_entry = breakdown.iter().find(|b| b["categoryId"] == parent_id);
        assert!(parent_entry.is_some(), "应找到合并后的父分类条目");
        assert_eq!(parent_entry.unwrap()["amountCents"], 8000);
    });
}

// =============================================================================
// update record_type 测试（通过 dispatch record_update，覆盖 L108-L111）
// =============================================================================

#[test]
fn test_update_record_type_expense_to_income_and_back() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let expense_cat = state["categories"]
            .as_array()
            .unwrap()
            .iter()
            .find(|c| c["categoryType"] == "expense")
            .unwrap()["id"]
            .as_i64()
            .unwrap();
        let income_cat = state["categories"]
            .as_array()
            .unwrap()
            .iter()
            .find(|c| c["categoryType"] == "income")
            .unwrap()["id"]
            .as_i64()
            .unwrap();

        // 创建支出记录
        let intent = format!(
            r#"{{"type":"record_create","amount_cents":1000,"record_type":"expense","category_id":{expense_cat},"note":"类型切换测试","timestamp":null}}"#
        );
        core.dispatch(intent).await.unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let record_id = state["records"][0]["id"].as_i64().unwrap();
        assert_eq!(state["records"][0]["recordType"], "expense");

        // expense -> income
        let intent = format!(
            r#"{{"type":"record_update","id":{record_id},"amount_cents":null,"record_type":"income","category_id":{income_cat},"note":null}}"#
        );
        let update = core.dispatch(intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null());
        assert_eq!(state["records"][0]["recordType"], "income");
        assert_eq!(state["records"][0]["categoryId"], income_cat);

        // income -> expense
        let intent = format!(
            r#"{{"type":"record_update","id":{record_id},"amount_cents":null,"record_type":"expense","category_id":{expense_cat},"note":null}}"#
        );
        let update = core.dispatch(intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null());
        assert_eq!(state["records"][0]["recordType"], "expense");
        assert_eq!(state["records"][0]["categoryId"], expense_cat);
    });
}

// =============================================================================
// 大量记录分页测试（通过 dispatch record_list）
// =============================================================================

#[test]
fn test_large_record_pagination_various_page_sizes() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().unwrap();

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let category_id = state["categories"][0]["id"].as_i64().unwrap();

        // 创建 10 条记录
        for i in 1..=10 {
            let intent = format!(
                r#"{{"type":"record_create","amount_cents":{}00,"record_type":"expense","category_id":{category_id},"note":"批量{i}","timestamp":{}}}"#,
                i, i * 1000
            );
            core.dispatch(intent).await.unwrap();
        }

        // page_size = 3
        let intent = r#"{"type":"record_list","page":0,"page_size":3}"#.to_string();
        let update = core.dispatch(intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let records = state["records"].as_array().unwrap();
        assert_eq!(records.len(), 3);
        assert_eq!(records[0]["note"], "批量10");
        assert_eq!(records[1]["note"], "批量9");
        assert_eq!(records[2]["note"], "批量8");

        // page = 2, page_size = 3 (第3页)
        let intent = r#"{"type":"record_list","page":2,"page_size":3}"#.to_string();
        let update = core.dispatch(intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let records = state["records"].as_array().unwrap();
        assert_eq!(records.len(), 3);
        assert_eq!(records[0]["note"], "批量4");
        assert_eq!(records[1]["note"], "批量3");
        assert_eq!(records[2]["note"], "批量2");

        // page = 3, page_size = 3 (第4页，只剩1条)
        let intent = r#"{"type":"record_list","page":3,"page_size":3}"#.to_string();
        let update = core.dispatch(intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let records = state["records"].as_array().unwrap();
        assert_eq!(records.len(), 1);
        assert_eq!(records[0]["note"], "批量1");

        // page = 4, page_size = 3 (超出范围)
        let intent = r#"{"type":"record_list","page":4,"page_size":3}"#.to_string();
        let update = core.dispatch(intent).await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&update.state_json).unwrap();
        let records = state["records"].as_array().unwrap();
        assert!(records.is_empty(), "超出页码应返回空");
    });
}
