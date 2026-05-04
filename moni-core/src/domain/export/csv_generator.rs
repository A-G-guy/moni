use moni_contracts::category::Category;
use moni_contracts::record::Record;
use moni_contracts::record::RecordType;

/// 将记录和分类导出为 CSV 字符串。
pub fn generate(records: &[Record], categories: &[Category]) -> String {
    let mut output = String::new();
    output.push('\u{FEFF}'); // UTF-8 BOM for Excel compatibility
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
    output.push_str("分类ID,分类名称,描述,类型,图标\n");
    for cat in categories {
        output.push_str(&format!(
            "{},{},{},{},{}\n",
            cat.id,
            escape_csv_field(&cat.name),
            escape_csv_field(cat.description.as_deref().unwrap_or("")),
            match cat.category_type {
                RecordType::Income => "收入",
                RecordType::Expense => "支出",
            },
            cat.icon_name,
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

