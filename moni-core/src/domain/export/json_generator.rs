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

