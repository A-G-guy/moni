use serde_json;

use moni_contracts::category::Category;
use moni_contracts::record::Record;

/// 将记录和分类导出为 JSON 字符串。
pub fn generate(records: &[Record], categories: &[Category]) -> Result<String, serde_json::Error> {
    #[derive(serde::Serialize)]
    struct ExportData<'a> {
        records: &'a [Record],
        categories: &'a [Category],
        version: &'a str,
    }

    let data = ExportData {
        records,
        categories,
        version: "1.0",
    };

    serde_json::to_string_pretty(&data)
}

#[cfg(test)]
mod tests {
    use super::*;
    use moni_contracts::category::Category;
    use moni_contracts::record::Record;

    #[test]
    fn test_generate_json() {
        let records = vec![Record {
            id: 1,
            amount_cents: 1234,
            record_type: moni_contracts::record::RecordType::Expense,
            category_id: 1,
            note: "午餐".to_string(),
            created_at: 1000,
            updated_at: 1000,
        }];
        let categories = vec![Category {
            id: 1,
            name: "餐饮".to_string(),
            category_type: moni_contracts::record::RecordType::Expense,
            icon_name: "restaurant".to_string(),
            color_hex: "#FF6B6B".to_string(),
            sort_order: 1,
            is_preset: true,
            created_at: 0,
            updated_at: 0,
        }];

        let json = generate(&records, &categories).unwrap();
        assert!(json.contains("午餐"));
        assert!(json.contains("餐饮"));
        assert!(json.contains("\"version\": \"1.0\""));
    }
}
