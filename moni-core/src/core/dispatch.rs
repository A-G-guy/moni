use crate::core::error::CoreError;
use crate::core::runtime::AppCoreRuntime;
use crate::models::effects::CoreUpdate;
use crate::models::intent::CoreIntent;

impl AppCoreRuntime {
    pub fn dispatch_intent(&mut self, intent: CoreIntent) -> CoreUpdate {
        let result = self.dispatch(intent);
        match result {
            Ok(update) => update,
            Err(err) => {
                self.state.ui.error_message = Some(err.to_string());
                self.finish(Vec::new())
            }
        }
    }

    fn dispatch(&mut self, intent: CoreIntent) -> Result<CoreUpdate, CoreError> {
        match intent {
            CoreIntent::RecordCreate { .. }
            | CoreIntent::RecordUpdate { .. }
            | CoreIntent::RecordDelete { .. }
            | CoreIntent::RecordList { .. }
            | CoreIntent::RecordGet { .. } => self.dispatch_record(intent),
            CoreIntent::CategoryCreate { .. }
            | CoreIntent::CategoryDelete { .. }
            | CoreIntent::CategoryList => self.dispatch_category(intent),
            CoreIntent::StatsMonthlySummary { .. }
            | CoreIntent::StatsCategoryBreakdown { .. } => self.dispatch_stats(intent),
            CoreIntent::SettingsUpdateCurrency { .. }
            | CoreIntent::SettingsExportData { .. } => self.dispatch_settings(intent),
            CoreIntent::NavigateTo { screen } => {
                self.state.ui.active_tab = screen;
                Ok(self.finish(Vec::new()))
            }
            CoreIntent::DismissError => {
                self.state.ui.error_message = None;
                Ok(self.finish(Vec::new()))
            }
        }
    }
}
