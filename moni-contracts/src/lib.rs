use serde::{Deserialize, Serialize};

pub mod category;
pub mod export;
pub mod record;
pub mod stats;
pub mod types;

/// 问候语领域契约
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Greeting {
    pub name: String,
    pub message: String,
}
