use crate::core::error::CoreError;
use crate::core::runtime::AppCoreRuntime;
use crate::db::record_repo;
use crate::domain::stats::calculator;
use crate::models::effects::CoreUpdate;
use crate::models::intent::CoreIntent;

impl AppCoreRuntime {
    pub(super) fn dispatch_stats(
        &mut self,
        intent: CoreIntent,
    ) -> Result<CoreUpdate, CoreError> {
        match intent {
            CoreIntent::StatsMonthlySummary { months } => {
                let aggregates = record_repo::monthly_aggregates(&self.conn, months)?;
                self.state.monthly_summaries = calculator::calculate_monthly_summary(aggregates);
                self.finish(Vec::new())
            }
            CoreIntent::StatsCategoryBreakdown { year_month } => {
                let aggregates = record_repo::category_aggregates(&self.conn, &year_month)?;
                self.state.current_month_breakdown =
                    calculator::calculate_category_breakdown(aggregates);
                self.finish(Vec::new())
            }
            _ => {
                log::warn!("统计模块收到未支持的意图类型");
                Err(CoreError::Internal("未支持的意图类型".to_string()))
            }
        }
    }
}
