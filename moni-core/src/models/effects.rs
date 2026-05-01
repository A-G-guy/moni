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
    pub fn new(state: &crate::models::state::AppState, effects: Vec<CoreEffect>) -> Self {
        Self {
            state_json: serde_json::to_string(state).unwrap_or_default(),
            effects,
        }
    }
}
