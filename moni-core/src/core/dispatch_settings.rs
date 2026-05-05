use crate::core::error::CoreError;
use crate::core::runtime::AppCoreRuntime;
use crate::models::effects::CoreUpdate;
use crate::models::intent::CoreIntent;

impl AppCoreRuntime {
    pub(super) fn dispatch_settings(
        &mut self,
        intent: CoreIntent,
    ) -> Result<CoreUpdate, CoreError> {
        match intent {
            CoreIntent::SettingsUpdateCurrency { symbol } => {
                self.state.settings.currency_symbol = symbol;
                self.finish(Vec::new())
            }
            _ => {
                log::warn!("设置模块收到未支持的意图类型");
                Err(CoreError::Internal("未支持的意图类型".to_string()))
            }
        }
    }
}
