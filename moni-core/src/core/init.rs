use crate::core::runtime::AppCoreRuntime;
use crate::db::schema::init_schema;
use crate::db::category_repo::seed_presets;
use crate::models::effects::CoreUpdate;
use crate::models::state::AppState;

impl AppCoreRuntime {
    /// 使用内存数据库初始化（用于测试兼容）。
    pub fn initialize(&mut self) -> Result<CoreUpdate, crate::core::error::CoreError> {
        self.state = AppState::default();
        init_schema(&self.conn)?;
        self.finish(Vec::new())
    }

    /// 使用指定路径的数据库初始化。
    pub fn initialize_with_db(&mut self, db_path: &str) -> Result<CoreUpdate, crate::core::error::CoreError> {
        use crate::db::connection::open_connection;
        log::info!("初始化数据库: path={db_path}");
        self.conn = open_connection(db_path)?;
        init_schema(&self.conn)?;
        seed_presets(&self.conn)?;
        log::info!("数据库初始化完成");

        // 加载初始数据到状态
        self.state.categories = crate::db::category_repo::list_all(&self.conn)?;

        self.finish(Vec::new())
    }
}
