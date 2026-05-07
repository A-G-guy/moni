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

impl From<rusqlite::Error> for CoreError {
    fn from(err: rusqlite::Error) -> Self {
        match err {
            rusqlite::Error::SqliteFailure(ref sql_err, ref msg) => {
                use rusqlite::ffi::ErrorCode;
                match sql_err.code {
                    ErrorCode::ConstraintViolation => {
                        let detail = msg.as_deref().unwrap_or("约束违反");
                        if detail.contains("FOREIGN KEY") {
                            CoreError::InvalidInput("分类已被使用，无法删除".to_string())
                        } else if detail.contains("UNIQUE") {
                            CoreError::InvalidInput("数据已存在".to_string())
                        } else {
                            CoreError::InvalidInput(format!("数据约束违反: {detail}"))
                        }
                    }
                    ErrorCode::DatabaseBusy => CoreError::Database("数据库正忙".to_string()),
                    ErrorCode::DatabaseLocked => CoreError::Database("数据库被锁定".to_string()),
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
