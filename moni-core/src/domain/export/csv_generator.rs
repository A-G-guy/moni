use std::fmt::Write as _;

use moni_contracts::category::Category;
use moni_contracts::record::Record;
use moni_contracts::record::RecordType;

/// 将记录和分类导出为 CSV 字符串。
pub fn generate(records: &[Record], categories: &[Category]) -> String {
    let mut output = String::new();
    output.push('\u{FEFF}'); // UTF-8 BOM for Excel compatibility
    output.push_str("ID,金额(分),类型,分类,备注,创建时间(Unix秒),更新时间(Unix秒)\n");

    for rec in records {
        let type_label = match rec.record_type {
            RecordType::Income => "收入",
            RecordType::Expense => "支出",
        };
        let escaped_note = escape_csv_field(&rec.note);
        // 用 write! 直接写入，避免 push_str + format! 的额外分配
        let _ = writeln!(
            output,
            "{},{},{},{},{},{},{}",
            rec.id,
            rec.amount_cents,
            type_label,
            rec.category_id,
            escaped_note,
            rec.created_at,
            rec.updated_at,
        );
    }

    output.push('\n');
    output.push_str("分类ID,分类名称,描述,类型,图标\n");
    for cat in categories {
        let escaped_name = escape_csv_field(&cat.name);
        let escaped_desc = escape_csv_field(cat.description.as_deref().unwrap_or(""));
        let type_label = match cat.category_type {
            RecordType::Income => "收入",
            RecordType::Expense => "支出",
        };
        let _ = writeln!(
            output,
            "{},{},{},{},{}",
            cat.id, escaped_name, escaped_desc, type_label, cat.icon_name,
        );
    }

    output
}

fn escape_csv_field(field: &str) -> String {
    if field.contains(',') || field.contains('"') || field.contains('\n') {
        let escaped = field.replace('"', "\"\"");
        format!("\"{escaped}\"")
    } else {
        field.to_string()
    }
}
