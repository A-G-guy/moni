/// 解析 "YYYY-MM" 字符串为 (year, month) 元组。
pub fn parse_year_month(year_month_str: &str) -> Option<(i32, u32)> {
    let parts: Vec<&str> = year_month_str.split('-').collect();
    if parts.len() != 2 {
        return None;
    }
    let year = parts[0].parse::<i32>().ok()?;
    let month = parts[1].parse::<u32>().ok()?;
    if !(1..=12).contains(&month) {
        return None;
    }
    Some((year, month))
}

/// 计算指定年月的总天数。
pub fn days_in_month(year: i32, month: u32) -> u32 {
    match month {
        1 | 3 | 5 | 7 | 8 | 10 | 12 => 31,
        4 | 6 | 9 | 11 => 30,
        2 => {
            if is_leap_year(year) {
                29
            } else {
                28
            }
        }
        _ => 30,
    }
}

/// 判断是否为闰年。
pub fn is_leap_year(year: i32) -> bool {
    (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}

/// 计算已过天数与剩余天数。
///
/// 规则：
/// - 选中月 == 当前月：elapsed = today_day，remaining = total_days - today_day
/// - 选中月 < 当前月：elapsed = total_days，remaining = 0（已过完）
/// - 选中月 > 当前月：elapsed = 0，remaining = total_days（未来月）
pub fn calculate_day_counts(
    sel_year: i32,
    sel_month: u32,
    today_year: i32,
    today_month: u32,
    today_day: u32,
    total_days: u32,
) -> (i32, i32) {
    if sel_year == today_year && sel_month == today_month {
        let elapsed = today_day.max(1) as i32;
        let remaining = (total_days - today_day).max(0) as i32;
        (elapsed, remaining)
    } else if sel_year < today_year || (sel_year == today_year && sel_month < today_month) {
        (total_days as i32, 0)
    } else {
        (0, total_days as i32)
    }
}
