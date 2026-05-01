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
                self.finish(Vec::new())
            }
            CoreIntent::DismissError => {
                self.state.ui.error_message = None;
                self.finish(Vec::new())
            }
        }
    }
}
