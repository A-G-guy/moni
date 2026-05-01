use crate::core::error::CoreError;
use crate::core::runtime::AppCoreRuntime;
use crate::db::category_repo;
use crate::models::effects::CoreUpdate;
use crate::models::intent::CoreIntent;

impl AppCoreRuntime {
    pub(super) fn dispatch_category(
        &mut self,
        intent: CoreIntent,
    ) -> Result<CoreUpdate, CoreError> {
        match intent {
            CoreIntent::CategoryCreate {
                name,
                category_type,
                icon_name,
                color_hex,
            } => {
                if name.trim().is_empty() {
                    return Err(CoreError::InvalidInput("分类名称不能为空".to_string()));
                }
                let id = category_repo::insert(
                    &self.conn,
                    &name,
                    category_type,
                    &icon_name,
                    &color_hex,
                    999, // 自定义分类排在末尾
                    false,
                )?;
                let category = category_repo::get_by_id(&self.conn, id)?
                    .ok_or_else(|| CoreError::Internal("插入后查询失败".to_string()))?;
                self.state.categories.push(category);
                self.state.categories.sort_by(|a, b| a.sort_order.cmp(&b.sort_order));
                Ok(self.finish(vec![crate::models::effects::CoreEffect {
                    kind: "show_snackbar".to_string(),
                    payload_json: r#"{"message":"分类添加成功"}"#.to_string(),
                }]))
            }
            CoreIntent::CategoryDelete { id } => {
                if category_repo::get_by_id(&self.conn, id)?.is_none() {
                    return Err(CoreError::CategoryNotFound(id));
                }
                if category_repo::is_in_use(&self.conn, id)? {
                    return Err(CoreError::CategoryInUse);
                }
                let affected = category_repo::delete(&self.conn, id)?;
                if affected == 0 {
                    return Err(CoreError::InvalidInput("预设分类不可删除".to_string()));
                }
                self.state.categories.retain(|c| c.id != id);
                Ok(self.finish(vec![crate::models::effects::CoreEffect {
                    kind: "show_snackbar".to_string(),
                    payload_json: r#"{"message":"分类删除成功"}"#.to_string(),
                }]))
            }
            CoreIntent::CategoryList => {
                self.state.categories = category_repo::list_all(&self.conn)?;
                Ok(self.finish(Vec::new()))
            }
            _ => unreachable!(),
        }
    }
}
