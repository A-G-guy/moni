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
        scope: &str,
    ) -> Result<CoreUpdate, CoreError> {
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

        match scope {
            "this_month" => {
                budget_repo::upsert(&self.conn, category_id, Some(year_month), amount_cents)?;
            }
            "this_and_future" => {
                budget_repo::upsert(&self.conn, category_id, None, amount_cents)?;
                budget_repo::delete_snapshots_from(&self.conn, category_id, year_month)?;
            }
            "future_only" => {
                // 若当前月无快照，用旧模板值创建当前月快照以保留当前月
                let has_snapshot =
                    budget_repo::has_snapshot_for_month(&self.conn, category_id, year_month)?;
                if !has_snapshot {
                    if let Some(template) = budget_repo::get_template(&self.conn, category_id)? {
                        budget_repo::upsert(
                            &self.conn,
                            category_id,
                            Some(year_month),
                            template.amount_cents,
                        )?;
                    }
                }
                budget_repo::upsert(&self.conn, category_id, None, amount_cents)?;
                let next_month = compute_next_month(year_month);
                budget_repo::delete_snapshots_from(&self.conn, category_id, &next_month)?;
            }
            other => {
                log::warn!("无效的预算范围: {other}");
                return Err(CoreError::InvalidInput("无效的预算范围".to_string()));
            }
        }

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
        scope: &str,
    ) -> Result<CoreUpdate, CoreError> {
        let budget = budget_repo::get_by_id(&self.conn, id)?
            .ok_or_else(|| CoreError::Internal("预算不存在".to_string()))?;

        match scope {
            "this_month" => {
                // 从本月起停止预算：删除模板 + 删除从当前月开始的所有快照
                budget_repo::delete_template(&self.conn, budget.category_id)?;
                budget_repo::delete_snapshots_from(
                    &self.conn,
                    budget.category_id,
                    year_month,
                )?;
            }
            "future_only" => {
                // 保留当前月（无快照则创建）
                let has_snapshot =
                    budget_repo::has_snapshot_for_month(
                        &self.conn, budget.category_id, year_month)?;
                if !has_snapshot {
                    budget_repo::upsert(
                        &self.conn,
                        budget.category_id,
                        Some(year_month),
                        budget.amount_cents,
                    )?;
                }
                // 删除模板
                if budget.year_month.is_none() {
                    budget_repo::delete(&self.conn, id)?;
                } else {
                    budget_repo::delete_template(&self.conn, budget.category_id)?;
                }
                // 删除下月及以后的快照
                let next_month = compute_next_month(year_month);
                budget_repo::delete_snapshots_from(
                    &self.conn,
                    budget.category_id,
                    &next_month,
                )?;
            }
            other => {
                log::warn!("无效的删除范围: {other}");
                return Err(CoreError::InvalidInput("无效的删除范围".to_string()));
            }
        }

        self.refresh_budget_states(Some(year_month))?;
        self.finish(vec![CoreEffect {
            kind: "show_snackbar".to_string(),
            payload_json: r#"{"message":"预算已删除"}"#.to_string(),
        }])
    }

    fn handle_budget_list(&mut self, year_month: Option<&str>) -> Result<CoreUpdate, CoreError> {
        self.refresh_budget_states(year_month)?;
        self.finish(Vec::new())
    }

    fn handle_budget_check(
        &mut self,
        category_id: CategoryId,
        year_month: &str,
        amount_cents: AmountCents,
    ) -> Result<CoreUpdate, CoreError> {
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

        // 模拟加上 amount_cents 后的状态
        let post_save_status = if let Some(eff) = effective {
            let remaining_after = eff - amount_cents;
            if remaining_after < 0 {
                Some("overrun".to_string())
            } else if remaining_after < (eff as f64 * 0.2) as i64 {
                Some("critical".to_string())
            } else {
                Some("safe".to_string())
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
            .unwrap_or_else(|| chrono::Local::now().format("%Y-%m").to_string());
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

/// 计算下个月份（格式 "YYYY-MM"）。
/// 例如 "2025-12" → "2026-01"。
fn compute_next_month(year_month: &str) -> String {
    let parts: Vec<&str> = year_month.split('-').collect();
    let year: i32 = parts[0].parse().unwrap_or(2025);
    let month: u32 = parts[1].parse().unwrap_or(1);
    let date = chrono::NaiveDate::from_ymd_opt(year, month, 1).unwrap_or_default();
    let next = date
        .checked_add_months(chrono::Months::new(1))
        .unwrap_or(date);
    next.format("%Y-%m").to_string()
}
