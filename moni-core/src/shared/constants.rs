/// 自定义分类的默认排序值，确保排在预设分类之后。
pub const CUSTOM_CATEGORY_SORT_ORDER: i32 = 999;

/// 分类描述的最大字符长度。
pub const CATEGORY_DESCRIPTION_MAX_LEN: usize = 200;

/// 导出数据时单次查询的最大记录数。
pub const EXPORT_MAX_RECORDS: i32 = 10000;

/// 分页查询的最大页大小。
pub const MAX_PAGE_SIZE: u32 = 1000;

/// 分页查询的最小页大小。
pub const MIN_PAGE_SIZE: u32 = 1;

/// 默认分页大小。
pub const DEFAULT_PAGE_SIZE: u32 = 20;

/// 预设分类的数量。
pub const PRESET_CATEGORY_COUNT: usize = 12;
