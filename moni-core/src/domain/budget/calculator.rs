use std::collections::HashMap;

use moni_contracts::budget::BudgetStatus;
use moni_contracts::types::{AmountCents, CategoryId};
use rusqlite::Connection;

use crate::dto::{BudgetDto, CategoryDto};

/// 计算指定月份所有分类的支出金额（仅 expense，按 category_id 分组）。
/// 返回 HashMap<category_id, spent_cents>。
pub fn compute_category_spending(
    conn: &Connection,
    year_month: &str,
) -> Result<HashMap<CategoryId, AmountCents>, rusqlite::Error> {
    let mut stmt = conn.prepare(
        "SELECT category_id, SUM(amount_cents) as total
         FROM records
         WHERE record_type = 'expense'
           AND strftime('%Y-%m', datetime(created_at, 'unixepoch')) = ?1
         GROUP BY category_id",
    )?;
    let rows = stmt.query_map([year_month], |row| {
        Ok((row.get::<_, i64>(0)?, row.get::<_, Option<AmountCents>>(1)?.unwrap_or(0)))
    })?;
    rows.collect()
}

/// 计算某一级预算范围的总已用金额。
/// = 直接挂在一级下的支出 + 所有子分类支出。
fn compute_parent_spent(
    parent_id: CategoryId,
    categories: &[CategoryDto],
    category_spending: &HashMap<CategoryId, AmountCents>,
) -> AmountCents {
    let mut total = category_spending.get(&parent_id).copied().unwrap_or(0);
    for cat in categories {
        if cat.parent_id == Some(parent_id) {
            total += category_spending.get(&cat.id).copied().unwrap_or(0);
        }
    }
    total
}

/// 计算总预算的已用金额（当月所有支出之和）。
fn compute_total_spent(category_spending: &HashMap<CategoryId, AmountCents>) -> AmountCents {
    category_spending.values().sum()
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
) -> Option<AmountCents> {
    let mut remainings: Vec<AmountCents> = Vec::new();

    // 1. 总预算（category_id = None）
    if let Some(total_budget) = budgets.iter().find(|b| b.category_id.is_none()) {
        let total_spent = compute_total_spent(category_spending);
        remainings.push(total_budget.amount_cents - total_spent);
    }

    // 2. 查找当前分类
    let category = categories.iter().find(|c| c.id == category_id)?;

    if let Some(parent_id) = category.parent_id {
        // 二级分类路径
        // 父一级预算
        if let Some(parent_budget) = budgets.iter().find(|b| b.category_id == Some(parent_id)) {
            let parent_spent = compute_parent_spent(parent_id, categories, category_spending);
            remainings.push(parent_budget.amount_cents - parent_spent);
        }
        // 二级预算自身
        if let Some(self_budget) = budgets.iter().find(|b| b.category_id == Some(category_id)) {
            let self_spent = category_spending.get(&category_id).copied().unwrap_or(0);
            remainings.push(self_budget.amount_cents - self_spent);
        }
    } else {
        // 一级分类路径
        if let Some(self_budget) = budgets.iter().find(|b| b.category_id == Some(category_id)) {
            let self_spent = compute_parent_spent(category_id, categories, category_spending);
            remainings.push(self_budget.amount_cents - self_spent);
        }
    }

    if remainings.is_empty() {
        None
    } else {
        Some(remainings.into_iter().min().unwrap())
    }
}

/// 判断瓶颈层级（路径上最紧张的那条预算线）。
/// 返回 "total" | "parent" | "self" 之一。
pub fn bottleneck_budget(
    category_id: CategoryId,
    budgets: &[BudgetDto],
    categories: &[CategoryDto],
    category_spending: &HashMap<CategoryId, AmountCents>,
) -> Option<String> {
    // 使用元组存储 (remaining, bottleneck_name)，避免 unused_assignments 警告
    let mut best: Option<(AmountCents, String)> = None;

    // 总预算
    if let Some(total_budget) = budgets.iter().find(|b| b.category_id.is_none()) {
        let total_spent = compute_total_spent(category_spending);
        let remaining = total_budget.amount_cents - total_spent;
        best = Some((remaining, "total".to_string()));
    }

    let category = categories.iter().find(|c| c.id == category_id)?;

    if let Some(parent_id) = category.parent_id {
        // 二级路径：先检查父一级
        if let Some(parent_budget) = budgets.iter().find(|b| b.category_id == Some(parent_id)) {
            let parent_spent = compute_parent_spent(parent_id, categories, category_spending);
            let remaining = parent_budget.amount_cents - parent_spent;
            if best.as_ref().map_or(true, |(min, _)| remaining < *min) {
                best = Some((remaining, "parent".to_string()));
            }
        }
        // 再检查二级自身
        if let Some(self_budget) = budgets.iter().find(|b| b.category_id == Some(category_id)) {
            let self_spent = category_spending.get(&category_id).copied().unwrap_or(0);
            let remaining = self_budget.amount_cents - self_spent;
            if best.as_ref().map_or(true, |(min, _)| remaining < *min) {
                best = Some((remaining, "self".to_string()));
            }
        }
    } else {
        // 一级路径
        if let Some(self_budget) = budgets.iter().find(|b| b.category_id == Some(category_id)) {
            let self_spent = compute_parent_spent(category_id, categories, category_spending);
            let remaining = self_budget.amount_cents - self_spent;
            if best.as_ref().map_or(true, |(min, _)| remaining < *min) {
                best = Some((remaining, "self".to_string()));
            }
        }
    }

    best.map(|(_, name)| name)
}

/// 计算预算状态。
fn budget_status(percentage: f64) -> BudgetStatus {
    BudgetStatus::from_percentage(percentage)
}

/// 构建完整的 BudgetDto 列表（含实时计算字段）。
pub fn build_budget_dtos(
    conn: &Connection,
    budgets: &[moni_contracts::budget::Budget],
    categories: &[CategoryDto],
    year_month: &str,
) -> Result<Vec<BudgetDto>, rusqlite::Error> {
    let category_spending = compute_category_spending(conn, year_month)?;

    let mut dtos: Vec<BudgetDto> = budgets
        .iter()
        .map(|b| {
            let mut dto = BudgetDto::from_budget(b);

            // 填充分类名称
            if let Some(cid) = b.category_id {
                if let Some(cat) = categories.iter().find(|c| c.id == cid) {
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
                if let Some(cat) = categories.iter().find(|c| c.id == cid) {
                    if cat.parent_id.is_some() {
                        // 二级预算：该二级分类自身支出
                        category_spending.get(&cid).copied().unwrap_or(0)
                    } else {
                        // 一级预算：直接挂在一级下的 + 所有子分类
                        compute_parent_spent(cid, categories, &category_spending)
                    }
                } else {
                    0
                }
            } else {
                0
            };

            dto.spent_cents = spent;
            dto.remaining_cents = b.amount_cents - spent;

            // 计算使用率（防止除零，amount_cents > 0 由 schema 保证）
            #[allow(clippy::cast_precision_loss)]
            let percentage = (spent as f64) / (b.amount_cents as f64);
            dto.percentage = percentage;
            dto.status = budget_status(percentage).as_str().to_string();

            dto
        })
        .collect();

    // 排序：总预算在前，然后按一级分类排序，二级分类紧随其后
    dtos.sort_by(|a, b| {
        match (a.category_id, b.category_id) {
            (None, None) => std::cmp::Ordering::Equal,
            (None, Some(_)) => std::cmp::Ordering::Less,
            (Some(_), None) => std::cmp::Ordering::Greater,
            (Some(a_id), Some(b_id)) => {
                let a_is_child = categories.iter().any(|c| c.id == a_id && c.parent_id.is_some());
                let b_is_child = categories.iter().any(|c| c.id == b_id && c.parent_id.is_some());

                if a_is_child && b_is_child {
                    // 都是二级：按父分类排序
                    let a_parent = categories.iter().find(|c| c.id == a_id).and_then(|c| c.parent_id);
                    let b_parent = categories.iter().find(|c| c.id == b_id).and_then(|c| c.parent_id);
                    a_parent.cmp(&b_parent).then_with(|| a_id.cmp(&b_id))
                } else if a_is_child {
                    // a 是二级，b 是一级
                    let a_parent = categories.iter().find(|c| c.id == a_id).and_then(|c| c.parent_id);
                    if a_parent == Some(b_id) {
                        std::cmp::Ordering::Greater // 子分类排父分类后面
                    } else {
                        a_parent.cmp(&Some(b_id))
                    }
                } else if b_is_child {
                    // a 是一级，b 是二级
                    let b_parent = categories.iter().find(|c| c.id == b_id).and_then(|c| c.parent_id);
                    if b_parent == Some(a_id) {
                        std::cmp::Ordering::Less // 父分类排子分类前面
                    } else {
                        Some(a_id).cmp(&b_parent)
                    }
                } else {
                    // 都是一级：按 ID 排序（或按 sort_order）
                    a_id.cmp(&b_id)
                }
            }
        }
    });

    Ok(dtos)
}
