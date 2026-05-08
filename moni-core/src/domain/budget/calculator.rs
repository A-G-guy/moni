use std::collections::HashMap;

use chrono::Datelike;
use moni_contracts::budget::BudgetStatus;
use moni_contracts::types::{AmountCents, CategoryId};
use rusqlite::Connection;

use crate::dto::{BudgetDto, CategoryDto};
use crate::shared::date_utils;

/// 计算指定月份所有分类的支出金额（仅 expense，按 category_id 分组）。
/// 返回 HashMap<category_id, spent_cents>。
/// 使用 `strftime` 动态计算本地时区年月，避免依赖持久化列值的一致性。
pub fn compute_category_spending(
    conn: &Connection,
    year_month: &str,
) -> Result<HashMap<CategoryId, AmountCents>, rusqlite::Error> {
    let mut stmt = conn.prepare(
        "SELECT category_id, SUM(amount_cents) as total
         FROM records
         WHERE record_type = 'expense'
           AND strftime('%Y-%m', datetime(created_at, 'unixepoch', 'localtime')) = ?1
         GROUP BY category_id",
    )?;
    let rows = stmt.query_map([year_month], |row| {
        Ok((row.get::<_, i64>(0)?, row.get::<_, Option<AmountCents>>(1)?.unwrap_or(0)))
    })?;
    rows.collect()
}

/// 计算指定月份按 parent_category_id 聚合的支出金额（仅 expense）。
/// 返回 HashMap<parent_category_id, spent_cents>。
/// 用于一级预算的已用金额计算，基于账单发生时的父级关系（而非当前层级）。
/// 使用 `strftime` 动态计算本地时区年月，避免依赖持久化列值的一致性。
pub fn compute_parent_category_spending(
    conn: &Connection,
    year_month: &str,
) -> Result<HashMap<CategoryId, AmountCents>, rusqlite::Error> {
    let mut stmt = conn.prepare(
        "SELECT parent_category_id, SUM(amount_cents) as total
         FROM records
         WHERE record_type = 'expense'
           AND parent_category_id IS NOT NULL
           AND strftime('%Y-%m', datetime(created_at, 'unixepoch', 'localtime')) = ?1
         GROUP BY parent_category_id",
    )?;
    let rows = stmt.query_map([year_month], |row| {
        Ok((row.get::<_, i64>(0)?, row.get::<_, Option<AmountCents>>(1)?.unwrap_or(0)))
    })?;
    rows.collect()
}

/// 计算某一级预算范围的总已用金额。
/// = 直接挂在一级下的支出 + 所有当时归属该父级的子分类支出。
fn compute_parent_spent(
    parent_id: CategoryId,
    category_spending: &HashMap<CategoryId, AmountCents>,
    parent_category_spending: &HashMap<CategoryId, AmountCents>,
) -> AmountCents {
    let direct = category_spending.get(&parent_id).copied().unwrap_or(0);
    let children = parent_category_spending.get(&parent_id).copied().unwrap_or(0);
    direct.saturating_add(children)
}

/// 计算总预算的已用金额（当月所有支出之和）。
fn compute_total_spent(category_spending: &HashMap<CategoryId, AmountCents>) -> AmountCents {
    category_spending.values().fold(0i64, |a, b| a.saturating_add(*b))
}

/// 计算指定分类路径上的有效可用额度。
///
/// 规则：
/// - 二级分类：min(二级名义剩余, 父一级名义剩余, 总预算名义剩余)
/// - 一级分类：min(一级名义剩余, 总预算名义剩余)
/// - 跳过未设置的层级
/// - 返回 None 表示路径上无预算设置
pub fn effective_available(
    category_id: CategoryId,
    budgets: &[BudgetDto],
    categories: &[CategoryDto],
    category_spending: &HashMap<CategoryId, AmountCents>,
    parent_category_spending: &HashMap<CategoryId, AmountCents>,
) -> Option<AmountCents> {
    let mut remainings: Vec<AmountCents> = Vec::new();

    // 1. 总预算（category_id = None）
    if let Some(total_budget) = budgets.iter().find(|b| b.category_id.is_none()) {
        let total_spent = compute_total_spent(category_spending);
        remainings.push(total_budget.amount_cents.saturating_sub(total_spent));
    }

    // 2. 查找当前分类
    let category = categories.iter().find(|c| c.id == category_id)?;

    if let Some(parent_id) = category.parent_id {
        // 二级分类路径
        // 父一级预算
        if let Some(parent_budget) = budgets.iter().find(|b| b.category_id == Some(parent_id)) {
            let parent_spent = compute_parent_spent(parent_id, category_spending, parent_category_spending);
            remainings.push(parent_budget.amount_cents.saturating_sub(parent_spent));
        }
        // 二级预算自身
        if let Some(self_budget) = budgets.iter().find(|b| b.category_id == Some(category_id)) {
            let self_spent = category_spending.get(&category_id).copied().unwrap_or(0);
            remainings.push(self_budget.amount_cents.saturating_sub(self_spent));
        }
    } else {
        // 一级分类路径
        if let Some(self_budget) = budgets.iter().find(|b| b.category_id == Some(category_id)) {
            let self_spent = compute_parent_spent(category_id, category_spending, parent_category_spending);
            remainings.push(self_budget.amount_cents.saturating_sub(self_spent));
        }
    }

    if remainings.is_empty() {
        None
    } else {
        Some(remainings.into_iter().min().unwrap())
    }
}

/// 判断瓶颈层级（路径上最紧张的那条预算线）。
/// 返回 (瓶颈类型, 瓶颈分类名称)。
/// 瓶颈类型："total" | "parent" | "self"。
pub fn bottleneck_budget_with_name(
    category_id: CategoryId,
    budgets: &[BudgetDto],
    categories: &[CategoryDto],
    category_spending: &HashMap<CategoryId, AmountCents>,
    parent_category_spending: &HashMap<CategoryId, AmountCents>,
) -> (Option<String>, Option<String>) {
    // 使用元组存储 (remaining, bottleneck_type, bottleneck_category_name)
    let mut best: Option<(AmountCents, String, Option<String>)> = None;

    // 总预算
    if let Some(total_budget) = budgets.iter().find(|b| b.category_id.is_none()) {
        let total_spent = compute_total_spent(category_spending);
        let remaining = total_budget.amount_cents.saturating_sub(total_spent);
        best = Some((remaining, "total".to_string(), None));
    }

    let Some(category) = categories.iter().find(|c| c.id == category_id) else {
        let t = best.as_ref().map(|(_, t, _)| t.clone());
        let n = best.and_then(|(_, _, n)| n);
        return (t, n);
    };

    if let Some(parent_id) = category.parent_id {
        // 二级路径：先检查父一级
        if let Some(parent_budget) = budgets.iter().find(|b| b.category_id == Some(parent_id)) {
            let parent_spent = compute_parent_spent(parent_id, category_spending, parent_category_spending);
            let remaining = parent_budget.amount_cents.saturating_sub(parent_spent);
            let parent_name = categories
                .iter()
                .find(|c| c.id == parent_id)
                .map(|c| c.name.clone());
            if best.as_ref().map_or(true, |(min, _, _)| remaining < *min) {
                best = Some((remaining, "parent".to_string(), parent_name));
            }
        }
        // 再检查二级自身
        if let Some(self_budget) = budgets.iter().find(|b| b.category_id == Some(category_id)) {
            let self_spent = category_spending.get(&category_id).copied().unwrap_or(0);
            let remaining = self_budget.amount_cents.saturating_sub(self_spent);
            if best.as_ref().map_or(true, |(min, _, _)| remaining < *min) {
                best = Some((
                    remaining,
                    "self".to_string(),
                    Some(category.name.clone()),
                ));
            }
        }
    } else {
        // 一级路径
        if let Some(self_budget) = budgets.iter().find(|b| b.category_id == Some(category_id)) {
            let self_spent = compute_parent_spent(category_id, category_spending, parent_category_spending);
            let remaining = self_budget.amount_cents.saturating_sub(self_spent);
            if best.as_ref().map_or(true, |(min, _, _)| remaining < *min) {
                best = Some((
                    remaining,
                    "self".to_string(),
                    Some(category.name.clone()),
                ));
            }
        }
    }

    match best {
        Some((_, t, n)) => (Some(t), n),
        None => (None, None),
    }
}

/// 构建完整的 BudgetDto 列表（含实时计算字段）。
/// 同时返回 category_spending 和 parent_category_spending，供调用方复用，避免重复查询。
pub fn build_budget_dtos(
    conn: &Connection,
    budgets: &[moni_contracts::budget::Budget],
    categories: &[CategoryDto],
    year_month: &str,
    today: &str,
) -> Result<(Vec<BudgetDto>, HashMap<CategoryId, AmountCents>, HashMap<CategoryId, AmountCents>), rusqlite::Error> {
    let category_spending = compute_category_spending(conn, year_month)?;
    let parent_category_spending = compute_parent_category_spending(conn, year_month)?;

    // 预计算分类索引，避免排序时重复遍历
    let cat_by_id: std::collections::HashMap<CategoryId, &CategoryDto> =
        categories.iter().map(|c| (c.id, c)).collect();

    let mut dtos: Vec<BudgetDto> = budgets
        .iter()
        .map(|b| {
            let mut dto = BudgetDto::from_budget(b);

            // 填充分类名称
            if let Some(cid) = b.category_id {
                if let Some(cat) = cat_by_id.get(&cid) {
                    dto.category_name = Some(cat.name.clone());
                }
            } else {
                dto.category_name = Some("总预算".to_string());
            }

            // 计算已用金额
            let spent = if b.category_id.is_none() {
                // 总预算：当月所有支出
                compute_total_spent(&category_spending)
            } else if let Some(cid) = b.category_id {
                if let Some(cat) = cat_by_id.get(&cid) {
                    if cat.parent_id.is_some() {
                        // 二级预算：该二级分类自身支出
                        category_spending.get(&cid).copied().unwrap_or(0)
                    } else {
                        // 一级预算：直接挂在一级下的 + 当时归属该父级的所有子分类支出
                        compute_parent_spent(cid, &category_spending, &parent_category_spending)
                    }
                } else {
                    0
                }
            } else {
                0
            };

            dto.spent_cents = spent;
            dto.remaining_cents = b.amount_cents.saturating_sub(spent);

            // 计算使用率（防御除零，虽然 amount_cents > 0 由 schema 保证）
            #[allow(clippy::cast_precision_loss)]
            let percentage = if b.amount_cents > 0 {
                (spent as f64) / (b.amount_cents as f64)
            } else {
                0.0
            };
            dto.percentage = percentage;
            dto.status = BudgetStatus::from_percentage(percentage).as_str().to_string();

            // 仅对总预算计算进度状态（需要 elapsed_days / total_days）
            if b.category_id.is_none() {
                dto.progress_status = compute_budget_progress_status(
                    spent, b.amount_cents, year_month, today,
                );
            }

            dto
        })
        .collect();

    // 排序：总预算在前，然后按一级分类 sort_order 排序，二级分类紧随其父分类
    dtos.sort_by(|a, b| {
        match (a.category_id, b.category_id) {
            (None, None) => std::cmp::Ordering::Equal,
            (None, Some(_)) => std::cmp::Ordering::Less,
            (Some(_), None) => std::cmp::Ordering::Greater,
            (Some(a_id), Some(b_id)) => {
                let a_is_child = cat_by_id.get(&a_id).map_or(false, |c| c.parent_id.is_some());
                let b_is_child = cat_by_id.get(&b_id).map_or(false, |c| c.parent_id.is_some());

                if a_is_child && b_is_child {
                    // 都是二级：先按父分类排序，再按自身排序
                    let a_parent = cat_by_id.get(&a_id).and_then(|c| c.parent_id);
                    let b_parent = cat_by_id.get(&b_id).and_then(|c| c.parent_id);
                    a_parent.cmp(&b_parent).then_with(|| a_id.cmp(&b_id))
                } else if a_is_child {
                    // a 是二级，b 是一级
                    let a_parent = cat_by_id.get(&a_id).and_then(|c| c.parent_id);
                    if a_parent == Some(b_id) {
                        std::cmp::Ordering::Greater // 子分类排父分类后面
                    } else {
                        a_parent.cmp(&Some(b_id))
                    }
                } else if b_is_child {
                    // a 是一级，b 是二级
                    let b_parent = cat_by_id.get(&b_id).and_then(|c| c.parent_id);
                    if b_parent == Some(a_id) {
                        std::cmp::Ordering::Less // 父分类排子分类前面
                    } else {
                        Some(a_id).cmp(&b_parent)
                    }
                } else {
                    // 都是一级：按 sort_order 排序，相同时 fallback 到 id
                    let a_order = cat_by_id.get(&a_id).map(|c| c.sort_order).unwrap_or(0);
                    let b_order = cat_by_id.get(&b_id).map(|c| c.sort_order).unwrap_or(0);
                    a_order.cmp(&b_order).then_with(|| a_id.cmp(&b_id))
                }
            }
        }
    });

    Ok((dtos, category_spending, parent_category_spending))
}

/// 计算总预算进度状态。
/// 基于实际支出比例与理想时间进度的对比。
fn compute_budget_progress_status(
    spent_cents: AmountCents,
    amount_cents: AmountCents,
    year_month: &str,
    today: &str,
) -> Option<String> {
    let (sel_year, sel_month) = date_utils::parse_year_month(year_month)?;
    let total_days = date_utils::days_in_month(sel_year, sel_month);

    let today_date = chrono::NaiveDate::parse_from_str(today, "%Y-%m-%d").ok()?;
    let today_year = today_date.year();
    let today_month = today_date.month();
    let today_day = today_date.day();

    let (elapsed_days, _) = date_utils::calculate_day_counts(
        sel_year, sel_month, today_year, today_month, today_day, total_days,
    );

    #[allow(clippy::cast_precision_loss)]
    let actual_percentage = if amount_cents > 0 {
        spent_cents as f64 / amount_cents as f64
    } else {
        0.0
    };
    #[allow(clippy::cast_precision_loss)]
    let ideal_percentage = if total_days > 0 {
        elapsed_days as f64 / total_days as f64
    } else {
        0.0
    };

    Some(if actual_percentage > ideal_percentage {
        "overrun".to_string()
    } else if actual_percentage > ideal_percentage * 0.9 {
        "warning".to_string()
    } else {
        "normal".to_string()
    })
}
