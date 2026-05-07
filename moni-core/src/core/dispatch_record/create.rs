use crate::core::error::CoreError;
use crate::core::runtime::AppCoreRuntime;
use crate::db::record_repo;
use crate::dto::RecordDto;
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
        let category = crate::db::category_repo::get_by_id(&self.conn, category_id)?
            .ok_or(CoreError::CategoryNotFound(category_id))?;
        if category.archived_at.is_some() {
            log::warn!("创建记录失败: 分类已归档, category_id={category_id}");
            return Err(CoreError::InvalidInput("该分类已归档，无法记账".to_string()));
        }
        if category.category_type != record_type {
            log::warn!(
                "创建记录失败: 记录类型与分类类型不匹配, record_type={record_type:?}, category_type={:?}",
                category.category_type
            );
            return Err(CoreError::InvalidInput(
                "记录类型与分类类型不匹配".to_string(),
            ));
        }

        let id = record_repo::insert(
            &self.conn,
            amount_cents,
            record_type,
            category_id,
            category.parent_id,
            &note,
            timestamp,
        )?;
        let record = record_repo::get_by_id(&self.conn, id)?
            .ok_or_else(|| CoreError::Internal("插入后查询失败".to_string()))?;
        let dto = RecordDto::from_record(&record, &self.state.categories);
        self.state.records.insert(0, dto);
        self.state.record_groups = crate::dto::group_records_by_date(&self.state.records);

        if record_type == moni_contracts::record::RecordType::Expense {
            self.refresh_budget_states(None)?;
        }

        self.finish(vec![CoreEffect {
            kind: "show_snackbar".to_string(),
            payload_json: "{\"message\":\"保存成功\"}".to_string(),
        }])
    }
}
