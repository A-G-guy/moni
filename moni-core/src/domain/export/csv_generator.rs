use moni_contracts::category::Category;
use moni_contracts::record::Record;
use moni_contracts::record::RecordType;

/// 将记录和分类导出为 CSV 字符串。
pub fn generate(records: &[Record], categories: &[Category]) -> String {
    let mut output = String::new();
    output.push_str("\u{FEFF}"); // UTF-8 BOM for Excel compatibility
    output.push_str("ID,金额(分),类型,分类,备注,创建时间(Unix秒),更新时间(Unix秒)\n");

    for rec in records {
        output.push_str(&format!(
            "{},{},{},{},{},{},{}\n",
            rec.id,
            rec.amount_cents,
            match rec.record_type {
                RecordType::Income => "收入",
                RecordType::Expense => "支出",
            },
            rec.category_id,
            escape_csv_field(&rec.note),
            rec.created_at,
            rec.updated_at,
        ));
    }

    output.push('\n');
    output.push_str("分类ID,分类名称,类型,图标,颜色\n");
    for cat in categories {
        output.push_str(&format!(
            "{},{},{},{},{}\n",
            cat.id,
            escape_csv_field(&cat.name),
            match cat.category_type {
                RecordType::Income => "收入",
                RecordType::Expense => "支出",
            },
            cat.icon_name,
            cat.color_hex,
        ));
    }

    output
}

fn escape_csv_field(field: &str) -> String {
    if field.contains(',') || field.contains('"') || field.contains('\n') {
        let escaped = field.replace('"', "\"\"");
        format!("\"{}\"", escaped)
    } else {
        field.to_string()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use moni_contracts::category::Category;
    use moni_contracts::record::Record;

    #[test]
    fn test_generate_csv() {
        let records = vec![Record {
            id: 1,
            amount_cents: 1234,
            record_type: RecordType::Expense,
            category_id: 1,
            note: "午餐".to_string(),
            created_at: 1000,
            updated_at: 1000,
        }];
        let categories = vec![Category {
            id: 1,
            name: "餐饮".to_string(),
            category_type: RecordType::Expense,
            icon_name: "restaurant".to_string(),
            color_hex: "#FF6B6B".to_string(),
            sort_order: 1,
            is_preset: true,
            created_at: 0,
            updated_at: 0,
        }];

        let csv = generate(&records, &categories);
        assert!(csv.contains("\u{FEFF}"));
        assert!(csv.contains("午餐"));
        assert!(csv.contains("餐饮"));
    }
}
