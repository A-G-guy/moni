use crate::core::error::CoreError;
use crate::core::runtime::AppCoreRuntime;
use crate::db::record_repo;
use crate::dto::record_list_to_dto;
use crate::models::effects::CoreUpdate;
use crate::models::intent::CoreIntent;

impl AppCoreRuntime {
    pub(super) fn handle_record_list_by_month(
        &mut self,
        intent: &CoreIntent,
    ) -> Result<CoreUpdate, CoreError> {
        let CoreIntent::RecordListByMonth { year_month } = intent else {
            return Err(CoreError::Internal("意图类型不匹配".to_string()));
        };

        let list = record_repo::list_by_year_month(&self.conn, year_month)?;
        self.state.records = record_list_to_dto(&list, &self.state.categories);
        self.state.record_groups = crate::dto::group_records_by_date(&self.state.records);
        self.finish(Vec::new())
    }
}
