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
            } => self.handle_budget_upsert(category_id, amount_cents),
            CoreIntent::BudgetDelete { id } => self.handle_budget_delete(id),
            CoreIntent::BudgetList => self.handle_budget_list(),
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
        amount_cents: AmountCents,
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

        budget_repo::upsert(&self.conn, category_id, amount_cents)?;
        self.refresh_budget_states()?;

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

    fn handle_budget_delete(&mut self, id: BudgetId) -> Result<CoreUpdate, CoreError> {
        let affected = budget_repo::delete(&self.conn, id)?;
        if affected == 0 {
            return Err(CoreError::Internal("预算不存在".to_string()));
        }
        self.refresh_budget_states()?;
        self.finish(vec![CoreEffect {
            kind: "show_snackbar".to_string(),
            payload_json: r#"{"message":"预算已删除"}"#.to_string(),
        }])
    }

    fn handle_budget_list(&mut self) -> Result<CoreUpdate, CoreError> {
        self.refresh_budget_states()?;
        self.finish(Vec::new())
    }

    fn handle_budget_check(
        &mut self,
        category_id: CategoryId,
        year_month: &str,
        amount_cents: AmountCents,
    ) -> Result<CoreUpdate, CoreError> {
        let raw_budgets = budget_repo::list_all(&self.conn)?;
        let budget_dtos =
            calculator::build_budget_dtos(&self.conn, &raw_budgets, &self.state.categories, year_month)?;

        let category_spending = calculator::compute_category_spending(&self.conn, year_month)?;

        let effective = calculator::effective_available(
            category_id,
            &budget_dtos,
            &self.state.categories,
            &category_spending,
        );

        let bottleneck = calculator::bottleneck_budget(
            category_id,
            &budget_dtos,
            &self.state.categories,
            &category_spending,
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
            post_save_status,
        });

        self.finish(Vec::new())
    }

    /// 刷新所有预算的实时状态。
    pub(super) fn refresh_budget_states(&mut self) -> Result<(), CoreError> {
        let year_month = chrono::Utc::now().format("%Y-%m").to_string();
        let raw_budgets = budget_repo::list_all(&self.conn)?;
        let budget_dtos = calculator::build_budget_dtos(
            &self.conn,
            &raw_budgets,
            &self.state.categories,
            &year_month,
        )?;
        self.state.budgets = budget_dtos;
        Ok(())
    }
}
