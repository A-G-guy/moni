pub use moni_contracts::types::*;

/// 从 UTC 时间戳推导 year_month（"YYYY-MM" 格式）。
pub fn year_month_from_timestamp(ts: i64) -> String {
    chrono::DateTime::from_timestamp(ts, 0)
        .map(|dt| dt.format("%Y-%m").to_string())
        .unwrap_or_else(|| {
            log::warn!("无法从时间戳 {} 推导 year_month，回退到当前 UTC 月份", ts);
            chrono::Utc::now().format("%Y-%m").to_string()
        })
}
