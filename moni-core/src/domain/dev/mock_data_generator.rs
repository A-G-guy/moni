use moni_contracts::category::Category;
use moni_contracts::record::RecordType;
use moni_contracts::types::{AmountCents, TimestampSec};
use rand::Rng;
use rand::seq::SliceRandom;

use crate::models::intent::MockPreset;

/// 待插入的 Mock 记录。
#[derive(Debug)]
pub struct MockRecord {
    pub amount_cents: AmountCents,
    pub record_type: RecordType,
    pub category_id: i64,
    pub note: String,
    pub created_at: TimestampSec,
}

/// 根据现有分类生成 Mock 记录列表。
pub fn generate(
    categories: &[Category],
    count: u32,
    preset: MockPreset,
) -> Result<Vec<MockRecord>, String> {
    if categories.is_empty() {
        return Err("没有可用分类".to_string());
    }

    let mut rng = rand::thread_rng();
    let mut records = Vec::with_capacity(count as usize);
    let now = chrono::Utc::now().timestamp();

    for i in 0..count {
        let category = categories
            .choose(&mut rng)
            .ok_or_else(|| "分类列表为空，无法生成 Mock 记录".to_string())?;
        let (amount_cents, note, record_type) = match preset {
            MockPreset::Normal => generate_normal(&mut rng, category, i),
            MockPreset::Stress => generate_stress(&mut rng, category, i),
        };

        // 随机日期：过去 90 天内
        let days_ago = rng.gen_range(0..90);
        let created_at = now - days_ago * 86400 + rng.gen_range(-3600..3600);

        records.push(MockRecord {
            amount_cents,
            record_type,
            category_id: category.id,
            note,
            created_at,
        });
    }

    // 按创建时间降序排列（与真实数据一致）
    records.sort_by(|a, b| b.created_at.cmp(&a.created_at));
    Ok(records)
}

fn generate_normal(
    rng: &mut impl Rng,
    category: &Category,
    _index: u32,
) -> (AmountCents, String, RecordType) {
    let amount_cents = rng.gen_range(10..10000);
    let note = format!("{}记录", category.name);
    let record_type = category.category_type;
    (amount_cents, note, record_type)
}

fn generate_stress(
    rng: &mut impl Rng,
    category: &Category,
    index: u32,
) -> (AmountCents, String, RecordType) {
    let amount_cents = match index % 4 {
        0 => 99_999_999, // 超大金额
        1 => 1,          // 极小金额
        _ => rng.gen_range(10..10000),
    };

    let note = match index % 5 {
        0 => "a".repeat(500), // 超长无空格文本
        1 => "🎉💰🚀测试备注包含Emoji和特殊符号✨📝".to_string(),
        2 => "VeryLongEnglishWordWithoutAnySpace".repeat(20),
        3 => String::new(), // 空备注
        _ => format!("{}记录", category.name),
    };

    let record_type = category.category_type;
    (amount_cents, note, record_type)
}
