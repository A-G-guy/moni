use moni_contracts::budget::BudgetStatus;
use moni_contracts::record::RecordType;
use moni_contracts::types::{AmountCents, BudgetId, CategoryId};

use crate::core::error::CoreError;
use crate::core::runtime::AppCoreRuntime;
use crate::db::{budget_repo, category_repo};
use crate::domain::budget::calculator;
use crate::models::effects::{CoreEffect, CoreUpdate};
use crate::models::intent::CoreIntent;
use crate::models::state::BudgetCheckResult;

impl AppCoreRuntime {
    pub(super) fn dispatch_budget(
        &mut self,
        intent: CoreIntent,
    ) -> Result<CoreUpdate, CoreError> {
        match intent {
            CoreIntent::BudgetUpsert {
                category_id,
                amount_cents,
                year_month,
                scope,
            } => self.handle_budget_upsert(category_id, &year_month, amount_cents, &scope),
            CoreIntent::BudgetDelete {
                id,
                year_month,
                scope,
            } => self.handle_budget_delete(id, &year_month, &scope),
            CoreIntent::BudgetList { year_month } => {
                self.handle_budget_list(year_month.as_deref())
            }
            CoreIntent::BudgetCheck {
                category_id,
                year_month,
                amount_cents,
            } => self.handle_budget_check(category_id, &year_month, amount_cents),
            _ => {
                log::warn!("预算模块收到未支持的意图类型");
                Err(CoreError::Internal("未支持的意图类型".to_string()))
            }
        }
    }

    fn handle_budget_upsert(
        &mut self,
        category_id: Option<CategoryId>,
        year_month: &str,
        amount_cents: AmountCents,
        scope: &moni_contracts::budget::BudgetScope,
    ) -> Result<CoreUpdate, CoreError> {
        validate_year_month(year_month)?;

        if amount_cents <= 0 {
            log::warn!("设置预算失败: 金额必须大于0");
            return Err(CoreError::InvalidInput("预算金额必须大于0".to_string()));
        }

        // 若指定了分类，校验分类存在且为支出类型
        if let Some(cid) = category_id {
            let category = category_repo::get_by_id(&self.conn, cid)?
                .ok_or(CoreError::CategoryNotFound(cid))?;
            if category.category_type != RecordType::Expense {
                log::warn!("设置预算失败: 预算仅支持支出分类, id={cid}");
                return Err(CoreError::InvalidInput("预算仅支持支出分类".to_string()));
            }
        }

        // 多步写入操作需要事务保护
        let tx = self.conn.transaction()?;

        match scope {
            moni_contracts::budget::BudgetScope::ThisMonth => {
                budget_repo::upsert(&tx, category_id, Some(year_month), amount_cents,
                )?;
            }
            moni_contracts::budget::BudgetScope::ThisAndFuture => {
                budget_repo::upsert(&tx, category_id, None, amount_cents)?;
                budget_repo::delete_snapshots_from(&tx, category_id, year_month)?;
            }
            moni_contracts::budget::BudgetScope::FutureOnly => {
                // 若当前月无快照，用旧模板值创建当前月快照以保留当前月
                let has_snapshot =
                    budget_repo::has_snapshot_for_month(&tx, category_id, year_month)?;
                if !has_snapshot {
                    let snapshot_amount = budget_repo::get_template(&tx, category_id)?
                        .map(|t| t.amount_cents)
                        .unwrap_or(0);
                    budget_repo::upsert(
                        &tx,
                        category_id,
                        Some(year_month),
                        snapshot_amount,
                    )?;
                }
                budget_repo::upsert(&tx, category_id, None, amount_cents)?;
                let next_month = compute_next_month(year_month)?;
                budget_repo::delete_snapshots_from(
                    &tx, category_id, &next_month)?;
            }
        }

        tx.commit()?;

        self.state.budget_check_result = None;
        self.refresh_budget_states(Some(year_month))?;

        let msg = if category_id.is_some() {
            "分类预算已更新"
        } else {
            "总预算已更新"
        };
        self.finish(vec![CoreEffect {
            kind: "show_snackbar".to_string(),
            payload_json: format!("{{\"message\":\"{}\"}}", msg),
        }])
    }

    fn handle_budget_delete(
        &mut self,
        id: BudgetId,
        year_month: &str,
        scope: &moni_contracts::budget::BudgetScope,
    ) -> Result<CoreUpdate, CoreError> {
        validate_year_month(year_month)?;

        let budget = budget_repo::get_by_id(&self.conn, id)?
            .ok_or_else(|| CoreError::Internal("预算不存在".to_string()))?;

        let tx = self.conn.transaction()?;

        match scope {
            moni_contracts::budget::BudgetScope::ThisMonth => {
                // 从本月起停止预算：删除模板 + 删除从当前月开始的所有快照
                budget_repo::delete_template(&tx, budget.category_id)?;
                budget_repo::delete_snapshots_from(
                    &tx,
                    budget.category_id,
                    year_month,
                )?;
            }
            moni_contracts::budget::BudgetScope::FutureOnly => {
                // 保留当前月（无快照则创建）
                let has_snapshot =
                    budget_repo::has_snapshot_for_month(
                        &tx, budget.category_id, year_month)?;
                if !has_snapshot {
                    budget_repo::upsert(
                        &tx,
                        budget.category_id,
                        Some(year_month),
                        budget.amount_cents,
                    )?;
                }
                // 删除模板
                budget_repo::delete_template(&tx, budget.category_id)?;
                // 删除下月及以后的快照
                let next_month = compute_next_month(year_month)?;
                budget_repo::delete_snapshots_from(
                    &tx,
                    budget.category_id,
                    &next_month,
                )?;
            }
            moni_contracts::budget::BudgetScope::ThisAndFuture => {
                // BudgetDelete 不支持 ThisAndFuture，按 ThisMonth 处理
                log::warn!("预算删除不支持 this_and_future 范围，回退到 this_month");
                budget_repo::delete_template(&tx, budget.category_id)?;
                budget_repo::delete_snapshots_from(
                    &tx,
                    budget.category_id,
                    year_month,
                )?;
            }
        }

        tx.commit()?;

        self.state.budget_check_result = None;
        self.refresh_budget_states(Some(year_month))?;
        self.finish(vec![CoreEffect {
            kind: "show_snackbar".to_string(),
            payload_json: r#"{"message":"预算已删除"}"#.to_string(),
        }])
    }

    fn handle_budget_list(
        &mut self,
        year_month: Option<&str>,
    ) -> Result<CoreUpdate, CoreError> {
        if let Some(ym) = year_month {
            validate_year_month(ym)?;
        }
        self.state.budget_check_result = None;
        self.refresh_budget_states(year_month)?;
        self.finish(Vec::new())
    }

    fn handle_budget_check(
        &mut self,
        category_id: CategoryId,
        year_month: &str,
        amount_cents: AmountCents,
    ) -> Result<CoreUpdate, CoreError> {
        validate_year_month(year_month)?;

        // 校验分类存在且为支出类型
        let category = category_repo::get_by_id(&self.conn, category_id)?
            .ok_or(CoreError::CategoryNotFound(category_id))?;
        if category.category_type != RecordType::Expense {
            return Err(CoreError::InvalidInput("预算仅支持支出分类".to_string()));
        }

        let raw_budgets = budget_repo::list_for_month(&self.conn, year_month)?;
        let (budget_dtos, category_spending, parent_category_spending) =
            calculator::build_budget_dtos(&self.conn, &raw_budgets, &self.state.categories, year_month)?;

        let effective = calculator::effective_available(
            category_id,
            &budget_dtos,
            &self.state.categories,
            &category_spending,
            &parent_category_spending,
        );

        let (bottleneck, bottleneck_name) = calculator::bottleneck_budget_with_name(
            category_id,
            &budget_dtos,
            &self.state.categories,
            &category_spending,
            &parent_category_spending,
        );

        // 模拟加上 amount_cents 后的状态（复用 BudgetStatus::from_percentage 统一临界标准）
        let post_save_status = if let Some(eff) = effective {
            if eff <= 0 {
                // 已超支或刚好用完
                Some(BudgetStatus::Overrun.as_str().to_string())
            } else {
                let remaining_after = eff.saturating_sub(amount_cents);
                if remaining_after < 0 {
                    Some(BudgetStatus::Overrun.as_str().to_string())
                } else {
                    #[allow(clippy::cast_precision_loss)]
                    let percentage =
                        (eff.saturating_sub(remaining_after) as f64) / (eff as f64);
                    Some(BudgetStatus::from_percentage(percentage).as_str().to_string())
                }
            }
        } else {
            None
        };

        self.state.budget_check_result = Some(BudgetCheckResult {
            category_id,
            amount_cents,
            effective_available: effective,
            bottleneck_budget: bottleneck,
            bottleneck_category_name: bottleneck_name,
            post_save_status,
        });

        self.finish(Vec::new())
    }

    /// 刷新所有预算的实时状态。
    pub(super) fn refresh_budget_states(
        &mut self,
        year_month: Option<&str>,
    ) -> Result<(), CoreError> {
        let year_month = year_month
            .map(|s| s.to_string())
            .unwrap_or_else(|| chrono::Utc::now().format("%Y-%m").to_string());
        let raw_budgets = budget_repo::list_for_month(&self.conn, &year_month)?;
        let (budget_dtos, _, _) = calculator::build_budget_dtos(
            &self.conn,
            &raw_budgets,
            &self.state.categories,
            &year_month,
        )?;
        self.state.budgets = budget_dtos;
        Ok(())
    }
}

/// 校验 year_month 格式是否为 "YYYY-MM"。
fn validate_year_month(year_month: &str) -> Result<(), CoreError> {
    if year_month.len() != 7 {
        return Err(CoreError::InvalidInput(format!(
            "year_month 长度应为 7（YYYY-MM），收到: {year_month}"
        )));
    }
    let bytes = year_month.as_bytes();
    if bytes[4] != b'-' {
        return Err(CoreError::InvalidInput(format!(
            "year_month 第 5 位应为 '-'，收到: {year_month}"
        )));
    }
    let year_str = &year_month[..4];
    let month_str = &year_month[5..];

    let year: i32 = year_str.parse().map_err(|_| {
        CoreError::InvalidInput(format!("year_month 年份无效: {year_month}"))
    })?;
    let month: u32 = month_str.parse().map_err(|_| {
        CoreError::InvalidInput(format!("year_month 月份无效: {year_month}"))
    })?;

    if year < 1900 || year > 3000 {
        return Err(CoreError::InvalidInput(format!(
            "year_month 年份超出范围 [1900, 3000]: {year_month}"
        )));
    }
    if month < 1 || month > 12 {
        return Err(CoreError::InvalidInput(format!(
            "year_month 月份超出范围 [1, 12]: {year_month}"
        )));
    }

    Ok(())
}

/// 计算下个月份（格式 "YYYY-MM"）。
/// 例如 "2025-12" → "2026-01"。
fn compute_next_month(year_month: &str) -> Result<String, CoreError> {
    let parts: Vec<&str> = year_month.split('-').collect();
    if parts.len() != 2 {
        return Err(CoreError::InvalidInput(format!(
            "compute_next_month 输入格式错误，应为 YYYY-MM: {year_month}"
        )));
    }
    let year: i32 = parts[0].parse().map_err(|_| {
        CoreError::InvalidInput(format!("compute_next_month 年份解析失败: {year_month}"))
    })?;
    let month: u32 = parts[1].parse().map_err(|_| {
        CoreError::InvalidInput(format!("compute_next_month 月份解析失败: {year_month}"))
    })?;

    if month < 1 || month > 12 {
        return Err(CoreError::InvalidInput(format!(
            "compute_next_month 月份超出范围 [1, 12]: {year_month}"
        )));
    }

    // 纯整数运算，避免日期构造和时区问题
    if month == 12 {
        Ok(format!("{:04}-01", year + 1))
    } else {
        Ok(format!("{:04}-{:02}", year, month + 1))
    }
}
