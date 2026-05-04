use std::sync::{Arc, Mutex};

use crate::core::AppCoreRuntime;
use crate::core::error::CoreError;
use crate::models::effects::CoreUpdate;
use crate::models::state::AppState;

pub mod core;
pub mod db;
pub mod domain;
pub mod dto;
pub mod models;
pub mod shared;

pub use models::CoreEffect;

#[cfg(target_os = "android")]
fn init_logging() {
    android_logger::init_once(
        android_logger::Config::default().with_max_level(log::LevelFilter::Debug),
    );
}

#[cfg(not(target_os = "android"))]
fn init_logging() {
    // 非 Android 平台（如测试环境）静默初始化，避免 android_logger 编译问题
}

#[derive(uniffi::Object)]
pub struct MoniCore {
    inner: Arc<Mutex<AppCoreRuntime>>,
    runtime: Arc<tokio::runtime::Runtime>,
}

// 内部包含 SQLite Connection 与 tokio Runtime，二者均不实现 Debug；
// 这里仅暴露占位字符串以满足 lint 要求，避免泄漏内部状态。
impl std::fmt::Debug for MoniCore {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("MoniCore").finish_non_exhaustive()
    }
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
        init_logging();
        let runtime = Arc::new(tokio::runtime::Runtime::new().expect("tokio runtime 创建失败"));
        Self {
            inner: Arc::new(Mutex::new(AppCoreRuntime {
                state: AppState::default(),
                conn: rusqlite::Connection::open_in_memory().expect("内存数据库创建失败"),
            })),
            runtime,
        }
    }

    pub async fn initialize(&self) -> Result<CoreUpdate, CoreError> {
        let inner = Arc::clone(&self.inner);
        self.runtime
            .spawn_blocking(move || {
                let mut inner = inner
                    .lock()
                    .map_err(|_| CoreError::Internal("状态锁已中毒".to_string()))?;
                inner.initialize()
            })
            .await
            .map_err(|e| CoreError::Internal(format!("任务执行失败: {e}")))?
    }

    pub async fn initialize_with_db(&self, db_path: String) -> Result<CoreUpdate, CoreError> {
        let inner = Arc::clone(&self.inner);
        self.runtime
            .spawn_blocking(move || {
                let mut inner = inner
                    .lock()
                    .map_err(|_| CoreError::Internal("状态锁已中毒".to_string()))?;
                inner.initialize_with_db(&db_path)
            })
            .await
            .map_err(|e| CoreError::Internal(format!("任务执行失败: {e}")))?
    }

    pub async fn dispatch(&self, intent_json: String) -> Result<CoreUpdate, CoreError> {
        let inner = Arc::clone(&self.inner);
        self.runtime
            .spawn_blocking(move || {
                let intent = serde_json::from_str(&intent_json)
                    .map_err(|error| CoreError::Internal(format!("意图解析失败: {error}")))?;
                let mut inner = inner
                    .lock()
                    .map_err(|_| CoreError::Internal("状态锁已中毒".to_string()))?;
                Ok(inner.dispatch_intent(intent))
            })
            .await
            .map_err(|e| CoreError::Internal(format!("任务执行失败: {e}")))?
    }

    pub async fn snapshot_json(&self) -> Result<String, CoreError> {
        let inner = Arc::clone(&self.inner);
        self.runtime
            .spawn_blocking(move || {
                let inner = inner
                    .lock()
                    .map_err(|_| CoreError::Internal("状态锁已中毒".to_string()))?;
                serde_json::to_string(&inner.state)
                    .map_err(|error| CoreError::Internal(format!("状态序列化失败: {error}")))
            })
            .await
            .map_err(|e| CoreError::Internal(format!("任务执行失败: {e}")))?
    }
}

uniffi::setup_scaffolding!();
