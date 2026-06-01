use serde_json::{Value, json};

use crate::ai::domain::{AiBookkeepingImageInput, AiBookkeepingParseRequest, ProviderPreset};
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
        request: &AiBookkeepingParseRequest,
        category_context: &str,
    ) -> Result<crate::ai::domain::AiBookkeepingParseResult, AiError> {
        let url = chat_completions_url(&preset.base_url);
        let body = build_request_body(preset, request, category_context, true);
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
            timeout_seconds: 60,
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
    request: &AiBookkeepingParseRequest,
    category_context: &str,
    structured: bool,
) -> Value {
    let mut body = json!({
        "model": preset.model,
        "messages": [
            { "role": "system", "content": base_prompt(category_context) },
            { "role": "user", "content": build_user_content(request) }
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

fn build_user_content(request: &AiBookkeepingParseRequest) -> Value {
    if request.images.is_empty() {
        return json!(request.normalized_text());
    }

    let mut parts = vec![json!({
        "type": "text",
        "text": request.normalized_text()
    })];
    parts.extend(request.images.iter().map(openai_image_part));
    json!(parts)
}

fn openai_image_part(image: &AiBookkeepingImageInput) -> Value {
    json!({
        "type": "image_url",
        "image_url": {
            "url": format!("data:{};base64,{}", image.mime_type.trim(), image.base64_data)
        }
    })
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

pub(crate) fn chat_completions_url(base_url: &str) -> String {
    let trimmed = base_url.trim().trim_end_matches('/');
    if trimmed.ends_with("/chat/completions") {
        return trimmed.to_string();
    }
    format!("{trimmed}/chat/completions")
}

#[cfg(test)]
#[path = "tests/openai_chat.rs"]
mod tests;
