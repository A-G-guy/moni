use crate::core::error::CoreError;
use crate::core::runtime::AppCoreRuntime;
use crate::db::record_repo;
use crate::models::effects::CoreUpdate;
use crate::models::intent::CoreIntent;

impl AppCoreRuntime {
    pub(super) fn dispatch_record(
        &mut self,
        intent: CoreIntent,
    ) -> Result<CoreUpdate, CoreError> {
        match intent {
            CoreIntent::RecordCreate {
                amount_cents,
                record_type,
                category_id,
                note,
                timestamp,
            } => {
                if amount_cents <= 0 {
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
                Ok(self.finish(vec![crate::models::effects::CoreEffect {
                    kind: "show_snackbar".to_string(),
                    payload_json: "{\"message\":\"保存成功\"}".to_string(),
                }]))
            }
            CoreIntent::RecordUpdate {
                id,
                amount_cents,
                record_type,
                category_id,
                note,
            } => {
                if record_repo::get_by_id(&self.conn, id)?.is_none() {
                    return Err(CoreError::RecordNotFound(id));
                }
                if let Some(cid) = category_id {
                    if crate::db::category_repo::get_by_id(&self.conn, cid)?.is_none() {
                        return Err(CoreError::CategoryNotFound(cid));
                    }
                }
                record_repo::update(
                    &self.conn,
                    id,
                    amount_cents,
                    record_type,
                    category_id,
                    note.as_deref(),
                )?;
                if let Some(idx) = self.state.records.iter().position(|r| r.id == id) {
                    self.state.records[idx] = record_repo::get_by_id(&self.conn, id)?.unwrap();
                }
                Ok(self.finish(vec![crate::models::effects::CoreEffect {
                    kind: "show_snackbar".to_string(),
                    payload_json: "{\"message\":\"更新成功\"}".to_string(),
                }]))
            }
            CoreIntent::RecordDelete { id } => {
                if record_repo::get_by_id(&self.conn, id)?.is_none() {
                    return Err(CoreError::RecordNotFound(id));
                }
                record_repo::delete(&self.conn, id)?;
                self.state.records.retain(|r| r.id != id);
                Ok(self.finish(vec![crate::models::effects::CoreEffect {
                    kind: "show_snackbar".to_string(),
                    payload_json: "{\"message\":\"删除成功\"}".to_string(),
                }]))
            }
            CoreIntent::RecordList { page, page_size } => {
                let list = record_repo::list_paginated(&self.conn, page, page_size)?;
                self.state.records = list;
                Ok(self.finish(Vec::new()))
            }
            CoreIntent::RecordGet { id } => {
                let _ = record_repo::get_by_id(&self.conn, id)?.ok_or(CoreError::RecordNotFound(id))?;
                self.state.ui.selected_record_id = Some(id);
                Ok(self.finish(Vec::new()))
            }
            _ => unreachable!(),
        }
    }
}
