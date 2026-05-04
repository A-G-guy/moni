use rusqlite::Connection;

use crate::models::state::AppState;

/// 核心运行时，持有应用状态和数据库连接。
pub struct AppCoreRuntime {
    pub state: AppState,
    pub conn: Connection,
}

// SQLite Connection 不实现 Debug，这里手写占位实现满足 lint 要求。
impl std::fmt::Debug for AppCoreRuntime {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("AppCoreRuntime")
            .field("state", &self.state)
            .finish_non_exhaustive()
    }
}

impl AppCoreRuntime {
    pub fn finish(
        &self,
        effects: Vec<crate::models::effects::CoreEffect>,
    ) -> Result<crate::models::effects::CoreUpdate, crate::core::error::CoreError> {
        crate::models::effects::CoreUpdate::new(&self.state, effects)
    }
}
