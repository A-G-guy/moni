use moni_contracts::record::RecordType;

use crate::dto::CategoryDto;

/// 前端消费用的记录 DTO，展开关联分类信息，不包含内部时间戳字段。
#[derive(Debug, Clone, serde::Serialize, serde::Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RecordDto {
    pub id: i64,
    pub amount_cents: i64,
    pub record_type: RecordType,
    pub category_id: i64,
    pub category_name: String,
    pub note: String,
    pub created_at: i64,
}

impl RecordDto {
    pub fn from_record(
        record: &moni_contracts::record::Record,
        categories: &[CategoryDto],
    ) -> Self {
        let cat_name = categories
            .iter()
            .find(|c| c.id == record.category_id)
            .map_or_else(|| "未知分类".to_string(), |c| c.name.clone());

        Self {
            id: record.id,
            amount_cents: record.amount_cents,
            record_type: record.record_type,
            category_id: record.category_id,
            category_name: cat_name,
            note: record.note.clone(),
            created_at: record.created_at,
        }
    }
}

/// 将记录实体列表批量转换为 DTO 列表。
pub fn record_list_to_dto(
    records: &[moni_contracts::record::Record],
    categories: &[CategoryDto],
) -> Vec<RecordDto> {
    records
        .iter()
        .map(|r| RecordDto::from_record(r, categories))
        .collect()
}

/// 按日期分组的记录列表单元，供前端直接渲染。
#[derive(Debug, Clone, serde::Serialize, serde::Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RecordDayGroup {
    pub date: String,
    pub income_cents: i64,
    pub expense_cents: i64,
    pub records: Vec<RecordDto>,
}

/// 将记录 DTO 列表按日期分组并计算日汇总。
pub fn group_records_by_date(records: &[RecordDto]) -> Vec<RecordDayGroup> {
    use std::collections::BTreeMap;

    let mut groups: BTreeMap<String, Vec<RecordDto>> = BTreeMap::new();

    for record in records {
        let date = chrono::DateTime::from_timestamp(record.created_at, 0)
            .map(|dt| dt.format("%Y-%m-%d").to_string())
            .unwrap_or_default();
        groups.entry(date).or_default().push(record.clone());
    }

    groups
        .into_iter()
        .rev()
        .map(|(date, records)| {
            let income_cents = records
                .iter()
                .filter(|r| r.record_type == RecordType::Income)
                .map(|r| r.amount_cents)
                .sum();
            let expense_cents = records
                .iter()
                .filter(|r| r.record_type == RecordType::Expense)
                .map(|r| r.amount_cents)
                .sum();
            RecordDayGroup {
                date,
                income_cents,
                expense_cents,
                records,
            }
        })
        .collect()
}
