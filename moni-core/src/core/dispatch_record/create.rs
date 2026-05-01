use crate::core::error::CoreError;
use crate::core::runtime::AppCoreRuntime;
use crate::db::record_repo;
use crate::models::effects::CoreEffect;
use crate::models::effects::CoreUpdate;
use crate::models::intent::CoreIntent;

impl AppCoreRuntime {
    pub(super) fn handle_record_create(
        &mut self,
        intent: CoreIntent,
    ) -> Result<CoreUpdate, CoreError> {
        let CoreIntent::RecordCreate {
            amount_cents,
            record_type,
            category_id,
            note,
            timestamp,
        } = intent
        else {
            return Err(CoreError::Internal("意图类型不匹配".to_string()));
        };

        if amount_cents <= 0 {
            log::warn!("创建记录失败: 金额必须大于0, 收到: {amount_cents}");
            return Err(CoreError::InvalidInput("金额必须大于0".to_string()));
        }
        if crate::db::category_repo::get_by_id(&self.conn, category_id)?.is_none() {
            return Err(CoreError::CategoryNotFound(category_id));
        }

        let id = record_repo::insert(
            &self.conn,
            amount_cents,
            record_type,
            category_id,
            &note,
            timestamp,
        )?;
        let record = record_repo::get_by_id(&self.conn, id)?.ok_or_else(|| {
            CoreError::Internal("插入后查询失败".to_string())
        })?;
        self.state.records.insert(0, record);

        self.finish(vec![CoreEffect {
            kind: "show_snackbar".to_string(),
            payload_json: "{\"message\":\"保存成功\"}".to_string(),
        }])
    }
}
