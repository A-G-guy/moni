use crate::core::dispatch_record::validate_category_for_record;
use crate::core::error::CoreError;
use crate::core::runtime::AppCoreRuntime;
use crate::db::record_repo;
use crate::dto::RecordDto;
use crate::models::effects::CoreEffect;
use crate::models::effects::CoreUpdate;
use crate::models::intent::CoreIntent;

impl AppCoreRuntime {
    pub(super) fn handle_record_update(
        &mut self,
        intent: CoreIntent,
    ) -> Result<CoreUpdate, CoreError> {
        let CoreIntent::RecordUpdate {
            id,
            amount_cents,
            record_type,
            category_id,
            note,
        } = intent
        else {
            return Err(CoreError::Internal("意图类型不匹配".to_string()));
        };

        if let Some(amt) = amount_cents
            && amt <= 0
        {
            log::warn!("更新记录失败: 金额必须大于0, 收到: {amt}");
            return Err(CoreError::InvalidInput("金额必须大于0".to_string()));
        }
        let original = record_repo::get_by_id(&self.conn, id)?
            .ok_or_else(|| CoreError::RecordNotFound(id))?;

        let new_category = if let Some(cid) = category_id {
            let new_type = record_type.unwrap_or(original.record_type);
            Some(validate_category_for_record(&self.conn, cid, new_type)?)
        } else {
            None
        };

        // 若分类变化，同步更新 parent_category_id
        let parent_category_id = if category_id.is_some() {
            new_category.map(|c| c.parent_id)
        } else {
            None // 不更新
        };

        record_repo::update(
            &self.conn,
            id,
            amount_cents,
            record_type,
            category_id,
            parent_category_id,
            note.as_deref(),
        )?;
        let updated = record_repo::get_by_id(&self.conn, id)?
            .ok_or_else(|| CoreError::Internal(format!("更新后查询记录失败: id={id}")))?;
        let dto = RecordDto::from_record(&updated, &self.state.categories);
        if let Some(idx) = self.state.records.iter().position(|r| r.id == id) {
            self.state.records[idx] = dto;
        }
        self.state.record_groups = crate::dto::group_records_by_date(&self.state.records);

        // 若原记录或新记录为支出，刷新预算状态
        if original.record_type == moni_contracts::record::RecordType::Expense
            || updated.record_type == moni_contracts::record::RecordType::Expense
        {
            self.refresh_budget_states(None)?;
        }

        self.finish(vec![CoreEffect {
            kind: "show_snackbar".to_string(),
            payload_json: "{\"message\":\"更新成功\"}".to_string(),
        }])
    }
}
