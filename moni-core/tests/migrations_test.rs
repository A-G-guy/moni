use moni_core::db::schema::{init_schema, CURRENT_SCHEMA_VERSION};
use moni_core::domain::backup::migrations::{apply_migrations, migration_registry};

/// 当前 schema_version=1，无破坏性迁移，注册表应为空。
#[test]
fn test_migration_registry_is_empty_for_v1() {
    let registry = migration_registry();
    assert!(
        registry.is_empty(),
        "schema_version=1 时迁移注册表应为空，实际有 {} 条",
        registry.len()
    );
}

/// `from_version >= to_version` 时 `apply_migrations` 应直接成功返回（提前 return）。
#[test]
fn test_apply_migrations_noop_when_from_ge_to() {
    let conn = rusqlite::Connection::open_in_memory().expect("内存数据库创建失败");
    init_schema(&conn).expect("init_schema 应成功");

    // from == to：跳过迁移
    apply_migrations(&conn, 1, 1).expect("from==to 应直接 Ok");
    // from > to：跳过迁移
    apply_migrations(&conn, 2, 1).expect("from>to 应直接 Ok");
    // from > to (大跨度)：跳过迁移
    apply_migrations(&conn, 99, 1).expect("from>to(大跨度) 应直接 Ok");
}

/// `from_version < to_version` 但注册表为空，循环不会执行任何函数，应直接成功。
#[test]
fn test_apply_migrations_with_empty_registry_loops_zero_times() {
    let conn = rusqlite::Connection::open_in_memory().expect("内存数据库创建失败");
    init_schema(&conn).expect("init_schema 应成功");

    apply_migrations(&conn, 0, CURRENT_SCHEMA_VERSION)
        .expect("空注册表 + from<to 应直接 Ok（循环零次）");
    apply_migrations(&conn, 0, 5).expect("跨多个版本但注册表空时应直接 Ok");
}
