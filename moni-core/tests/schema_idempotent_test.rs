use moni_core::db::schema::init_schema;

/// 多次调用 `init_schema` 应幂等：列检测分支应在第二次跳过 ALTER。
#[test]
fn test_init_schema_is_idempotent_across_calls() {
    let conn = rusqlite::Connection::open_in_memory().expect("内存数据库创建失败");

    // 第一次：表与列均不存在，会触发 CREATE 与 ALTER 分支
    init_schema(&conn).expect("首次 init_schema 应成功");

    // 第二次：表已存在 + 三列均已存在，触发 has_xxx != 0 的跳过分支
    init_schema(&conn).expect("再次 init_schema 应成功（幂等）");

    // 第三次：进一步确认无副作用
    init_schema(&conn).expect("第三次 init_schema 应成功");

    // 验证 categories 表存在且包含 description / archived_at / parent_id
    let names: Vec<String> = conn
        .prepare("SELECT name FROM pragma_table_info('categories')")
        .unwrap()
        .query_map([], |row| row.get::<_, String>(0))
        .unwrap()
        .filter_map(Result::ok)
        .collect();
    assert!(names.iter().any(|n| n == "description"), "应存在 description 列");
    assert!(names.iter().any(|n| n == "archived_at"), "应存在 archived_at 列");
    assert!(names.iter().any(|n| n == "parent_id"), "应存在 parent_id 列");

    // description 列应仅有一份（多次 ALTER 不应出现重复）
    let description_count = names.iter().filter(|n| n.as_str() == "description").count();
    assert_eq!(description_count, 1, "description 列应仅出现一次");

    // records 表也应存在
    let table_count: i64 = conn
        .query_row(
            "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='records'",
            [],
            |row| row.get(0),
        )
        .unwrap();
    assert_eq!(table_count, 1, "records 表应存在");
}
