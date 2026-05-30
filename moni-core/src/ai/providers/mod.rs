use crate::ai::domain::{ProviderPreset, ThinkingLevel};
use crate::ai::errors::AiError;
use crate::ai::http::HttpClient;
use crate::ai::schema::parse_bookkeeping_output;

/// Provider adapter 的统一接口。
pub trait ProviderAdapter {
    fn parse_bookkeeping(
        &self,
        http_client: &dyn HttpClient,
        preset: &ProviderPreset,
        input: &str,
        category_context: &str,
    ) -> Result<crate::ai::domain::AiBookkeepingParseResult, AiError>;
}

pub mod gemini;
pub mod openai_chat;

pub(crate) fn base_prompt(category_context: &str) -> String {
    format!(
        "你是 Moni 记账助手。请从用户自然语言中提取记账信息，并且只输出符合 JSON Schema 的 JSON。\n\
         要求：金额单位转换为分；支出 record_type 为 expense，收入为 income；无法确定分类时 category_id 用 -1；非记账内容 is_bookkeeping=false，金额填 0，备注填空字符串。\n\
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
