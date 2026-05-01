use serde::{Deserialize, Serialize};

use crate::types::{AmountCents, CategoryId, RecordId, TimestampSec};

/// 收支类型
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum RecordType {
    Income,
    Expense,
}

/// 记账记录实体
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Record {
    pub id: RecordId,
    pub amount_cents: AmountCents,
    pub record_type: RecordType,
    pub category_id: CategoryId,
    pub note: String,
    pub created_at: TimestampSec,
    pub updated_at: TimestampSec,
}
