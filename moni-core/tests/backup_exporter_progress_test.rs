mod common;

use std::sync::{Arc, Mutex};

/// 测试辅助：在指定连接中插入一个分类并返回其 ID。
fn insert_category(
    conn: &rusqlite::Connection,
    name: &str,
    category_type: &str,
    icon: &str,
    parent_id: Option<i64>,
) -> i64 {
    conn.execute(
        "INSERT INTO categories (name, description, category_type, icon_name, sort_order, is_preset, parent_id) VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)",
        rusqlite::params![
            name,
            "",
            category_type,
            icon,
            1,
            0,
            parent_id
        ],
    )
    .unwrap();
    conn.last_insert_rowid()
}

/// 直接调用 exporter::backup_export 并传入 on_progress = None，
/// 覆盖 exporter.rs 中所有 `if let Some(cb)` 的 None 分支。
/// 同时验证生成的 ZIP 包含预期条目且 manifest 可被正常读取。
#[test]
fn test_exporter_no_listener_produces_valid_zip() {
    let tmp_dir = tempfile::TempDir::new().unwrap();
    let out_zip = tmp_dir.path().join("no_listener.zip");

    let conn = rusqlite::Connection::open_in_memory().unwrap();
    moni_core::db::schema::init_schema(&conn).unwrap();

    // 插入分类与记录
    let cat_id = insert_category(&conn, "餐饮", "expense", "restaurant", None);
    let _sub_id = insert_category(&conn, "午餐", "expense", "lunch", Some(cat_id));

    for i in 0..5 {
        conn.execute(
            "INSERT INTO records (amount_cents, record_type, category_id, note, created_at) VALUES (?1, ?2, ?3, ?4, ?5)",
            rusqlite::params![
                1000 + i * 100,
                "expense",
                cat_id,
                format!("记录-{i}"),
                chrono::Local::now().timestamp() - i * 3600
            ],
        )
        .unwrap();
    }

    let report = moni_core::domain::backup::exporter::backup_export(
        &conn,
        out_zip.to_str().unwrap(),
        r#"{"schema":1,"currency_symbol":"¥","theme_mode":"system","dynamic_color":false,"seed_color":4284612842}"#,
        "0.1.0-no-listener",
        1,
        "TestManufacturer",
        "TestModel",
        36,
        None,
    )
    .expect("无监听器导出应成功");

    assert!(out_zip.exists(), "ZIP 文件应已生成");
    assert!(report.total_bytes > 0, "total_bytes 应大于 0");
    assert_eq!(report.record_count, 5, "应导出 5 条记录");
    assert_eq!(report.category_count, 2, "应导出 2 个分类");

    // 验证 ZIP 内部结构
    let file = std::fs::File::open(&out_zip).unwrap();
    let mut zip = zip::ZipArchive::new(file).unwrap();
    assert!(zip.by_name("db/moni.db").is_ok(), "ZIP 应含 db/moni.db");
    assert!(
        zip.by_name("settings/preferences.json").is_ok(),
        "ZIP 应含 settings/preferences.json"
    );
    assert!(zip.by_name("README.md").is_ok(), "ZIP 应含 README.md");
    assert!(
        zip.by_name("manifest.json").is_ok(),
        "ZIP 应含 manifest.json"
    );

    // 验证 manifest 可读且字段正确
    let manifest = moni_core::domain::backup::manifest::read_manifest(&mut zip).unwrap();
    assert_eq!(manifest.format_version, 1);
    assert_eq!(manifest.app_version_name, "0.1.0-no-listener");
    assert_eq!(manifest.stats.record_count, 5);
    assert_eq!(manifest.stats.category_count, 2);
}

/// 大数据量导出：创建大量分类（含二级分类）与记录，
/// 确保 exporter 所有进度阶段在 on_progress = None 时也能正常走完。
#[test]
fn test_exporter_no_listener_large_dataset() {
    let tmp_dir = tempfile::TempDir::new().unwrap();
    let out_zip = tmp_dir.path().join("large.zip");

    let conn = rusqlite::Connection::open_in_memory().unwrap();
    moni_core::db::schema::init_schema(&conn).unwrap();

    // 创建 20 个一级分类，每个带 2 个子分类
    for i in 0..20 {
        let parent_id = insert_category(
            &conn,
            &format!("一级-{i}"),
            if i % 2 == 0 { "expense" } else { "income" },
            "icon",
            None,
        );
        for j in 0..2 {
            insert_category(
                &conn,
                &format!("子-{i}-{j}"),
                "expense",
                "icon",
                Some(parent_id),
            );
        }
    }

    // 创建 100 条记录，分散到不同分类
    let cat_ids: Vec<i64> = conn
        .prepare("SELECT id FROM categories WHERE parent_id IS NOT NULL")
        .unwrap()
        .query_map([], |row| row.get(0))
        .unwrap()
        .collect::<Result<Vec<_>, _>>()
        .unwrap();

    for i in 0..100 {
        let cat_id = cat_ids[i % cat_ids.len()];
        conn.execute(
            "INSERT INTO records (amount_cents, record_type, category_id, note, created_at) VALUES (?1, ?2, ?3, ?4, ?5)",
            rusqlite::params![
                500 + (i as i64) * 10,
                "expense",
                cat_id,
                format!("大数据记录-{i}"),
                chrono::Local::now().timestamp() - (i as i64) * 60
            ],
        )
        .unwrap();
    }

    let report = moni_core::domain::backup::exporter::backup_export(
        &conn,
        out_zip.to_str().unwrap(),
        r#"{"schema":1}"#,
        "1.0.0",
        100,
        "LargeVendor",
        "LargeModel",
        34,
        None,
    )
    .expect("大数据量导出应成功");

    assert_eq!(report.record_count, 100);
    assert_eq!(report.category_count, 60); // 20 一级 + 40 子分类
    assert!(report.total_bytes > 0);
    assert!(out_zip.exists());

    // 验证 ZIP 条目完整性
    let file = std::fs::File::open(&out_zip).unwrap();
    let mut zip = zip::ZipArchive::new(file).unwrap();
    assert!(
        zip.by_name("manifest.json").is_ok(),
        "大数据量 ZIP 也应含 manifest.json"
    );
}

/// 导出到不存在的子目录应返回 BackupIo 错误，且错误文案包含“创建 ZIP 失败”。
#[test]
fn test_exporter_no_listener_nonexistent_dir_errors() {
    let tmp_dir = tempfile::TempDir::new().unwrap();
    let nonexistent = tmp_dir.path().join("不存在的子目录").join("x.zip");

    let conn = rusqlite::Connection::open_in_memory().unwrap();
    moni_core::db::schema::init_schema(&conn).unwrap();

    let result = moni_core::domain::backup::exporter::backup_export(
        &conn,
        nonexistent.to_str().unwrap(),
        r#"{}"#,
        "0.1.0",
        1,
        "Test",
        "Test",
        30,
        None,
    );

    assert!(result.is_err(), "不存在的目录应导致导出失败");
    let err_msg = format!("{:?}", result.unwrap_err());
    assert!(
        err_msg.contains("创建 ZIP 失败") || err_msg.contains("BackupIo"),
        "错误应提示创建 ZIP 失败，实际: {err_msg}"
    );
}

/// 导出到只读目录应失败。
#[test]
fn test_exporter_no_listener_readonly_dir_errors() {
    let tmp_dir = tempfile::TempDir::new().unwrap();
    let readonly_zip = tmp_dir.path().join("readonly");
    std::fs::create_dir(&readonly_zip).unwrap();

    // 设置目录为只读（owner 无写权限）
    let mut perms = std::fs::metadata(&readonly_zip).unwrap().permissions();
    perms.set_readonly(true);
    std::fs::set_permissions(&readonly_zip, perms).unwrap();

    let out_path = readonly_zip.join("test.zip");

    let conn = rusqlite::Connection::open_in_memory().unwrap();
    moni_core::db::schema::init_schema(&conn).unwrap();

    let result = moni_core::domain::backup::exporter::backup_export(
        &conn,
        out_path.to_str().unwrap(),
        r#"{}"#,
        "0.1.0",
        1,
        "Test",
        "Test",
        30,
        None,
    );

    // 恢复权限以便 TempDir 能自动清理
    let mut perms = std::fs::metadata(&readonly_zip).unwrap().permissions();
    perms.set_readonly(false);
    let _ = std::fs::set_permissions(&readonly_zip, perms);

    assert!(result.is_err(), "只读目录应导致导出失败");
    let err_msg = format!("{:?}", result.unwrap_err());
    assert!(
        err_msg.contains("创建 ZIP 失败") || err_msg.contains("BackupIo"),
        "错误应来自 I/O，实际: {err_msg}"
    );
}

/// 重复导出到同一路径应覆盖原文件，而非追加。
#[test]
fn test_exporter_no_listener_overwrite_same_path() {
    let tmp_dir = tempfile::TempDir::new().unwrap();
    let out_zip = tmp_dir.path().join("overwrite.zip");

    let conn = rusqlite::Connection::open_in_memory().unwrap();
    moni_core::db::schema::init_schema(&conn).unwrap();

    // 第一次导出：空数据库
    let report1 = moni_core::domain::backup::exporter::backup_export(
        &conn,
        out_zip.to_str().unwrap(),
        r#"{"schema":1}"#,
        "0.1.0",
        1,
        "Test",
        "Test",
        30,
        None,
    )
    .unwrap();

    let size1 = report1.total_bytes;

    // 插入数据后第二次导出到同一路径
    let cat_id = insert_category(&conn, "交通", "expense", "bus", None);
    conn.execute(
        "INSERT INTO records (amount_cents, record_type, category_id, note, created_at) VALUES (?1, ?2, ?3, ?4, ?5)",
        rusqlite::params![2000, "expense", cat_id, "地铁", chrono::Local::now().timestamp()],
    )
    .unwrap();

    let report2 = moni_core::domain::backup::exporter::backup_export(
        &conn,
        out_zip.to_str().unwrap(),
        r#"{"schema":1}"#,
        "0.1.0",
        1,
        "Test",
        "Test",
        30,
        None,
    )
    .unwrap();

    let size2 = report2.total_bytes;
    assert!(
        size2 > size1,
        "覆盖后文件应更大（含数据），size1={size1}, size2={size2}"
    );

    // 验证 ZIP 不是损坏的追加文件：应能正常读取 manifest
    let file = std::fs::File::open(&out_zip).unwrap();
    let mut zip = zip::ZipArchive::new(file).unwrap();
    let manifest = moni_core::domain::backup::manifest::read_manifest(&mut zip).unwrap();
    assert_eq!(manifest.stats.record_count, 1);
    assert_eq!(manifest.stats.category_count, 1);
}

/// 无监听器导出后再导入，验证数据行数与内容等价。
#[test]
fn test_exporter_no_listener_export_then_import_matches() {
    let tmp_dir = tempfile::TempDir::new().unwrap();
    let out_zip = tmp_dir.path().join("roundtrip.zip");

    let conn = rusqlite::Connection::open_in_memory().unwrap();
    moni_core::db::schema::init_schema(&conn).unwrap();

    // 填充数据
    let cat_id = insert_category(&conn, "工资", "income", "money", None);
    for i in 0..10 {
        conn.execute(
            "INSERT INTO records (amount_cents, record_type, category_id, note, created_at) VALUES (?1, ?2, ?3, ?4, ?5)",
            rusqlite::params![
                10000 + i * 1000,
                "income",
                cat_id,
                format!("工资-{i}"),
                chrono::Local::now().timestamp() - i * 86400
            ],
        )
        .unwrap();
    }

    // 导出
    let report = moni_core::domain::backup::exporter::backup_export(
        &conn,
        out_zip.to_str().unwrap(),
        r#"{"schema":1,"currency_symbol":"¥"}"#,
        "1.0.0-roundtrip",
        10,
        "RoundTrip",
        "Device",
        35,
        None,
    )
    .unwrap();

    assert_eq!(report.record_count, 10);
    assert_eq!(report.category_count, 1);

    // 导入到新的数据库
    let target_db = tmp_dir.path().join("target.db");
    {
        let target_conn = rusqlite::Connection::open(&target_db).unwrap();
        moni_core::db::schema::init_schema(&target_conn).unwrap();
    }

    let core = common::setup_core();
    let rt = tokio::runtime::Runtime::new().unwrap();
    let restore_report = rt
        .block_on(async {
            core.backup_restore(
                out_zip.to_str().unwrap().to_string(),
                target_db.to_str().unwrap().to_string(),
                None,
            )
            .await
        })
        .unwrap();

    assert_eq!(restore_report.restored_record_count, 10);
    assert_eq!(restore_report.restored_category_count, 1);

    // 验证目标数据库行数
    let target_conn = rusqlite::Connection::open(&target_db).unwrap();
    let record_count: i64 = target_conn
        .query_row("SELECT COUNT(*) FROM records", [], |row| row.get(0))
        .unwrap();
    let category_count: i64 = target_conn
        .query_row("SELECT COUNT(*) FROM categories", [], |row| row.get(0))
        .unwrap();
    assert_eq!(record_count, 10);
    assert_eq!(category_count, 1);

    // 验证具体数据内容
    let amount: i64 = target_conn
        .query_row(
            "SELECT amount_cents FROM records WHERE note = ?1",
            ["工资-5"],
            |row| row.get(0),
        )
        .unwrap();
    assert_eq!(amount, 15000);
}

/// 验证 exporter 在传入 Some(listener) 时进度回调被触发，
/// 与 None 分支形成互补覆盖。
#[test]
fn test_exporter_with_listener_receives_callbacks() {
    let tmp_dir = tempfile::TempDir::new().unwrap();
    let out_zip = tmp_dir.path().join("with_listener.zip");

    let conn = rusqlite::Connection::open_in_memory().unwrap();
    moni_core::db::schema::init_schema(&conn).unwrap();
    insert_category(&conn, "测试", "expense", "test", None);

    let log: Arc<Mutex<Vec<(String, i32)>>> = Arc::new(Mutex::new(Vec::new()));
    let log_clone = Arc::clone(&log);
    let on_progress = |stage: &str, percent: i32| {
        log_clone.lock().unwrap().push((stage.to_string(), percent));
    };

    let report = moni_core::domain::backup::exporter::backup_export(
        &conn,
        out_zip.to_str().unwrap(),
        r#"{}"#,
        "0.1.0",
        1,
        "Test",
        "Test",
        30,
        Some(&on_progress),
    )
    .unwrap();

    assert!(report.total_bytes > 0);

    let captured = log.lock().unwrap().clone();
    assert!(!captured.is_empty(), "应至少收到一次进度回调");
    assert!(
        captured.iter().any(|(_, p)| *p == 100),
        "最终 percent 应到达 100，实际: {captured:?}"
    );

    // 验证各阶段都被触发
    let stages: Vec<String> = captured.iter().map(|(s, _)| s.clone()).collect();
    assert!(stages.iter().any(|s| s.contains("统计")), "应包含统计阶段");
    assert!(
        stages.iter().any(|s| s.contains("副本")),
        "应包含创建副本阶段"
    );
    assert!(stages.iter().any(|s| s.contains("打包")), "应包含打包阶段");
    assert!(
        stages.iter().any(|s| s.contains("清单")),
        "应包含生成清单阶段"
    );
    assert!(stages.iter().any(|s| s.contains("完成")), "应包含完成阶段");
}

/// 验证 build_manifest 在正常场景下成功生成合法 JSON，
/// 且 manifest 的 sha256 自校验通过。
/// （build_manifest 内部错误路径依赖极端序列化异常，
///  在现有类型系统下几乎无法从外部触发，故以正向验证为主。）
#[test]
fn test_exporter_manifest_integrity_self_verifies() {
    let tmp_dir = tempfile::TempDir::new().unwrap();
    let out_zip = tmp_dir.path().join("manifest_integrity.zip");

    let conn = rusqlite::Connection::open_in_memory().unwrap();
    moni_core::db::schema::init_schema(&conn).unwrap();
    let cat_id = insert_category(&conn, "测试分类", "expense", "test", None);
    conn.execute(
        "INSERT INTO records (amount_cents, record_type, category_id, note, created_at) VALUES (?1, ?2, ?3, ?4, ?5)",
        rusqlite::params![999, "expense", cat_id, "manifest 测试", chrono::Local::now().timestamp()],
    )
    .unwrap();

    moni_core::domain::backup::exporter::backup_export(
        &conn,
        out_zip.to_str().unwrap(),
        r#"{"schema":1}"#,
        "1.0.0-manifest",
        99,
        "ManifestVendor",
        "ManifestModel",
        33,
        None,
    )
    .unwrap();

    let file = std::fs::File::open(&out_zip).unwrap();
    let mut zip = zip::ZipArchive::new(file).unwrap();
    let manifest = moni_core::domain::backup::manifest::read_manifest(&mut zip).unwrap();

    // 自校验应通过
    moni_core::domain::backup::manifest::verify_manifest_integrity(&manifest)
        .expect("manifest 自校验应通过");

    assert_eq!(manifest.app_version_code, 99);
    assert_eq!(manifest.device.manufacturer, "ManifestVendor");
    assert!(!manifest.manifest_sha256.is_empty());
}
