use crate::core::error::CoreError;
use crate::core::runtime::AppCoreRuntime;
use crate::db::record_repo;
use crate::dto::record_list_to_dto;
use crate::models::effects::CoreUpdate;
use crate::models::intent::CoreIntent;

const ALLOWED_SORT_BY: &[&str] = &["created_at", "amount_cents"];
const ALLOWED_SORT_ORDER: &[&str] = &["asc", "desc"];
const MAX_CATEGORY_FILTER_COUNT: usize = 50;

impl AppCoreRuntime {
    pub(super) fn handle_record_search(
        &mut self,
        intent: &CoreIntent,
    ) -> Result<CoreUpdate, CoreError> {
        let CoreIntent::RecordSearch {
            keyword,
            record_type,
            category_ids,
            amount_min,
            amount_max,
            date_start,
            date_end,
            sort_by,
            sort_order,
            page,
            page_size,
        } = intent
        else {
            return Err(CoreError::Internal("意图类型不匹配".to_string()));
        };

        // 分页参数校验
        if !(crate::shared::constants::MIN_PAGE_SIZE..=crate::shared::constants::MAX_PAGE_SIZE)
            .contains(page_size)
        {
            log::warn!("搜索失败: 页大小越界, 收到: {page_size}");
            return Err(CoreError::InvalidInput(format!(
                "分页大小必须在 {}-{} 之间",
                crate::shared::constants::MIN_PAGE_SIZE,
                crate::shared::constants::MAX_PAGE_SIZE,
            )));
        }
        const MAX_PAGE: u32 = 100_000;
        if *page > MAX_PAGE {
            log::warn!("搜索失败: 页码过大, 收到: {page}");
            return Err(CoreError::InvalidInput(format!("页码不能超过 {MAX_PAGE}")));
        }

        // 排序字段白名单校验
        if !ALLOWED_SORT_BY.contains(&sort_by.as_str()) {
            log::warn!("搜索失败: 非法排序字段, 收到: {sort_by}");
            return Err(CoreError::InvalidInput(format!(
                "排序字段必须是 {} 之一",
                ALLOWED_SORT_BY.join(", ")
            )));
        }
        if !ALLOWED_SORT_ORDER.contains(&sort_order.as_str()) {
            log::warn!("搜索失败: 非法排序方向, 收到: {sort_order}");
            return Err(CoreError::InvalidInput(format!(
                "排序方向必须是 {} 之一",
                ALLOWED_SORT_ORDER.join(", ")
            )));
        }

        // 分类筛选数量上限
        if let Some(ids) = category_ids {
            if ids.len() > MAX_CATEGORY_FILTER_COUNT {
                log::warn!("搜索失败: 分类筛选数量过多, 收到: {}", ids.len());
                return Err(CoreError::InvalidInput(format!(
                    "最多选择 {MAX_CATEGORY_FILTER_COUNT} 个分类"
                )));
            }
        }

        let rt_str = record_type.as_ref().map(|t| match t {
            moni_contracts::record::RecordType::Income => "income",
            moni_contracts::record::RecordType::Expense => "expense",
        });

        let category_ids_slice = category_ids.as_deref();

        // 执行搜索查询
        let list = record_repo::search(
            &self.conn,
            keyword.as_deref(),
            rt_str,
            category_ids_slice,
            *amount_min,
            *amount_max,
            *date_start,
            *date_end,
            sort_by,
            sort_order,
            *page,
            *page_size,
        )?;

        // 聚合统计
        let (total_count, total_income, total_expense, _total_amount) =
            record_repo::search_summary(
                &self.conn,
                keyword.as_deref(),
                rt_str,
                category_ids_slice,
                *amount_min,
                *amount_max,
                *date_start,
                *date_end,
            )?;

        self.state.records = record_list_to_dto(&list, &self.state.categories);
        self.state.record_groups = crate::dto::group_records_by_date(&self.state.records);

        // 构造搜索统计概览（复用 CoreOverviewMetrics 结构）
        self.state.overview_metrics = Some(crate::models::state::OverviewMetrics {
            month_expense: total_expense,
            month_income: total_income,
            month_balance: total_income - total_expense,
            today_expense: None,
            daily_avg: None,
            daily_remaining: None,
            total_budget: None,
            elapsed_days: 1,
            total_days: 30,
            remaining_days: 0,
        });

        self.state.search_result_count = total_count;

        self.finish(Vec::new())
    }
}
