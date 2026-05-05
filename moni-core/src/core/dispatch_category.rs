use moni_contracts::record::RecordType;

use crate::core::error::CoreError;
use crate::core::runtime::AppCoreRuntime;
use crate::db::category_repo;
use crate::dto::CategoryDto;
use crate::models::effects::{CoreEffect, CoreUpdate};
use crate::models::intent::CoreIntent;

impl AppCoreRuntime {
    pub(super) fn dispatch_category(
        &mut self,
        intent: CoreIntent,
    ) -> Result<CoreUpdate, CoreError> {
        match intent {
            CoreIntent::CategoryCreate {
                name,
                description,
                category_type,
                icon_name,
            } => self.handle_category_create(
                &name,
                description.as_deref(),
                category_type,
                &icon_name,
            ),
            CoreIntent::CategoryUpdate {
                id,
                name,
                description,
                icon_name,
            } => self.handle_category_update(
                id,
                name.as_deref(),
                description.as_deref(),
                icon_name.as_deref(),
            ),
            CoreIntent::CategoryArchive { id } => self.handle_category_archive(id),
            CoreIntent::CategoryUnarchive { id } => self.handle_category_unarchive(id),
            CoreIntent::CategoryList => self.handle_category_list(),
            _ => {
                log::warn!("分类模块收到未支持的意图类型");
                Err(CoreError::Internal("未支持的意图类型".to_string()))
            }
        }
    }

    fn handle_category_create(
        &mut self,
        name: &str,
        description: Option<&str>,
        category_type: RecordType,
        icon_name: &str,
    ) -> Result<CoreUpdate, CoreError> {
        let trimmed_name = name.trim();
        if trimmed_name.is_empty() {
            log::warn!("创建分类失败: 名称为空");
            return Err(CoreError::InvalidInput("分类名称不能为空".to_string()));
        }
        if icon_name.trim().is_empty() {
            log::warn!("创建分类失败: 图标名称为空");
            return Err(CoreError::InvalidInput("图标名称不能为空".to_string()));
        }
        validate_description_len(description)?;

        let id = category_repo::insert(
            &self.conn,
            trimmed_name,
            description,
            category_type,
            icon_name,
            crate::shared::constants::CUSTOM_CATEGORY_SORT_ORDER,
            false,
        )?;
        let category = category_repo::get_by_id(&self.conn, id)?
            .ok_or_else(|| CoreError::Internal("插入后查询失败".to_string()))?;
        let dto = CategoryDto::from_category(&category);
        self.state.categories.push(dto);
        self.state
            .categories
            .sort_by(|a, b| a.sort_order.cmp(&b.sort_order));
        self.finish(vec![CoreEffect {
            kind: "show_snackbar".to_string(),
            payload_json: r#"{"message":"分类添加成功"}"#.to_string(),
        }])
    }

    fn handle_category_update(
        &mut self,
        id: i64,
        name: Option<&str>,
        description: Option<&str>,
        icon_name: Option<&str>,
    ) -> Result<CoreUpdate, CoreError> {
        let _category =
            category_repo::get_by_id(&self.conn, id)?.ok_or(CoreError::CategoryNotFound(id))?;
        if let Some(n) = name
            && n.trim().is_empty()
        {
            log::warn!("更新分类失败: 名称为空, id={id}");
            return Err(CoreError::InvalidInput("分类名称不能为空".to_string()));
        }
        validate_description_len(description)?;
        if let Some(icon) = icon_name
            && icon.trim().is_empty()
        {
            log::warn!("更新分类失败: 图标名称为空, id={id}");
            return Err(CoreError::InvalidInput("图标名称不能为空".to_string()));
        }
        let affected = category_repo::update(
            &self.conn,
            id,
            name.map(str::trim),
            description,
            icon_name.map(str::trim),
        )?;
        if affected == 0 {
            return Err(CoreError::CategoryNotFound(id));
        }
        // 刷新内存态
        let updated = category_repo::get_by_id(&self.conn, id)?
            .ok_or_else(|| CoreError::Internal("更新后查询失败".to_string()))?;
        let dto = CategoryDto::from_category(&updated);
        if let Some(idx) = self.state.categories.iter().position(|c| c.id == id) {
            self.state.categories[idx] = dto;
        }
        self.finish(vec![CoreEffect {
            kind: "show_snackbar".to_string(),
            payload_json: r#"{"message":"分类更新成功"}"#.to_string(),
        }])
    }

    fn handle_category_archive(&mut self, id: i64) -> Result<CoreUpdate, CoreError> {
        let category =
            category_repo::get_by_id(&self.conn, id)?.ok_or(CoreError::CategoryNotFound(id))?;
        if category.archived_at.is_some() {
            log::warn!("归档分类失败: 分类已归档, id={id}");
            return Err(CoreError::CategoryAlreadyArchived);
        }
        category_repo::archive(&self.conn, id)?;
        if let Some(idx) = self.state.categories.iter().position(|c| c.id == id) {
            self.state.categories[idx].archived_at = Some(chrono::Utc::now().timestamp());
        }
        self.finish(vec![CoreEffect {
            kind: "show_snackbar".to_string(),
            payload_json: r#"{"message":"分类已归档"}"#.to_string(),
        }])
    }

    fn handle_category_unarchive(&mut self, id: i64) -> Result<CoreUpdate, CoreError> {
        let category =
            category_repo::get_by_id(&self.conn, id)?.ok_or(CoreError::CategoryNotFound(id))?;
        if category.archived_at.is_none() {
            log::warn!("取消归档失败: 分类未归档, id={id}");
            return Err(CoreError::CategoryNotArchived);
        }
        category_repo::unarchive(&self.conn, id)?;
        if let Some(idx) = self.state.categories.iter().position(|c| c.id == id) {
            self.state.categories[idx].archived_at = None;
        }
        self.finish(vec![CoreEffect {
            kind: "show_snackbar".to_string(),
            payload_json: r#"{"message":"分类已恢复"}"#.to_string(),
        }])
    }

    fn handle_category_list(&mut self) -> Result<CoreUpdate, CoreError> {
        let list = category_repo::list_all(&self.conn)?;
        self.state.categories = list.iter().map(CategoryDto::from_category).collect();
        self.finish(Vec::new())
    }
}

/// 校验描述长度上限。
fn validate_description_len(description: Option<&str>) -> Result<(), CoreError> {
    if let Some(desc) = description
        && desc.len() > crate::shared::constants::CATEGORY_DESCRIPTION_MAX_LEN
    {
        log::warn!("分类描述长度超限, {} 字符", desc.len());
        return Err(CoreError::InvalidInput(format!(
            "描述长度不能超过 {} 字符",
            crate::shared::constants::CATEGORY_DESCRIPTION_MAX_LEN
        )));
    }
    Ok(())
}
