use crate::core::runtime::AppCoreRuntime;
use crate::db::schema::init_schema;
use crate::dto::CategoryDto;
use crate::models::effects::CoreUpdate;
use crate::models::state::AppState;

impl AppCoreRuntime {
    /// 使用内存数据库初始化（用于文件数据库失败后的回退）。
    pub fn initialize(&mut self) -> Result<CoreUpdate, crate::core::error::CoreError> {
        self.state = AppState::default();
        // 确保使用全新内存连接，避免被之前失败的文件连接污染
        self.conn = rusqlite::Connection::open_in_memory().map_err(|e| {
            crate::core::error::CoreError::Database(format!("内存数据库创建失败: {e}"))
        })?;
        init_schema(&self.conn)?;
        self.finish(Vec::new())
    }

    /// 使用指定路径的数据库初始化。
    pub fn initialize_with_db(
        &mut self,
        db_path: &str,
    ) -> Result<CoreUpdate, crate::core::error::CoreError> {
        use crate::db::connection::open_connection;
        log::info!("初始化数据库: path={db_path}");
        self.conn = open_connection(db_path)?;
        init_schema(&self.conn)?;
        log::info!("数据库初始化完成");

        // 加载初始数据到状态
        let categories = crate::db::category_repo::list_all(&self.conn)?;
        self.state.categories = categories.iter().map(CategoryDto::from_category).collect();

        // 加载持久化设置
        if let Ok(Some(symbol)) = crate::db::settings_repo::get(&self.conn, "currency_symbol") {
            self.state.settings.currency_symbol = symbol;
        }

        self.finish(Vec::new())
    }
}
