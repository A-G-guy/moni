mod create;
mod delete;
mod get;
mod list;
mod list_by_month;
mod update;

use moni_contracts::category::Category;
use moni_contracts::record::RecordType;

use crate::core::error::CoreError;
use crate::core::runtime::AppCoreRuntime;
use crate::models::effects::CoreUpdate;
use crate::models::intent::CoreIntent;

/// 校验分类是否可用于记账：存在、未归档、类型匹配。
pub fn validate_category_for_record(
    conn: &rusqlite::Connection,
    category_id: i64,
    record_type: RecordType,
) -> Result<Category, CoreError> {
    let category = crate::db::category_repo::get_by_id(conn, category_id)?
        .ok_or(CoreError::CategoryNotFound(category_id))?;
    if category.archived_at.is_some() {
        return Err(CoreError::InvalidInput(
            "该分类已归档，无法记账".to_string(),
        ));
    }
    if category.category_type != record_type {
        return Err(CoreError::InvalidInput(
            "记录类型与分类类型不匹配".to_string(),
        ));
    }
    Ok(category)
}

impl AppCoreRuntime {
    pub(super) fn dispatch_record(&mut self, intent: CoreIntent) -> Result<CoreUpdate, CoreError> {
        match intent {
            CoreIntent::RecordCreate { .. } => self.handle_record_create(intent),
            CoreIntent::RecordUpdate { .. } => self.handle_record_update(intent),
            CoreIntent::RecordDelete { .. } => self.handle_record_delete(&intent),
            CoreIntent::RecordList { .. } => self.handle_record_list(&intent),
            CoreIntent::RecordListByMonth { .. } => self.handle_record_list_by_month(&intent),
            CoreIntent::RecordGet { .. } => self.handle_record_get(&intent),
            _ => {
                log::warn!("记录模块收到未支持的意图类型");
                Err(CoreError::Internal("未支持的意图类型".to_string()))
            }
        }
    }
}
