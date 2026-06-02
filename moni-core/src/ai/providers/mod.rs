use crate::ai::domain::{AiBookkeepingParseRequest, ProviderPreset, ThinkingLevel};
use crate::ai::errors::AiError;
use crate::ai::http::HttpClient;
use crate::ai::schema::parse_bookkeeping_output;

/// Provider adapter 的统一接口。
pub trait ProviderAdapter {
    fn parse_bookkeeping(
        &self,
        http_client: &dyn HttpClient,
        preset: &ProviderPreset,
        request: &AiBookkeepingParseRequest,
        category_context: &str,
    ) -> Result<crate::ai::domain::AiBookkeepingParseResult, AiError>;
}

pub mod gemini;
pub mod openai_chat;

pub(crate) fn base_prompt(category_context: &str) -> String {
    format!(
        "你是 Moni 记账助手，只做单轮记账信息提取。请只根据本次用户文字和本次附带图片判断，不要假设任何历史上下文。\n\
         你必须只输出符合 JSON Schema 的 JSON，不要输出 Markdown、解释或额外文本。\n\
         规则：金额统一转换为分；支出 record_type=expense，收入 record_type=income；非记账内容 is_bookkeeping=false、amount_cents=0、note 为空字符串。\n\
         分类规则：只能从下方可用分类中选择 active 分类。能够确定二级分类时直接返回二级分类 id；无法确定具体二级分类，或该一级分类没有二级分类时，返回最匹配的一级分类 id；完全不知道属于哪个分类时 category_id=-1。\n\
         备注规则：备注 note 应提取用户文字或图片票据中的具体商户、商品、餐品或场景，不要只写泛化分类名。例如小微家盖饭的发票，note 应写“小微家盖饭”。\n\
         时间规则：用户消息中会包含本次请求发送时间。若文字或图片凭证中存在明确日期/时间，以凭证为准；若只有“今天、现在、刚刚”等相对时间，按发送时间解释；若没有任何时间线索，一般按发送当天当时返回 timestamp。\n\
         回复规则：reply_text 只能说明“已整理为待确认卡片”，不得说“已记录、已保存、已入账、成功记账”，因为正式入账必须由用户点击确认保存。\n\
         图片规则：如果有图片，请识别票据/截图中的金额、商户、时间和商品；如果文字与图片同时存在，应综合判断，用户文字对模糊信息有更高优先级。\n\
         可用分类：\n{category_context}"
    )
}

pub(crate) fn map_thinking_level(level: &ThinkingLevel) -> Option<serde_json::Value> {
    match level {
        ThinkingLevel::Off => None,
        ThinkingLevel::Low => Some(serde_json::json!({ "effort": "low" })),
        ThinkingLevel::Medium => Some(serde_json::json!({ "effort": "medium" })),
        ThinkingLevel::High => Some(serde_json::json!({ "effort": "high" })),
    }
}

pub(crate) fn handle_http_error(status: u16, body: &str) -> AiError {
    match status {
        401 | 403 => AiError::Auth(status),
        429 => AiError::RateLimited,
        500..=599 => AiError::Server {
            status_code: status,
            detail: body.chars().take(500).collect(),
        },
        _ => AiError::Server {
            status_code: status,
            detail: body.chars().take(500).collect(),
        },
    }
}

pub(crate) fn parse_json_output(
    text: &str,
) -> Result<crate::ai::domain::AiBookkeepingParseResult, AiError> {
    parse_bookkeeping_output(text)
}
