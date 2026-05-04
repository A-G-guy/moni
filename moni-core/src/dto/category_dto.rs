use moni_contracts::record::RecordType;

/// 前端消费用的分类 DTO，不包含内部时间戳字段。
#[derive(Debug, Clone, serde::Serialize, serde::Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CategoryDto {
    pub id: i64,
    pub name: String,
    pub description: Option<String>,
    pub category_type: RecordType,
    pub icon_name: String,
    pub sort_order: i32,
    pub is_preset: bool,
    pub archived_at: Option<i64>,
}

impl CategoryDto {
    pub fn from_category(category: &moni_contracts::category::Category) -> Self {
        Self {
            id: category.id,
            name: category.name.clone(),
            description: category.description.clone(),
            category_type: category.category_type,
            icon_name: category.icon_name.clone(),
            sort_order: category.sort_order,
            is_preset: category.is_preset,
            archived_at: category.archived_at,
        }
    }
}

/// 将分类实体列表批量转换为 DTO 列表。
pub fn category_list_to_dto(categories: &[moni_contracts::category::Category]) -> Vec<CategoryDto> {
    categories.iter().map(CategoryDto::from_category).collect()
}