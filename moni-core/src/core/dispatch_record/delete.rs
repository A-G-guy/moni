use crate::core::error::CoreError;
use crate::core::runtime::AppCoreRuntime;
use crate::db::record_repo;
use crate::models::effects::CoreEffect;
use crate::models::effects::CoreUpdate;
use crate::models::intent::CoreIntent;

impl AppCoreRuntime {
    pub(super) fn handle_record_delete(
        &mut self,
        intent: CoreIntent,
    ) -> Result<CoreUpdate, CoreError> {
        let CoreIntent::RecordDelete { id } = intent else {
            return Err(CoreError::Internal("意图类型不匹配".to_string()));
        };

        if record_repo::get_by_id(&self.conn, id)?.is_none() {
            return Err(CoreError::RecordNotFound(id));
        }
        record_repo::delete(&self.conn, id)?;
        log::info!("记录删除成功: id={id}");
        self.state.records.retain(|r| r.id != id);
        self.state.record_groups =
            crate::dto::group_records_by_date(&self.state.records);

        self.finish(vec![CoreEffect {
            kind: "show_snackbar".to_string(),
            payload_json: "{\"message\":\"删除成功\"}".to_string(),
        }])
    }
}
