mod common;

use moni_core::models::intent::{CoreIntent, MockPreset};

/// 覆盖 `dispatch_dev.rs`、`dispatch_settings.rs`、`dispatch_stats.rs` 三个模块的未覆盖分支：
/// - dev: mock_data_generate 边界（count=0）、生成后状态刷新（monthly_summaries）、清空后再 seed、
///        生成后清空、先后使用不同 preset。
/// - settings: currency 多次切换、空字符串 symbol。
/// - stats: 有记录后的 monthly_summary、aggregate_by_parent=true 空数据与有数据、
///          months 参数边界、跨月记录仅出现在对应月份。

/// count=0 时生成 Mock 数据应返回空记录列表且状态刷新正常。
#[test]
fn test_dev_generate_mock_data_count_zero() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().expect("测试 Runtime 创建失败");

    rt.block_on(async {
        let result = core
            .dispatch(
                serde_json::to_string(&CoreIntent::DevGenerateMockData {
                    count: 0,
                    preset: MockPreset::Normal,
                })
                .unwrap(),
            )
            .await;
        assert!(result.is_ok());

        let state: serde_json::Value = serde_json::from_str(&result.unwrap().state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null());
        assert_eq!(state["records"].as_array().unwrap().len(), 0);
        assert_eq!(state["recordGroups"].as_array().unwrap().len(), 0);
    });
}

/// 生成 Mock 数据后 dispatch_dev 内部应自动刷新 monthly_summaries 到状态。
#[test]
fn test_dev_generate_mock_data_refreshes_monthly_summaries() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().expect("测试 Runtime 创建失败");

    rt.block_on(async {
        let result = core
            .dispatch(
                serde_json::to_string(&CoreIntent::DevGenerateMockData {
                    count: 5,
                    preset: MockPreset::Normal,
                })
                .unwrap(),
            )
            .await
            .unwrap();

        // DevGenerateMockData 完成后已自动刷新 monthly_summaries，无需再 dispatch stats
        let state: serde_json::Value = serde_json::from_str(&result.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null());
        let summaries = state["monthlySummaries"].as_array().unwrap();
        assert!(
            !summaries.is_empty(),
            "生成 mock 数据后 monthly_summaries 不应为空"
        );
    });
}

/// 清空所有数据后再执行 seed presets 应能重新填充分类。
#[test]
fn test_dev_clear_all_then_seed_presets() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().expect("测试 Runtime 创建失败");

    rt.block_on(async {
        // 先清空
        core.dispatch(serde_json::to_string(&CoreIntent::DevClearAllData).unwrap())
            .await
            .unwrap();

        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        assert!(state["categories"].as_array().unwrap().is_empty());
        assert!(state["records"].as_array().unwrap().is_empty());

        // 再 seed
        let result = core
            .dispatch(serde_json::to_string(&CoreIntent::DevSeedPresets).unwrap())
            .await
            .unwrap();
        let state: serde_json::Value = serde_json::from_str(&result.state_json).unwrap();
        let categories = state["categories"].as_array().unwrap();
        assert!(!categories.is_empty(), "清空后再 seed 应恢复分类");
        assert!(state["ui"]["errorMessage"].is_null());
    });
}

/// 生成 Mock 数据后再清空，应彻底清空记录与分类。
#[test]
fn test_dev_generate_then_clear_all() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().expect("测试 Runtime 创建失败");

    rt.block_on(async {
        core.dispatch(
            serde_json::to_string(&CoreIntent::DevGenerateMockData {
                count: 5,
                preset: MockPreset::Normal,
            })
            .unwrap(),
        )
        .await
        .unwrap();

        let result = core
            .dispatch(serde_json::to_string(&CoreIntent::DevClearAllData).unwrap())
            .await
            .unwrap();
        let state: serde_json::Value = serde_json::from_str(&result.state_json).unwrap();

        assert!(state["records"].as_array().unwrap().is_empty());
        assert!(state["categories"].as_array().unwrap().is_empty());
        assert!(state["monthlySummaries"].as_array().unwrap().is_empty());
        assert!(state["currentMonthBreakdown"].as_array().unwrap().is_empty());
        assert!(state["ui"]["errorMessage"].is_null());
    });
}

/// 先后使用 Normal 和 Stress preset 生成数据，记录数应累加。
#[test]
fn test_dev_generate_normal_then_stress() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().expect("测试 Runtime 创建失败");

    rt.block_on(async {
        core.dispatch(
            serde_json::to_string(&CoreIntent::DevGenerateMockData {
                count: 3,
                preset: MockPreset::Normal,
            })
            .unwrap(),
        )
        .await
        .unwrap();

        let result = core
            .dispatch(
                serde_json::to_string(&CoreIntent::DevGenerateMockData {
                    count: 7,
                    preset: MockPreset::Stress,
                })
                .unwrap(),
            )
            .await
            .unwrap();

        let state: serde_json::Value = serde_json::from_str(&result.state_json).unwrap();
        let records = state["records"].as_array().unwrap();
        assert_eq!(
            records.len(),
            10,
            "先后生成 3+7 条，状态中应共 10 条"
        );
        assert!(state["ui"]["errorMessage"].is_null());
    });
}

/// settings_update_currency 应支持多次切换且每次生效。
#[test]
fn test_settings_update_currency_multiple_times() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().expect("测试 Runtime 创建失败");

    rt.block_on(async {
        for symbol in ["$", "€", "£", "¥"] {
            let result = core
                .dispatch(
                    serde_json::to_string(&CoreIntent::SettingsUpdateCurrency {
                        symbol: symbol.to_string(),
                    })
                    .unwrap(),
                )
                .await
                .unwrap();
            let state: serde_json::Value = serde_json::from_str(&result.state_json).unwrap();
            assert!(state["ui"]["errorMessage"].is_null());
            assert_eq!(
                state["settings"]["currencySymbol"].as_str().unwrap(),
                symbol
            );
        }
    });
}

/// settings_update_currency 允许空字符串 symbol（边界行为）。
#[test]
fn test_settings_update_currency_empty_symbol() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().expect("测试 Runtime 创建失败");

    rt.block_on(async {
        let result = core
            .dispatch(
                serde_json::to_string(&CoreIntent::SettingsUpdateCurrency {
                    symbol: "".to_string(),
                })
                .unwrap(),
            )
            .await
            .unwrap();
        let state: serde_json::Value = serde_json::from_str(&result.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null());
        assert_eq!(state["settings"]["currencySymbol"].as_str().unwrap(), "");
    });
}

/// 有记录后查询 monthly_summary 应返回非空数据且金额正确。
#[test]
fn test_stats_monthly_summary_with_records() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().expect("测试 Runtime 创建失败");

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
        let expense_category = state["categories"]
            .as_array()
            .unwrap()
            .iter()
            .find(|c| c["categoryType"] == "expense")
            .unwrap()["id"]
            .as_i64()
            .unwrap();

        let now = chrono::Utc::now().timestamp();
        core.dispatch(
            format!(
                r#"{{"type":"record_create","amount_cents":12345,"record_type":"expense","category_id":{expense_category},"note":"测试","timestamp":{now}}}"#
            ),
        )
        .await
        .unwrap();

        let result = core
            .dispatch(
                serde_json::to_string(&CoreIntent::StatsMonthlySummary { months: 6 }).unwrap(),
            )
            .await
            .unwrap();
        let state: serde_json::Value = serde_json::from_str(&result.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null());
        let summaries = state["monthlySummaries"].as_array().unwrap();
        assert!(!summaries.is_empty());

        let current = summaries.last().unwrap();
        assert_eq!(current["expenseCents"], 12345);
        assert_eq!(current["incomeCents"], 0);
        assert_eq!(current["balanceCents"], -12345);
    });
}

/// aggregate_by_parent=true 在无记录时也应返回空数组。
#[test]
fn test_stats_category_breakdown_aggregate_by_parent_empty() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().expect("测试 Runtime 创建失败");

    rt.block_on(async {
        let year_month = chrono::Local::now().format("%Y-%m").to_string();
        let result = core
            .dispatch(
                serde_json::to_string(&CoreIntent::StatsCategoryBreakdown {
                    year_month,
                    aggregate_by_parent: true,
                })
                .unwrap(),
            )
            .await
            .unwrap();
        let state: serde_json::Value = serde_json::from_str(&result.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null());
        assert!(state["currentMonthBreakdown"].as_array().unwrap().is_empty());
    });
}

/// 有记录后 aggregate_by_parent=true 应正确聚合到父分类。
#[test]
fn test_stats_category_breakdown_with_records_aggregate_by_parent() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().expect("测试 Runtime 创建失败");

    rt.block_on(async {
        let snapshot = core.snapshot_json().await.unwrap();
        let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();

        // 找一级分类"餐饮"及其子分类"早餐"
        let parent = state["categories"]
            .as_array()
            .unwrap()
            .iter()
            .find(|c| c["name"] == "餐饮")
            .expect("应存在'餐饮'分类")
            .clone();
        let parent_id = parent["id"].as_i64().unwrap();

        let child = state["categories"]
            .as_array()
            .unwrap()
            .iter()
            .find(|c| c["name"] == "早餐")
            .expect("应存在'早餐'分类");
        let child_id = child["id"].as_i64().unwrap();

        let now = chrono::Utc::now().timestamp();
        let year_month = chrono::Local::now().format("%Y-%m").to_string();

        // 父分类 200，子分类 50
        core.dispatch(
            format!(
                r#"{{"type":"record_create","amount_cents":20000,"record_type":"expense","category_id":{parent_id},"note":"父","timestamp":{now}}}"#
            ),
        )
        .await
        .unwrap();
        core.dispatch(
            format!(
                r#"{{"type":"record_create","amount_cents":5000,"record_type":"expense","category_id":{child_id},"note":"子","timestamp":{now}}}"#
            ),
        )
        .await
        .unwrap();

        let result = core
            .dispatch(
                serde_json::to_string(&CoreIntent::StatsCategoryBreakdown {
                    year_month: year_month.clone(),
                    aggregate_by_parent: true,
                })
                .unwrap(),
            )
            .await
            .unwrap();
        let state: serde_json::Value = serde_json::from_str(&result.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null());
        let breakdown = state["currentMonthBreakdown"].as_array().unwrap();
        assert_eq!(breakdown.len(), 1, "聚合后应仅一条");
        assert_eq!(breakdown[0]["amountCents"], 25000, "应合并父+子金额");
        assert_eq!(breakdown[0]["categoryId"].as_i64(), Some(parent_id));
        assert_eq!(breakdown[0]["percentage"], 100.0);
    });
}

/// months=1 参数应只返回最近一个月的汇总。
#[test]
fn test_stats_monthly_summary_different_months_param() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().expect("测试 Runtime 创建失败");

    rt.block_on(async {
        let expense_category = {
            let snapshot = core.snapshot_json().await.unwrap();
            let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
            state["categories"]
                .as_array()
                .unwrap()
                .iter()
                .find(|c| c["categoryType"] == "expense")
                .unwrap()["id"]
                .as_i64()
                .unwrap()
        };

        let now = chrono::Utc::now().timestamp();
        core.dispatch(
            format!(
                r#"{{"type":"record_create","amount_cents":1000,"record_type":"expense","category_id":{expense_category},"note":"","timestamp":{now}}}"#
            ),
        )
        .await
        .unwrap();

        let result = core
            .dispatch(
                serde_json::to_string(&CoreIntent::StatsMonthlySummary { months: 1 }).unwrap(),
            )
            .await
            .unwrap();
        let state: serde_json::Value = serde_json::from_str(&result.state_json).unwrap();
        assert!(state["ui"]["errorMessage"].is_null());
        let summaries = state["monthlySummaries"].as_array().unwrap();
        assert!(
            !summaries.is_empty(),
            "months=1 时当前月有记录应返回数据"
        );
        assert_eq!(summaries.last().unwrap()["expenseCents"], 1000);
    });
}

/// 跨月记录只应出现在对应月份的汇总中。
#[test]
fn test_stats_records_only_in_correct_month() {
    let core = common::setup_core_with_presets();
    let rt = tokio::runtime::Runtime::new().expect("测试 Runtime 创建失败");

    rt.block_on(async {
        let expense_category = {
            let snapshot = core.snapshot_json().await.unwrap();
            let state: serde_json::Value = serde_json::from_str(&snapshot).unwrap();
            state["categories"]
                .as_array()
                .unwrap()
                .iter()
                .find(|c| c["categoryType"] == "expense")
                .unwrap()["id"]
                .as_i64()
                .unwrap()
        };

        let now = chrono::Utc::now();
        let this_month = now.format("%Y-%m").to_string();
        // 使用固定 35 天偏移，避免月末 chrono::Months 回退到不同月份的问题
        let last_month_dt = now - chrono::Duration::days(35);
        let last_month = last_month_dt.format("%Y-%m").to_string();

        let this_ts = now.timestamp();
        let last_ts = last_month_dt.timestamp();

        // 上个月记录
        core.dispatch(
            format!(
                r#"{{"type":"record_create","amount_cents":5000,"record_type":"expense","category_id":{expense_category},"note":"上月","timestamp":{last_ts}}}"#
            ),
        )
        .await
        .unwrap();

        // 本月记录
        core.dispatch(
            format!(
                r#"{{"type":"record_create","amount_cents":3000,"record_type":"expense","category_id":{expense_category},"note":"本月","timestamp":{this_ts}}}"#
            ),
        )
        .await
        .unwrap();

        let result = core
            .dispatch(
                serde_json::to_string(&CoreIntent::StatsMonthlySummary { months: 6 }).unwrap(),
            )
            .await
            .unwrap();
        let state: serde_json::Value = serde_json::from_str(&result.state_json).unwrap();
        let summaries = state["monthlySummaries"].as_array().unwrap();

        let this_month_summary = summaries
            .iter()
            .find(|s| s["yearMonth"].as_str() == Some(&this_month));
        let last_month_summary = summaries
            .iter()
            .find(|s| s["yearMonth"].as_str() == Some(&last_month));

        assert!(
            this_month_summary.is_some(),
            "本月应有汇总数据"
        );
        assert!(
            last_month_summary.is_some(),
            "上月应有汇总数据"
        );
        assert_eq!(
            this_month_summary.unwrap()["expenseCents"],
            3000,
            "本月只应含本月记录"
        );
        assert_eq!(
            last_month_summary.unwrap()["expenseCents"],
            5000,
            "上月只应含上月记录"
        );
    });
}
