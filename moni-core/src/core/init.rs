use crate::core::runtime::AppCoreRuntime;
use crate::db::schema::init_schema;
use crate::db::category_repo::seed_presets;
use crate::models::effects::CoreUpdate;
use crate::models::state::AppState;

impl AppCoreRuntime {
    /// 使用内存数据库初始化（用于测试/Greet 兼容）。
    pub fn initialize(&mut self) -> CoreUpdate {
        self.state = AppState::default();
        self.finish(Vec::new())
    }

    /// 使用指定路径的数据库初始化。
    pub fn initialize_with_db(&mut self, db_path: &str) -> Result<CoreUpdate, crate::core::error::CoreError> {
        use crate::db::connection::open_connection;
        self.conn = open_connection(db_path)?;
        init_schema(&self.conn)?;
        seed_presets(&self.conn)?;

        // 加载初始数据到状态
        self.state.categories = crate::db::category_repo::list_all(&self.conn)?;

        Ok(self.finish(Vec::new()))
    }
}
