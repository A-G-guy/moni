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
}

impl From<rusqlite::Error> for CoreError {
    fn from(err: rusqlite::Error) -> Self {
        CoreError::Database(err.to_string())
    }
}
