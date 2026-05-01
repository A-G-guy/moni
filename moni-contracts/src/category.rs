use serde::{Deserialize, Serialize};

use crate::types::{CategoryId, TimestampSec};
use crate::record::RecordType;

/// 分类实体
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Category {
    pub id: CategoryId,
    pub name: String,
    pub category_type: RecordType,
    pub icon_name: String,
    pub color_hex: String,
    pub sort_order: i32,
    pub is_preset: bool,
    pub created_at: TimestampSec,
    pub updated_at: TimestampSec,
}
