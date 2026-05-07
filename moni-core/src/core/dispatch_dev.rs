use crate::core::error::CoreError;
use crate::core::runtime::AppCoreRuntime;
use crate::db::{category_repo, record_repo};
use crate::models::effects::CoreUpdate;
use crate::models::intent::CoreIntent;

impl AppCoreRuntime {
    // 单元 enum 分支不消费 intent，但其他分支按值解构 String 字段，整体最直观签名仍为按值
    #[allow(clippy::needless_pass_by_value)]
    pub(super) fn dispatch_dev(&mut self, intent: CoreIntent) -> Result<CoreUpdate, CoreError> {
        match intent {
            CoreIntent::DevClearAllData => {
                log::warn!("执行清空所有数据");

                // 先删除记录（避免外键约束冲突）
                self.conn
                    .execute("DELETE FROM records;", [])
                    .map_err(|e| CoreError::Internal(format!("清空记录失败: {e}")))?;

                // 先删除预算（避免 categories 删除时外键约束干扰）
                self.conn
                    .execute("DELETE FROM budgets;", [])
                    .map_err(|e| CoreError::Internal(format!("清空预算失败: {e}")))?;

                // 先删除子分类，再删除一级分类（避免 parent_id 外键约束冲突）
                self.conn
                    .execute("DELETE FROM categories WHERE parent_id IS NOT NULL;", [])
                    .map_err(|e| CoreError::Internal(format!("清空子分类失败: {e}")))?;
                self.conn
                    .execute("DELETE FROM categories;", [])
                    .map_err(|e| CoreError::Internal(format!("清空分类失败: {e}")))?;

                // 压缩数据库
                self.conn
                    .execute("VACUUM;", [])
                    .map_err(|e| CoreError::Internal(format!("VACUUM 失败: {e}")))?;

                // 重置状态
                self.state = crate::models::state::AppState::default();

                log::info!("所有数据已清空");
                self.finish(Vec::new())
            }
            CoreIntent::DevSeedPresets => {
                log::info!("执行重置预设分类");

                category_repo::seed_presets(&self.conn)
                    .map_err(|e| CoreError::Internal(format!("填充预设分类失败: {e}")))?;

                let categories = category_repo::list_all(&self.conn)
                    .map_err(|e| CoreError::Internal(format!("加载分类失败: {e}")))?;
                self.state.categories = categories
                    .iter()
                    .map(crate::dto::CategoryDto::from_category)
                    .collect();

                log::info!("预设分类已重置");
                self.finish(vec![crate::models::effects::CoreEffect {
                    kind: "show_snackbar".to_string(),
                    payload_json: r#"{"message":"预设分类已重置"}"#.to_string(),
                }])
            }
            CoreIntent::DevGenerateMockData { count, preset } => {
                log::info!("生成 Mock 数据: count={count}, preset={preset}");

                let categories = category_repo::list_all(&self.conn)
                    .map_err(|e| CoreError::Internal(format!("加载分类失败: {e}")))?;
                if categories.is_empty() {
                    return Err(CoreError::Internal(
                        "没有可用分类，无法生成 Mock 数据".to_string(),
                    ));
                }

                let records =
                    crate::domain::dev::mock_data_generator::generate(&categories, count, preset)
                        .map_err(|e| CoreError::Internal(format!("生成 Mock 数据失败: {e}")))?;

                for record in &records {
                    let cat = categories.iter().find(|c| c.id == record.category_id);
                    record_repo::insert(
                        &self.conn,
                        record.amount_cents,
                        record.record_type,
                        record.category_id,
                        cat.and_then(|c| c.parent_id),
                        &record.note,
                        Some(record.created_at),
                    )
                    .map_err(|e| CoreError::Internal(format!("插入 Mock 记录失败: {e}")))?;
                }

                // 刷新记录列表到状态
                let all_records = record_repo::list_paginated(&self.conn, 0, 50)
                    .map_err(|e| CoreError::Internal(format!("加载记录失败: {e}")))?;
                self.state.records =
                    crate::dto::record_list_to_dto(&all_records, &self.state.categories);
                self.state.record_groups = crate::dto::group_records_by_date(&self.state.records);

                // 刷新月度统计
                let aggregates = record_repo::monthly_aggregates(&self.conn, 6)
                    .map_err(|e| CoreError::Internal(format!("刷新月度统计失败: {e}")))?;
                self.state.monthly_summaries =
                    crate::domain::stats::calculator::calculate_monthly_summary(aggregates);

                log::info!("Mock 数据生成完成: {} 条", records.len());
                self.finish(vec![crate::models::effects::CoreEffect {
                    kind: "show_snackbar".to_string(),
                    payload_json: format!(r#"{{"message":"已生成 {} 条测试数据"}}"#, records.len()),
                }])
            }
            _ => {
                log::warn!("开发者模块收到未支持的意图类型");
                Err(CoreError::Internal("未支持的意图类型".to_string()))
            }
        }
    }
}
