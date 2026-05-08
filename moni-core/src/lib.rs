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

    pub async fn backup_export(
        &self,
        out_zip_path: String,
        settings_json: String,
        app_version_name: String,
        app_version_code: i64,
        device_manufacturer: String,
        device_model: String,
        android_sdk: i32,
        progress: Option<Box<dyn crate::models::backup::BackupProgressListener>>,
    ) -> Result<crate::models::backup::BackupExportReport, CoreError> {
        let inner = Arc::clone(&self.inner);
        self.runtime
            .spawn_blocking(move || {
                let inner = inner
                    .lock()
                    .map_err(|_| CoreError::Internal("状态锁已中毒".to_string()))?;

                let on_progress = |stage: &str, percent: i32| {
                    if let Some(ref p) = progress {
                        p.on_stage(stage.to_string(), percent);
                    }
                };

                let report = crate::domain::backup::exporter::backup_export(
                    &inner.conn,
                    &out_zip_path,
                    &settings_json,
                    &app_version_name,
                    app_version_code,
                    &device_manufacturer,
                    &device_model,
                    android_sdk,
                    Some(&on_progress),
                )?;
                Ok(report)
            })
            .await
            .map_err(|e| CoreError::Internal(format!("任务执行失败: {e}")))?
    }

    pub async fn backup_inspect(
        &self,
        in_zip_path: String,
    ) -> Result<crate::models::backup::BackupInspection, CoreError> {
        self.runtime
            .spawn_blocking(move || {
                let file = std::fs::File::open(&in_zip_path)
                    .map_err(|e| CoreError::BackupIo(format!("打开备份 ZIP 失败: {e}")))?;
                let mut zip = zip::ZipArchive::new(file)
                    .map_err(|e| CoreError::BackupZipError(e.to_string()))?;
                let manifest = crate::domain::backup::manifest::read_manifest(&mut zip)?;
                Ok(crate::models::backup::BackupInspection {
                    format_version: manifest.format_version,
                    schema_version: manifest.schema_version,
                    app_version_name: manifest.app_version_name,
                    app_version_code: manifest.app_version_code,
                    created_at: manifest.created_at,
                    record_count: manifest.stats.record_count,
                    category_count: manifest.stats.category_count,
                    settings_count: manifest.stats.settings_count,
                    total_bytes: std::fs::metadata(&in_zip_path).map(|m| m.len()).unwrap_or(0),
                })
            })
            .await
            .map_err(|e| CoreError::Internal(format!("任务执行失败: {e}")))?
    }

    pub async fn backup_restore(
        &self,
        in_zip_path: String,
        db_path: String,
        progress: Option<Box<dyn crate::models::backup::BackupProgressListener>>,
    ) -> Result<crate::models::backup::BackupRestoreReport, CoreError> {
        let inner = Arc::clone(&self.inner);
        self.runtime
            .spawn_blocking(move || {
                let mut inner = inner
                    .lock()
                    .map_err(|_| CoreError::Internal("状态锁已中毒".to_string()))?;

                // 1. 创建恢复前快照
                let db_parent = std::path::Path::new(&db_path)
                    .parent()
                    .unwrap_or(std::path::Path::new("."));
                let timestamp = chrono::Local::now().format("%Y%m%d_%H%M%S").to_string();
                let snapshot_path = db_parent.join(format!(".pre_restore_{timestamp}.db"));

                // 先关闭当前连接，确保文件句柄释放
                inner.conn = rusqlite::Connection::open_in_memory()
                    .map_err(|e| CoreError::Database(format!("创建内存连接失败: {e}")))?;

                std::fs::copy(&db_path, &snapshot_path)
                    .map_err(|e| CoreError::BackupIo(format!("创建恢复前快照失败: {e}")))?;

                let on_progress = |stage: &str, percent: i32| {
                    if let Some(ref p) = progress {
                        p.on_stage(stage.to_string(), percent);
                    }
                };

                // 2. 执行恢复
                let report = crate::domain::backup::importer::backup_restore(
                    &in_zip_path,
                    &db_path,
                    snapshot_path.to_str(),
                    Some(&on_progress),
                );

                match report {
                    Ok(r) => {
                        // 恢复成功：重新打开连接、全面刷新状态
                        let _ = std::fs::remove_file(&snapshot_path);
                        inner.conn = crate::db::connection::open_connection(&db_path)
                            .map_err(|e| CoreError::Database(format!("重新打开数据库失败: {e}")))?;
                        crate::db::schema::init_schema(&inner.conn)
                            .map_err(|e| CoreError::Database(format!("Schema 初始化失败: {e}")))?;

                        // 全面刷新内存状态
                        let categories = crate::db::category_repo::list_all(&inner.conn)?;
                        inner.state.categories = categories.iter().map(crate::dto::CategoryDto::from_category).collect();

                        let records = crate::db::record_repo::list_paginated(&inner.conn, 0, crate::shared::constants::DEFAULT_PAGE_SIZE)?;
                        inner.state.records = crate::dto::record_list_to_dto(&records, &inner.state.categories);
                        inner.state.record_groups = crate::dto::group_records_by_date(&inner.state.records);

                        // 刷新预算状态
                        let ym = chrono::Utc::now().format("%Y-%m").to_string();
                        let raw_budgets = crate::db::budget_repo::list_for_month(&inner.conn, &ym)?;
                        let today = chrono::Local::now().format("%Y-%m-%d").to_string();
                        let (budget_dtos, _, _) = crate::domain::budget::calculator::build_budget_dtos(
                            &inner.conn,
                            &raw_budgets,
                            &inner.state.categories,
                            &ym,
                            &today,
                        )?;
                        inner.state.budgets = budget_dtos;

                        let aggregates = crate::db::record_repo::monthly_aggregates(&inner.conn, 6)?;
                        inner.state.monthly_summaries = crate::domain::stats::calculator::calculate_monthly_summary(aggregates);

                        let breakdown_aggregates = crate::db::record_repo::category_aggregates(&inner.conn, &ym)?;
                        inner.state.current_month_breakdown =
                            crate::domain::stats::calculator::calculate_category_breakdown(breakdown_aggregates);

                        Ok(r)
                    }
                    Err(e) => {
                        // 恢复失败：从快照回滚（copy 成功后再删快照）
                        match std::fs::copy(&snapshot_path, &db_path) {
                            Ok(_) => {
                                let _ = std::fs::remove_file(&snapshot_path);
                            }
                            Err(copy_err) => {
                                log::error!("回滚时复制快照失败: {copy_err}，保留快照供人工恢复: {}", snapshot_path.display());
                            }
                        }
                        inner.conn = match crate::db::connection::open_connection(&db_path) {
                            Ok(conn) => conn,
                            Err(open_err) => {
                                log::error!("回滚时重新打开数据库失败: {open_err}，尝试内存数据库");
                                match rusqlite::Connection::open_in_memory() {
                                    Ok(mem_conn) => {
                                        let _ = crate::db::schema::init_schema(&mem_conn);
                                        mem_conn
                                    }
                                    Err(mem_err) => {
                                        log::error!("内存数据库创建也失败: {mem_err}");
                                        return Err(CoreError::Database(format!(
                                            "恢复失败且回滚也失败: 原错误={e}, 回滚错误={open_err}, 内存错误={mem_err}"
                                        )));
                                    }
                                }
                            }
                        };
                        let _ = crate::db::schema::init_schema(&inner.conn);
                        Err(e)
                    }
                }
            })
            .await
            .map_err(|e| CoreError::Internal(format!("任务执行失败: {e}")))?
    }

    pub async fn auto_backup_should_run(
        &self,
        last_backup_time: Option<String>,
        frequency: String,
    ) -> Result<bool, CoreError> {
        let freq = crate::models::auto_backup::AutoBackupFrequency::from_str(&frequency)
            .ok_or_else(|| CoreError::Internal(format!("无效的自动备份频率: {frequency}")))?;
        let now = chrono::Local::now().to_rfc3339();
        Ok(crate::domain::auto_backup::should_auto_backup(
            last_backup_time.as_deref(),
            freq,
            &now,
        ))
    }

    pub async fn auto_backup_perform(
        &self,
        backup_dir: String,
        settings_json: String,
        app_version_name: String,
        app_version_code: i64,
        device_manufacturer: String,
        device_model: String,
        android_sdk: i32,
        progress: Option<Box<dyn crate::models::backup::BackupProgressListener>>,
    ) -> Result<crate::models::auto_backup::AutoBackupReport, CoreError> {
        let inner = Arc::clone(&self.inner);
        self.runtime
            .spawn_blocking(move || {
                let inner = inner
                    .lock()
                    .map_err(|_| CoreError::Internal("状态锁已中毒".to_string()))?;

                let on_progress = |stage: &str, percent: i32| {
                    if let Some(ref p) = progress {
                        p.on_stage(stage.to_string(), percent);
                    }
                };

                crate::domain::auto_backup::perform_auto_backup(
                    &inner.conn,
                    &backup_dir,
                    &settings_json,
                    &app_version_name,
                    app_version_code,
                    &device_manufacturer,
                    &device_model,
                    android_sdk,
                    Some(&on_progress),
                )
            })
            .await
            .map_err(|e| CoreError::Internal(format!("任务执行失败: {e}")))?
    }

    pub async fn auto_backup_cleanup(
        &self,
        backup_dir: String,
        max_count: u32,
    ) -> Result<u32, CoreError> {
        self.runtime
            .spawn_blocking(move || {
                crate::domain::auto_backup::cleanup_auto_backups(&backup_dir, max_count)
            })
            .await
            .map_err(|e| CoreError::Internal(format!("任务执行失败: {e}")))?
    }

    // === 纯计算函数（不涉及数据库，同步执行） ===

    /// 解析表达式并返回计算结果（分）。
    pub fn evaluate_expression(&self, expression: String) -> Option<i64> {
        crate::domain::calculator::expression::evaluate_expression(&expression)
    }

    /// 判断表达式是否包含未计算的运算符。
    pub fn has_pending_operation(&self, expression: String) -> bool {
        crate::domain::calculator::expression::has_pending_operation(&expression)
    }

    /// 格式化表达式用于显示，在运算符两侧添加空格。
    pub fn format_expression_for_display(&self, expression: String) -> String {
        crate::domain::calculator::expression::format_for_display(&expression)
    }
}

uniffi::setup_scaffolding!();
