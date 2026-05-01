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
                Ok(self.finish(Vec::new()))
            }
            CoreIntent::StatsCategoryBreakdown { year_month } => {
                let aggregates = record_repo::category_aggregates(&self.conn, &year_month)?;
                self.state.current_month_breakdown =
                    calculator::calculate_category_breakdown(aggregates);
                Ok(self.finish(Vec::new()))
            }
            _ => unreachable!(),
        }
    }
}
