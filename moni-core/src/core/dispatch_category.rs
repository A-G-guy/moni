use moni_contracts::record::RecordType;
use moni_contracts::types::CategoryId;

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
                parent_id,
            } => self.handle_category_create(
                &name,
                description.as_deref(),
                category_type,
                &icon_name,
                parent_id,
            ),
            CoreIntent::CategoryUpdate {
                id,
                name,
                description,
                icon_name,
                parent_id,
                clear_parent_id,
            } => self.handle_category_update(
                id,
                name.as_deref(),
                description.as_deref(),
                icon_name.as_deref(),
                parent_id,
                clear_parent_id,
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
        parent_id: Option<CategoryId>,
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

        if let Some(pid) = parent_id {
            validate_parent_id(&self.conn, pid, category_type, None)?;
        }

        let id = category_repo::insert(
            &self.conn,
            trimmed_name,
            description,
            category_type,
            icon_name,
            crate::shared::constants::CUSTOM_CATEGORY_SORT_ORDER,
            parent_id,
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
        parent_id: Option<CategoryId>,
        clear_parent_id: bool,
    ) -> Result<CoreUpdate, CoreError> {
        let category =
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

        // 解析 parent_id 的最终意图
        let parent_id_opt: Option<Option<CategoryId>> = if clear_parent_id {
            Some(None) // 清空 parent_id（变回一级分类）
        } else if let Some(pid) = parent_id {
            Some(Some(pid)) // 设置为指定父分类
        } else {
            None // 不更新 parent_id
        };

        // 校验新的 parent_id
        if let Some(Some(pid)) = parent_id_opt {
            if pid == id {
                log::warn!("更新分类失败: 不能将分类设为自己的父分类, id={id}");
                return Err(CoreError::InvalidInput("不能将分类设为自己的父分类".to_string()));
            }
            validate_parent_id(&self.conn, pid, category.category_type, Some(id))?;
        }

        // 若要将分类设为二级分类，检查该分类是否已有子分类
        if let Some(Some(_)) = parent_id_opt {
            if category_repo::has_children(&self.conn, id)? {
                log::warn!("更新分类失败: 该分类已有子分类，不能设为二级分类, id={id}");
                return Err(CoreError::InvalidInput(
                    "该分类已有子分类，不能设为二级分类".to_string(),
                ));
            }
        }

        let affected = category_repo::update(
            &self.conn,
            id,
            name.map(str::trim),
            description,
            icon_name.map(str::trim),
            parent_id_opt,
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
        if category_repo::has_children(&self.conn, id)? {
            log::warn!("归档分类失败: 该分类存在子分类, id={id}");
            return Err(CoreError::InvalidInput(
                "该分类存在子分类，请先归档子分类".to_string(),
            ));
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

/// 校验父分类是否合法。
fn validate_parent_id(
    conn: &rusqlite::Connection,
    parent_id: i64,
    child_type: RecordType,
    child_id: Option<i64>,
) -> Result<(), CoreError> {
    if Some(parent_id) == child_id {
        log::warn!("父分类校验失败: 不能将分类设为自己的父分类");
        return Err(CoreError::InvalidInput("不能将分类设为自己的父分类".to_string()));
    }

    let parent = category_repo::get_by_id(conn, parent_id)?
        .ok_or_else(|| {
            log::warn!("父分类校验失败: 父分类不存在, id={parent_id}");
            CoreError::CategoryNotFound(parent_id)
        })?;

    if parent.archived_at.is_some() {
        log::warn!("父分类校验失败: 父分类已归档, id={parent_id}");
        return Err(CoreError::InvalidInput("父分类已归档".to_string()));
    }

    if parent.category_type != child_type {
        log::warn!("父分类校验失败: 类型不一致, parent={:?}, child={:?}", parent.category_type, child_type);
        return Err(CoreError::InvalidInput("父分类与子分类类型不一致".to_string()));
    }

    if parent.parent_id.is_some() {
        log::warn!("父分类校验失败: 仅支持单一层级, parent_id={parent_id}");
        return Err(CoreError::InvalidInput("仅支持单一层级，不能将二级分类设为父分类".to_string()));
    }

    Ok(())
}
