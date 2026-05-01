use rusqlite::Connection;

use crate::models::state::AppState;

/// 核心运行时，持有应用状态和数据库连接。
pub struct AppCoreRuntime {
    pub state: AppState,
    pub conn: Connection,
}

impl AppCoreRuntime {
    pub fn finish(
        &self,
        effects: Vec<crate::models::effects::CoreEffect>,
    ) -> Result<crate::models::effects::CoreUpdate, crate::core::error::CoreError> {
        crate::models::effects::CoreUpdate::new(&self.state, effects)
    }
}
