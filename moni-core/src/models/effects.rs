use serde::{Deserialize, Serialize};

/// 副作用
#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct CoreEffect {
    pub kind: String,
    pub payload_json: String,
}

/// 状态更新
#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct CoreUpdate {
    pub state_json: String,
    pub effects: Vec<CoreEffect>,
}

impl CoreUpdate {
    pub fn new(state: &crate::models::state::AppState, effects: Vec<CoreEffect>) -> Result<Self, crate::core::error::CoreError> {
        let state_json = serde_json::to_string(state)
            .map_err(|e| crate::core::error::CoreError::Internal(format!("状态序列化失败: {e}")))?;
        Ok(Self { state_json, effects })
    }
}
