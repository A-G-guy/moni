use rusqlite::Connection;

/// Schema 迁移函数签名。
pub type MigrationFn = fn(&Connection) -> Result<(), crate::core::error::CoreError>;

/// 迁移注册表：从旧版本映射到迁移函数。
/// 当前 schema_version = 1，尚无破坏性迁移。
/// 未来出现破坏性变更时在此注册 `migrate_v1_to_v2`、`migrate_v2_to_v3` 等。
#[must_use]
pub fn migration_registry() -> Vec<(u32, u32, MigrationFn)> {
    vec![]
}

/// 对指定连接执行从 `from_version` 到 `to_version` 的逐级迁移。
/// `to_version` 必须是 `CURRENT_SCHEMA_VERSION`（来自 schema.rs）。
pub fn apply_migrations(
    conn: &Connection,
    from_version: u32,
    to_version: u32,
) -> Result<(), crate::core::error::CoreError> {
    if from_version >= to_version {
        return Ok(());
    }

    let registry = migration_registry();

    for (src, dst, func) in registry {
        if from_version <= src && dst <= to_version {
            func(conn)?;
        }
    }

    Ok(())
}
