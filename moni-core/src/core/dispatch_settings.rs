use crate::core::error::CoreError;
use crate::core::runtime::AppCoreRuntime;
use crate::db::settings_repo;
use crate::models::effects::CoreUpdate;
use crate::models::intent::CoreIntent;

impl AppCoreRuntime {
    pub(super) fn dispatch_settings(
        &mut self,
        intent: CoreIntent,
    ) -> Result<CoreUpdate, CoreError> {
        match intent {
            CoreIntent::SettingsUpdateCurrency { symbol } => {
                settings_repo::set(&self.conn, "currency_symbol", &symbol)?;
                self.state.settings.currency_symbol = symbol.clone();
                let effect = crate::models::effects::CoreEffect {
                    kind: "persist_setting".to_string(),
                    payload_json: serde_json::json!({
                        "key": "currency_symbol",
                        "value": symbol
                    })
                    .to_string(),
                };
                self.finish(vec![effect])
            }
            _ => {
                log::warn!("设置模块收到未支持的意图类型");
                Err(CoreError::Internal("未支持的意图类型".to_string()))
            }
        }
    }
}
