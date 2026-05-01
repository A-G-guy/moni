use crate::core::error::CoreError;
use crate::core::runtime::AppCoreRuntime;
use crate::db::record_repo;
use crate::dto::record_list_to_dto;
use crate::models::effects::CoreUpdate;
use crate::models::intent::CoreIntent;

impl AppCoreRuntime {
    pub(super) fn handle_record_list(
        &mut self,
        intent: CoreIntent,
    ) -> Result<CoreUpdate, CoreError> {
        let CoreIntent::RecordList { page, page_size } = intent else {
            return Err(CoreError::Internal("意图类型不匹配".to_string()));
        };

        if page_size < crate::shared::constants::MIN_PAGE_SIZE
            || page_size > crate::shared::constants::MAX_PAGE_SIZE
        {
            log::warn!("分页查询失败: 页大小越界, 收到: {page_size}");
            return Err(CoreError::InvalidInput(format!(
                "分页大小必须在 {}-{} 之间",
                crate::shared::constants::MIN_PAGE_SIZE,
                crate::shared::constants::MAX_PAGE_SIZE,
            )));
        }

        let list = record_repo::list_paginated(&self.conn, page, page_size)?;
        self.state.records = record_list_to_dto(&list, &self.state.categories);
        self.state.record_groups =
            crate::dto::group_records_by_date(&self.state.records);
        self.finish(Vec::new())
    }
}
