use crate::core::error::CoreError;
use crate::core::runtime::AppCoreRuntime;
use crate::models::effects::CoreUpdate;
use crate::models::intent::CoreIntent;

impl AppCoreRuntime {
    pub fn dispatch_intent(&mut self, intent: CoreIntent) -> CoreUpdate {
        log::debug!("接收到意图: {:?}", std::mem::discriminant(&intent));
        match self.dispatch(intent) {
            Ok(update) => update,
            Err(err) => {
                log::error!("意图处理失败: {err}");
                self.state.ui.error_message = Some(err.to_string());
                self.finish(Vec::new()).unwrap_or_else(|e| {
                    log::error!("错误状态序列化失败: {e}");
                    CoreUpdate {
                        state_json: r#"{"error":"状态不可序列化"}"#.to_string(),
                        effects: Vec::new(),
                    }
                })
            }
        }
    }

    fn dispatch(&mut self, intent: CoreIntent) -> Result<CoreUpdate, CoreError> {
        match intent {
            CoreIntent::RecordCreate { .. }
            | CoreIntent::RecordUpdate { .. }
            | CoreIntent::RecordDelete { .. }
            | CoreIntent::RecordList { .. }
            | CoreIntent::RecordListByMonth { .. }
            | CoreIntent::RecordGet { .. } => self.dispatch_record(intent),
            CoreIntent::CategoryCreate { .. }
            | CoreIntent::CategoryUpdate { .. }
            | CoreIntent::CategoryArchive { .. }
            | CoreIntent::CategoryUnarchive { .. }
            | CoreIntent::CategoryList
            | CoreIntent::CategoryReorder { .. } => self.dispatch_category(intent),
            CoreIntent::StatsMonthlySummary { .. }
            | CoreIntent::StatsCategoryBreakdown { .. }
            | CoreIntent::StatsOverviewMetrics { .. } => {
                self.dispatch_stats(intent)
            }
            CoreIntent::RefreshMonthData { year_month } => {
                self.dispatch_refresh_month_data(year_month)
            }
            CoreIntent::SettingsUpdateCurrency { .. } => {
                self.dispatch_settings(intent)
            }
            CoreIntent::BudgetUpsert { .. }
            | CoreIntent::BudgetDelete { .. }
            | CoreIntent::BudgetList { .. }
            | CoreIntent::BudgetCheck { .. } => self.dispatch_budget(intent),
            CoreIntent::DevClearAllData | CoreIntent::DevGenerateMockData { .. } | CoreIntent::DevSeedPresets => {
                self.dispatch_dev(intent)
            }
            CoreIntent::NavigateTo { screen } => {
                self.state.ui.active_tab = screen;
                self.finish(Vec::new())
            }
            CoreIntent::DismissError => {
                self.state.ui.error_message = None;
                self.finish(Vec::new())
            }
        }
    }

    fn dispatch_refresh_month_data(
        &mut self,
        year_month: String,
    ) -> Result<CoreUpdate, CoreError> {
        let mut all_effects = Vec::new();

        // 依次执行各子操作，任一失败不影响其他，收集所有 effects
        match self.dispatch_record(CoreIntent::RecordListByMonth {
            year_month: year_month.clone(),
        }) {
            Ok(update) => all_effects.extend(update.effects),
            Err(e) => log::warn!("RefreshMonthData 中 RecordListByMonth 失败: {e}"),
        }
        match self.dispatch_stats(CoreIntent::StatsCategoryBreakdown {
            year_month: year_month.clone(),
            aggregate_by_parent: false,
        }) {
            Ok(update) => all_effects.extend(update.effects),
            Err(e) => log::warn!("RefreshMonthData 中 StatsCategoryBreakdown 失败: {e}"),
        }
        match self.dispatch_budget(CoreIntent::BudgetList {
            year_month: Some(year_month.clone()),
        }) {
            Ok(update) => all_effects.extend(update.effects),
            Err(e) => log::warn!("RefreshMonthData 中 BudgetList 失败: {e}"),
        }
        let today = chrono::Local::now().format("%Y-%m-%d").to_string();
        match self.dispatch_stats(CoreIntent::StatsOverviewMetrics {
            year_month: year_month.clone(),
            today,
        }) {
            Ok(update) => all_effects.extend(update.effects),
            Err(e) => log::warn!("RefreshMonthData 中 StatsOverviewMetrics 失败: {e}"),
        }
        self.finish(all_effects)
    }
}
