/// 自定义分类的默认排序值，确保排在预设分类之后。
pub const CUSTOM_CATEGORY_SORT_ORDER: i32 = 999;

/// 分类描述的最大字符长度。
pub const CATEGORY_DESCRIPTION_MAX_LEN: usize = 200;

/// 分页查询的最大页大小。
pub const MAX_PAGE_SIZE: u32 = 1000;

/// 分页查询的最小页大小。
pub const MIN_PAGE_SIZE: u32 = 1;

/// 默认分页大小。
pub const DEFAULT_PAGE_SIZE: u32 = 20;

/// 预设分类的数量（12 个一级 + 6 个二级）。
pub const PRESET_CATEGORY_COUNT: usize = 18;

/// 预算警告临界阈值（80%）
pub const BUDGET_CRITICAL_THRESHOLD: f64 = 0.80;
/// 预算超支阈值（100%）
pub const BUDGET_OVERRUN_THRESHOLD: f64 = 1.00;

/// 记录备注的最大字符长度。
pub const NOTE_MAX_CHARS: usize = 2000;
