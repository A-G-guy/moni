use serde_json::{Value, json};

use crate::ai::domain::ProviderPreset;
use crate::ai::errors::AiError;
use crate::ai::http::{HttpClient, HttpRequest};
use crate::ai::providers::{
    ProviderAdapter, base_prompt, handle_http_error, map_thinking_level, parse_json_output,
};
use crate::ai::schema::openai_response_format;

/// OpenAI-compatible Chat Completions adapter。
#[derive(Debug, Default)]
pub struct OpenAiChatAdapter;

impl ProviderAdapter for OpenAiChatAdapter {
    fn parse_bookkeeping(
        &self,
        http_client: &dyn HttpClient,
        preset: &ProviderPreset,
        input: &str,
        category_context: &str,
    ) -> Result<crate::ai::domain::AiBookkeepingParseResult, AiError> {
        let url = chat_completions_url(&preset.base_url);
        let body = build_request_body(preset, input, category_context, true);
        let response = http_client.post_json(&HttpRequest {
            url,
            headers: vec![
                (
                    "Authorization".to_string(),
                    format!("Bearer {}", preset.api_key),
                ),
                ("Content-Type".to_string(), "application/json".to_string()),
            ],
            body,
            timeout_seconds: 45,
            redaction_secret: preset.api_key.clone(),
        })?;
        if !(200..300).contains(&response.status) {
            return Err(handle_http_error(response.status, &response.body));
        }
        let content = extract_content(&response.body)?;
        parse_json_output(&content)
    }
}

fn build_request_body(
    preset: &ProviderPreset,
    input: &str,
    category_context: &str,
    structured: bool,
) -> Value {
    let mut body = json!({
        "model": preset.model,
        "messages": [
            { "role": "system", "content": base_prompt(category_context) },
            { "role": "user", "content": input }
        ],
        "temperature": 0.2
    });
    if structured {
        body["response_format"] = openai_response_format();
    } else {
        body["response_format"] = json!({ "type": "json_object" });
    }
    if let Some(reasoning) = map_thinking_level(&preset.thinking_level) {
        body["reasoning"] = reasoning;
    }
    body
}

fn extract_content(body: &str) -> Result<String, AiError> {
    let value: Value =
        serde_json::from_str(body).map_err(|error| AiError::Parse(error.to_string()))?;
    if let Some(refusal) = value
        .pointer("/choices/0/message/refusal")
        .and_then(Value::as_str)
        .filter(|text| !text.trim().is_empty())
    {
        return Err(AiError::Refusal(refusal.to_string()));
    }
    value
        .pointer("/choices/0/message/content")
        .and_then(Value::as_str)
        .map(ToString::to_string)
        .ok_or_else(|| AiError::Parse("OpenAI 响应缺少 choices[0].message.content".to_string()))
}

fn chat_completions_url(base_url: &str) -> String {
    let trimmed = base_url.trim().trim_end_matches('/');
    if trimmed.ends_with("/chat/completions") {
        return trimmed.to_string();
    }
    format!("{trimmed}/chat/completions")
}

#[cfg(test)]
mod tests {
    use std::sync::Mutex;

    use serde_json::json;

    use super::{OpenAiChatAdapter, chat_completions_url};
    use crate::ai::domain::{ApiFormat, ProviderPreset, ThinkingLevel};
    use crate::ai::errors::AiError;
    use crate::ai::http::{HttpClient, HttpRequest, HttpResponse};
    use crate::ai::providers::ProviderAdapter;

    #[derive(Debug)]
    struct FakeHttpClient {
        request_body: Mutex<Option<serde_json::Value>>,
    }

    impl HttpClient for FakeHttpClient {
        fn post_json(&self, request: &HttpRequest) -> Result<HttpResponse, AiError> {
            *self.request_body.lock().expect("lock") = Some(request.body.clone());
            Ok(HttpResponse {
                status: 200,
                body: json!({
                    "choices": [{
                        "message": {
                            "content": "{\"is_bookkeeping\":true,\"reply_text\":\"ok\",\"amount_cents\":1200,\"record_type\":\"expense\",\"category_id\":1,\"account_id\":null,\"timestamp\":null,\"note\":\"午餐\",\"confidence\":0.9,\"clarification_question\":null}"
                        }
                    }]
                })
                .to_string(),
            })
        }
    }

    #[test]
    fn appends_chat_completions_path() {
        assert_eq!(
            chat_completions_url("https://api.openai.com/v1"),
            "https://api.openai.com/v1/chat/completions"
        );
    }

    #[test]
    fn parses_success_response() {
        let http = FakeHttpClient {
            request_body: Mutex::new(None),
        };
        let preset = preset();
        let result = OpenAiChatAdapter
            .parse_bookkeeping(&http, &preset, "午餐 12 元", "1 餐饮 expense")
            .expect("parse");
        assert!(result.is_bookkeeping);
        assert_eq!(result.card_data.expect("card").amount_cents, 1200);
        let body = http
            .request_body
            .lock()
            .expect("lock")
            .clone()
            .expect("body");
        assert_eq!(body["response_format"]["type"], "json_schema");
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
