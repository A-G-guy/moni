use serde_json::{Value, json};

use crate::ai::domain::ProviderPreset;
use crate::ai::errors::AiError;
use crate::ai::http::{HttpClient, HttpRequest};
use crate::ai::providers::{
    ProviderAdapter, base_prompt, handle_http_error, map_thinking_level, parse_json_output,
};
use crate::ai::schema::gemini_response_schema;

/// Gemini generateContent adapter。
#[derive(Debug, Default)]
pub struct GeminiAdapter;

impl ProviderAdapter for GeminiAdapter {
    fn parse_bookkeeping(
        &self,
        http_client: &dyn HttpClient,
        preset: &ProviderPreset,
        input: &str,
        category_context: &str,
    ) -> Result<crate::ai::domain::AiBookkeepingParseResult, AiError> {
        let url = generate_content_url(&preset.base_url, &preset.model);
        let body = build_request_body(preset, input, category_context);
        let response = http_client.post_json(&HttpRequest {
            url,
            headers: vec![
                ("x-goog-api-key".to_string(), preset.api_key.clone()),
                ("Content-Type".to_string(), "application/json".to_string()),
            ],
            body,
            timeout_seconds: 45,
            redaction_secret: preset.api_key.clone(),
        })?;
        if !(200..300).contains(&response.status) {
            return Err(handle_http_error(response.status, &response.body));
        }
        let content = extract_text(&response.body)?;
        parse_json_output(&content)
    }
}

fn build_request_body(preset: &ProviderPreset, input: &str, category_context: &str) -> Value {
    let mut generation_config = json!({
        "temperature": 0.2,
        "responseMimeType": "application/json",
        "responseSchema": gemini_response_schema()
    });
    if let Some(thinking) = map_thinking_level(&preset.thinking_level) {
        generation_config["thinkingConfig"] = thinking;
    }
    json!({
        "systemInstruction": {
            "role": "user",
            "parts": [{ "text": base_prompt(category_context) }]
        },
        "contents": [{
            "role": "user",
            "parts": [{ "text": input }]
        }],
        "generationConfig": generation_config
    })
}

fn extract_text(body: &str) -> Result<String, AiError> {
    let value: Value =
        serde_json::from_str(body).map_err(|error| AiError::Parse(error.to_string()))?;
    value
        .pointer("/candidates/0/content/parts")
        .and_then(Value::as_array)
        .and_then(|parts| {
            parts
                .iter()
                .filter_map(|part| part.get("text").and_then(Value::as_str))
                .find(|text| !text.trim().is_empty())
        })
        .map(ToString::to_string)
        .ok_or_else(|| {
            AiError::Parse("Gemini 响应缺少 candidates[0].content.parts[].text".to_string())
        })
}

fn generate_content_url(base_url: &str, model: &str) -> String {
    let trimmed = base_url.trim().trim_end_matches('/');
    if trimmed.ends_with(":generateContent") {
        return trimmed.to_string();
    }
    let model_path = if model.starts_with("models/") {
        model.to_string()
    } else {
        format!("models/{model}")
    };
    format!("{trimmed}/{model_path}:generateContent")
}

#[cfg(test)]
mod tests {
    use serde_json::json;

    use super::{GeminiAdapter, generate_content_url};
    use crate::ai::domain::{ApiFormat, ProviderPreset, ThinkingLevel};
    use crate::ai::errors::AiError;
    use crate::ai::http::{HttpClient, HttpRequest, HttpResponse};
    use crate::ai::providers::ProviderAdapter;

    #[derive(Debug)]
    struct FakeHttpClient;

    impl HttpClient for FakeHttpClient {
        fn post_json(&self, _request: &HttpRequest) -> Result<HttpResponse, AiError> {
            Ok(HttpResponse {
                status: 200,
                body: json!({
                    "candidates": [{
                        "content": {
                            "parts": [{
                                "text": "{\"is_bookkeeping\":true,\"reply_text\":\"ok\",\"amount_cents\":3500,\"record_type\":\"expense\",\"category_id\":1,\"account_id\":null,\"timestamp\":null,\"note\":\"午餐\",\"confidence\":0.9,\"clarification_question\":null}"
                            }]
                        }
                    }]
                })
                .to_string(),
            })
        }
    }

    #[test]
    fn builds_generate_content_url() {
        assert_eq!(
            generate_content_url(
                "https://generativelanguage.googleapis.com/v1beta",
                "gemini-2.5-flash"
            ),
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
        );
    }

    #[test]
    fn parses_success_response() {
        let result = GeminiAdapter
            .parse_bookkeeping(&FakeHttpClient, &preset(), "午餐 35", "1 餐饮 expense")
            .expect("parse");
        assert!(result.is_bookkeeping);
        assert_eq!(result.card_data.expect("card").amount_cents, 3500);
    }

    fn preset() -> ProviderPreset {
        ProviderPreset {
            id: 1,
            name: "Gemini".to_string(),
            api_format: ApiFormat::GeminiGenerateContent,
            base_url: "https://generativelanguage.googleapis.com/v1beta".to_string(),
            api_key: "key".to_string(),
            model: "gemini-2.5-flash".to_string(),
            thinking_level: ThinkingLevel::Off,
            supports_vision: false,
            is_default: true,
            created_at: 0,
            updated_at: 0,
        }
    }
}
