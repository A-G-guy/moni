use rusqlite::Connection;

use crate::ai::domain::{AiBookkeepingParseResult, ApiFormat};
use crate::ai::errors::AiError;
use crate::ai::http::{DefaultHttpClient, HttpClient};
use crate::ai::preset_repo;
use crate::ai::providers::ProviderAdapter;
use crate::ai::providers::gemini::GeminiAdapter;
use crate::ai::providers::openai_chat::OpenAiChatAdapter;
use crate::db::category_repo;

/// AI 记账服务编排层。
#[derive(Debug, Default)]
pub struct AiBookkeepingService;

impl AiBookkeepingService {
    pub fn parse_with_default(
        conn: &Connection,
        input: &str,
    ) -> Result<AiBookkeepingParseResult, AiError> {
        let preset = preset_repo::get_default(conn)?.ok_or(AiError::NoDefaultPreset)?;
        if preset.api_key.trim().is_empty() {
            return Err(AiError::MissingApiKey);
        }
        let category_context = build_category_context(conn)?;
        let http_client = DefaultHttpClient;
        parse_with_client(&http_client, &preset, input, &category_context)
    }

    pub fn test_connection(conn: &Connection, id: i64) -> Result<serde_json::Value, AiError> {
        let preset = preset_repo::get(conn, id)?.ok_or(AiError::PresetNotFound(id))?;
        if preset.api_key.trim().is_empty() {
            return Err(AiError::MissingApiKey);
        }
        let http_client = DefaultHttpClient;
        let result = parse_with_client(
            &http_client,
            &preset,
            "测试：午餐花了 1 元",
            "1 餐饮 expense",
        )?;
        Ok(serde_json::json!({
            "ok": true,
            "is_bookkeeping": result.is_bookkeeping,
            "reply_text": result.reply_text
        }))
    }
}

pub fn parse_with_client(
    http_client: &dyn HttpClient,
    preset: &crate::ai::domain::ProviderPreset,
    input: &str,
    category_context: &str,
) -> Result<AiBookkeepingParseResult, AiError> {
    match preset.api_format {
        ApiFormat::OpenAiChatCompletions => {
            OpenAiChatAdapter.parse_bookkeeping(http_client, preset, input, category_context)
        }
        ApiFormat::GeminiGenerateContent => {
            GeminiAdapter.parse_bookkeeping(http_client, preset, input, category_context)
        }
    }
}

pub(crate) fn build_category_context(conn: &Connection) -> Result<String, AiError> {
    let categories = category_repo::list_all(conn)
        .map_err(|error| AiError::InvalidPreset(format!("读取分类失败: {error}")))?;
    let mut lines = categories
        .iter()
        .map(|category| {
            format!(
                "{} {} {}",
                category.id,
                category.name,
                match category.category_type {
                    moni_contracts::record::RecordType::Income => "income",
                    moni_contracts::record::RecordType::Expense => "expense",
                }
            )
        })
        .collect::<Vec<_>>();
    if lines.is_empty() {
        lines.push("1 未分类 expense".to_string());
    }
    Ok(lines.join("\n"))
}

#[cfg(test)]
mod tests {
    use serde_json::json;

    use super::parse_with_client;
    use crate::ai::domain::{ApiFormat, ProviderPreset, ThinkingLevel};
    use crate::ai::errors::AiError;
    use crate::ai::http::{HttpClient, HttpRequest, HttpResponse};

    #[derive(Debug)]
    struct FakeHttpClient;

    impl HttpClient for FakeHttpClient {
        fn post_json(&self, _request: &HttpRequest) -> Result<HttpResponse, AiError> {
            Ok(HttpResponse {
                status: 200,
                body: json!({
                    "choices": [{
                        "message": {
                            "content": "{\"is_bookkeeping\":true,\"reply_text\":\"ok\",\"amount_cents\":100,\"record_type\":\"expense\",\"category_id\":1,\"account_id\":null,\"timestamp\":null,\"note\":\"测试\",\"confidence\":0.9,\"clarification_question\":null}"
                        }
                    }]
                })
                .to_string(),
            })
        }
    }

    #[test]
    fn parses_with_openai_adapter() {
        let result = parse_with_client(&FakeHttpClient, &preset(), "午餐 1 元", "1 餐饮 expense")
            .expect("parse");
        assert_eq!(result.card_data.expect("card").amount_cents, 100);
    }

    fn preset() -> ProviderPreset {
        ProviderPreset {
            id: 1,
            name: "OpenAI".to_string(),
            api_format: ApiFormat::OpenAiChatCompletions,
            base_url: "https://api.openai.com/v1".to_string(),
            api_key: "key".to_string(),
            model: "gpt-4o-mini".to_string(),
            thinking_level: ThinkingLevel::Off,
            supports_vision: false,
            is_default: true,
            created_at: 0,
            updated_at: 0,
        }
    }
}
