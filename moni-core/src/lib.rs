use std::sync::Mutex;

use crate::core::AppCoreRuntime;
use crate::core::error::CoreError;
use crate::models::effects::CoreUpdate;
use crate::models::state::AppState;

pub mod core;
pub mod db;
pub mod domain;
pub mod models;
pub mod shared;

pub use models::CoreEffect;

#[derive(uniffi::Object)]
pub struct MoniCore {
    inner: Mutex<AppCoreRuntime>,
}

impl Default for MoniCore {
    fn default() -> Self {
        Self::new()
    }
}

#[uniffi::export]
impl MoniCore {
    #[uniffi::constructor]
    pub fn new() -> Self {
        Self {
            inner: Mutex::new(AppCoreRuntime {
                state: AppState::default(),
                conn: rusqlite::Connection::open_in_memory().expect("内存数据库创建失败"),
            }),
        }
    }

    pub fn initialize(&self) -> Result<CoreUpdate, CoreError> {
        let mut inner = self
            .inner
            .lock()
            .map_err(|_| CoreError::Internal("状态锁已中毒".to_string()))?;
        Ok(inner.initialize())
    }

    pub fn initialize_with_db(&self, db_path: String) -> Result<CoreUpdate, CoreError> {
        let mut inner = self
            .inner
            .lock()
            .map_err(|_| CoreError::Internal("状态锁已中毒".to_string()))?;
        inner.initialize_with_db(&db_path)
    }

    pub fn dispatch(&self, intent_json: String) -> Result<CoreUpdate, CoreError> {
        let intent = serde_json::from_str(&intent_json)
            .map_err(|error| CoreError::Internal(format!("意图解析失败: {error}")))?;
        let mut inner = self
            .inner
            .lock()
            .map_err(|_| CoreError::Internal("状态锁已中毒".to_string()))?;
        Ok(inner.dispatch_intent(intent))
    }

    pub fn snapshot_json(&self) -> Result<String, CoreError> {
        let inner = self
            .inner
            .lock()
            .map_err(|_| CoreError::Internal("状态锁已中毒".to_string()))?;
        serde_json::to_string(&inner.state)
            .map_err(|error| CoreError::Internal(format!("状态序列化失败: {error}")))
    }
}

uniffi::setup_scaffolding!();
