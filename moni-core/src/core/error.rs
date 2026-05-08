/// 核心错误类型。
#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum CoreError {
    #[error("{0}")]
    Internal(String),
    #[error("数据库错误: {0}")]
    Database(String),
    #[error("参数错误: {0}")]
    InvalidInput(String),
    #[error("记录不存在: id={0}")]
    RecordNotFound(i64),
    #[error("分类不存在: id={0}")]
    CategoryNotFound(i64),
    #[error("分类已被使用，无法删除")]
    CategoryInUse,
    #[error("分类已被归档")]
    CategoryAlreadyArchived,
    #[error("分类未归档")]
    CategoryNotArchived,
    #[error("备份 ZIP 错误: {0}")]
    BackupZipError(String),
    #[error("备份清单无效: {0}")]
    BackupManifestInvalid(String),
    #[error("备份文件损坏: {0}")]
    BackupCorrupted(String),
    #[error("备份版本过新: 要求 format_version={required}, 当前支持={supported}")]
    BackupTooNew { required: u32, supported: u32 },
    #[error("备份恢复失败: 阶段={stage}, 原因={reason}")]
    BackupRestoreFailed { stage: String, reason: String },
    #[error("备份 IO 错误: {0}")]
    BackupIo(String),
}

// region 错误消息常量（用于 error_key 映射，避免魔法字符串）

pub const MSG_CATEGORY_NAME_EMPTY: &str = "分类名称不能为空";
pub const MSG_ICON_NAME_EMPTY: &str = "图标名称不能为空";
pub const MSG_AMOUNT_MUST_BE_POSITIVE: &str = "金额必须大于0";
pub const MSG_BUDGET_AMOUNT_MUST_BE_POSITIVE: &str = "预算金额必须大于0";
pub const MSG_BUDGET_ONLY_EXPENSE: &str = "预算仅支持支出分类";
pub const MSG_CATEGORY_ARCHIVED_FOR_RECORD: &str = "该分类已归档，无法记账";
pub const MSG_RECORD_TYPE_MISMATCH: &str = "记录类型与分类类型不匹配";
pub const MSG_CANNOT_SET_SELF_AS_PARENT: &str = "不能将分类设为自己的父分类";
pub const MSG_HAS_CHILDREN_CANNOT_BE_SUB: &str = "该分类已有子分类，不能设为二级分类";
pub const MSG_REORDER_LIST_EMPTY: &str = "排序列表不能为空";
pub const MSG_ARCHIVED_CANNOT_SORT: &str = "已归档分类不能参与排序";
pub const MSG_DIFFERENT_LEVEL_CANNOT_SORT: &str = "只能对同一层级的分类进行排序";
pub const MSG_PARENT_CATEGORY_ARCHIVED: &str = "父分类已归档";
pub const MSG_PARENT_CHILD_TYPE_MISMATCH: &str = "父分类与子分类类型不一致";
pub const MSG_ONLY_SINGLE_LEVEL: &str = "仅支持单一层级，不能将二级分类设为父分类";
pub const MSG_CATEGORY_IN_USE: &str = "分类已被使用，无法删除";
pub const MSG_DATA_ALREADY_EXISTS: &str = "数据已存在";
pub const MSG_DATABASE_BUSY: &str = "数据库正忙";
pub const MSG_DATABASE_LOCKED: &str = "数据库被锁定";

// endregion

impl CoreError {
    /// 返回用于前端本地化的错误 key。
    pub fn error_key(&self) -> &'static str {
        match self {
            CoreError::Internal(_) => "error_internal",
            CoreError::Database(_) => "error_database",
            CoreError::InvalidInput(msg) => match msg.as_str() {
                MSG_CATEGORY_NAME_EMPTY => "error_category_name_empty",
                MSG_ICON_NAME_EMPTY => "error_icon_name_empty",
                MSG_AMOUNT_MUST_BE_POSITIVE => "error_amount_must_be_positive",
                MSG_BUDGET_AMOUNT_MUST_BE_POSITIVE => "error_budget_amount_must_be_positive",
                MSG_BUDGET_ONLY_EXPENSE => "error_budget_only_expense",
                MSG_CATEGORY_ARCHIVED_FOR_RECORD => "error_category_archived_for_record",
                MSG_RECORD_TYPE_MISMATCH => "error_record_type_mismatch",
                MSG_CANNOT_SET_SELF_AS_PARENT => "error_cannot_set_self_as_parent",
                MSG_HAS_CHILDREN_CANNOT_BE_SUB => "error_has_children_cannot_be_sub",
                MSG_REORDER_LIST_EMPTY => "error_reorder_list_empty",
                MSG_ARCHIVED_CANNOT_SORT => "error_archived_cannot_sort",
                MSG_DIFFERENT_LEVEL_CANNOT_SORT => "error_different_level_cannot_sort",
                MSG_PARENT_CATEGORY_ARCHIVED => "error_parent_category_archived",
                MSG_PARENT_CHILD_TYPE_MISMATCH => "error_parent_child_type_mismatch",
                MSG_ONLY_SINGLE_LEVEL => "error_only_single_level",
                MSG_CATEGORY_IN_USE => "error_category_in_use",
                MSG_DATA_ALREADY_EXISTS => "error_data_already_exists",
                MSG_DATABASE_BUSY => "error_database_busy",
                MSG_DATABASE_LOCKED => "error_database_locked",
                _ => "error_invalid_input",
            },
            CoreError::RecordNotFound(_) => "error_record_not_found",
            CoreError::CategoryNotFound(_) => "error_category_not_found",
            CoreError::CategoryInUse => "error_category_in_use",
            CoreError::CategoryAlreadyArchived => "error_category_already_archived",
            CoreError::CategoryNotArchived => "error_category_not_archived",
            CoreError::BackupZipError(_) => "error_backup_zip",
            CoreError::BackupManifestInvalid(_) => "error_backup_manifest_invalid",
            CoreError::BackupCorrupted(_) => "error_backup_corrupted",
            CoreError::BackupTooNew { .. } => "error_backup_too_new",
            CoreError::BackupRestoreFailed { .. } => "error_backup_restore_failed",
            CoreError::BackupIo(_) => "error_backup_io",
        }
    }

    /// 返回错误 key 对应的格式化参数。
    pub fn error_args(&self) -> Vec<String> {
        match self {
            CoreError::RecordNotFound(id) => vec![id.to_string()],
            CoreError::CategoryNotFound(id) => vec![id.to_string()],
            CoreError::BackupTooNew { required, supported } => {
                vec![required.to_string(), supported.to_string()]
            }
            CoreError::BackupRestoreFailed { stage, reason } => {
                vec![stage.clone(), reason.clone()]
            }
            CoreError::InvalidInput(msg) => {
                // 从 "备注长度不能超过 200 字符" 中提取参数
                if let Some(pos) = msg.find("不能超过 ") {
                    let rest = &msg[pos + 12..];
                    if let Some(end) = rest.find(" 字符") {
                        return vec![rest[..end].to_string()];
                    }
                }
                Vec::new()
            }
            _ => Vec::new(),
        }
    }
}

impl From<rusqlite::Error> for CoreError {
    fn from(err: rusqlite::Error) -> Self {
        match err {
            rusqlite::Error::SqliteFailure(ref sql_err, ref msg) => {
                use rusqlite::ffi::ErrorCode;
                match sql_err.code {
                    ErrorCode::ConstraintViolation => {
                        let detail = msg.as_deref().unwrap_or("约束违反");
                        if detail.contains("FOREIGN KEY") {
                            CoreError::InvalidInput(MSG_CATEGORY_IN_USE.to_string())
                        } else if detail.contains("UNIQUE") {
                            CoreError::InvalidInput(MSG_DATA_ALREADY_EXISTS.to_string())
                        } else {
                            CoreError::InvalidInput(format!("数据约束违反: {detail}"))
                        }
                    }
                    ErrorCode::DatabaseBusy => CoreError::Database(MSG_DATABASE_BUSY.to_string()),
                    ErrorCode::DatabaseLocked => {
                        CoreError::Database(MSG_DATABASE_LOCKED.to_string())
                    }
                    _ => CoreError::Database(err.to_string()),
                }
            }
            rusqlite::Error::InvalidParameterName(name) => {
                CoreError::InvalidInput(format!("无效参数: {name}"))
            }
            _ => CoreError::Database(err.to_string()),
        }
    }
}
