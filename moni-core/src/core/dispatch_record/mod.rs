mod create;
mod delete;
mod get;
mod list;
mod update;

use crate::core::error::CoreError;
use crate::core::runtime::AppCoreRuntime;
use crate::models::effects::CoreUpdate;
use crate::models::intent::CoreIntent;

impl AppCoreRuntime {
    pub(super) fn dispatch_record(&mut self, intent: CoreIntent) -> Result<CoreUpdate, CoreError> {
        match intent {
            CoreIntent::RecordCreate { .. } => self.handle_record_create(intent),
            CoreIntent::RecordUpdate { .. } => self.handle_record_update(intent),
            CoreIntent::RecordDelete { .. } => self.handle_record_delete(&intent),
            CoreIntent::RecordList { .. } => self.handle_record_list(&intent),
            CoreIntent::RecordGet { .. } => self.handle_record_get(&intent),
            _ => {
                log::warn!("记录模块收到未支持的意图类型");
                Err(CoreError::Internal("未支持的意图类型".to_string()))
            }
        }
    }
}
