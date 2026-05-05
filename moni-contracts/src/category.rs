use serde::{Deserialize, Serialize};

use crate::record::RecordType;
use crate::types::{CategoryId, TimestampSec};

/// 分类实体。
///
/// 设计要点：
/// - 颜色统一由 `record_type` 决定（支出红 / 收入绿），不再让用户自定义；
/// - `description` 选填，用于补充分类的语义说明；
/// - `archived_at` 为 NULL 表示活跃；非 NULL 表示已归档（不可再被新建记录选中，但历史记录依然展示）。
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Category {
    pub id: CategoryId,
    pub name: String,
    pub description: Option<String>,
    pub category_type: RecordType,
    pub icon_name: String,
    pub sort_order: i32,
    pub parent_id: Option<CategoryId>,
    pub archived_at: Option<TimestampSec>,
    pub created_at: TimestampSec,
    pub updated_at: TimestampSec,
}

impl Category {
    /// 是否处于活跃状态（未归档）。
    pub fn is_active(&self) -> bool {
        self.archived_at.is_none()
    }

    /// 是否为二级分类（拥有父分类）。
    pub fn is_sub_category(&self) -> bool {
        self.parent_id.is_some()
    }
}
