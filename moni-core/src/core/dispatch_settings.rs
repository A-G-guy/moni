use crate::core::error::CoreError;
use crate::core::runtime::AppCoreRuntime;
use crate::db::category_repo;
use crate::db::record_repo;
use crate::domain::export::{csv_generator, json_generator};
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
                Ok(self.finish(Vec::new()))
            }
            CoreIntent::SettingsExportData { format } => {
                let records = record_repo::list_paginated(&self.conn, 0, 10000)?;
                let categories = category_repo::list_all(&self.conn)?;

                let content = match format {
                    moni_contracts::export::ExportFormat::Csv => csv_generator::generate(&records, &categories),
                    moni_contracts::export::ExportFormat::Json => {
                        json_generator::generate(&records, &categories)
                            .map_err(|e| CoreError::Internal(format!("JSON 序列化失败: {e}")))?
                    }
                };

                Ok(self.finish(vec![crate::models::effects::CoreEffect {
                    kind: "export_file".to_string(),
                    payload_json: format!(
                        r#"{{"format":"{}","content":{}}}"#,
                        match format {
                            moni_contracts::export::ExportFormat::Csv => "csv",
                            moni_contracts::export::ExportFormat::Json => "json",
                        },
                        serde_json::Value::String(content)
                    ),
                }]))
            }
            _ => unreachable!(),
        }
    }
}
