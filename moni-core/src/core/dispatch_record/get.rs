use crate::core::error::CoreError;
use crate::core::runtime::AppCoreRuntime;
use crate::db::record_repo;
use crate::models::effects::CoreUpdate;
use crate::models::intent::CoreIntent;

impl AppCoreRuntime {
    pub(super) fn handle_record_get(
        &mut self,
        intent: &CoreIntent,
    ) -> Result<CoreUpdate, CoreError> {
        let &CoreIntent::RecordGet { id } = intent else {
            return Err(CoreError::Internal("意图类型不匹配".to_string()));
        };

        let _ = record_repo::get_by_id(&self.conn, id)?.ok_or(CoreError::RecordNotFound(id))?;
        self.state.ui.selected_record_id = Some(id);
        self.finish(Vec::new())
    }
}
