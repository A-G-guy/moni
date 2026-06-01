use serde_json::{Value, json};

use crate::ai::domain::{AiBookkeepingImageInput, AiBookkeepingParseRequest, ProviderPreset};
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
        request: &AiBookkeepingParseRequest,
        category_context: &str,
    ) -> Result<crate::ai::domain::AiBookkeepingParseResult, AiError> {
        let url = generate_content_url(&preset.base_url, &preset.model);
        let body = build_request_body(preset, request, category_context);
        let response = http_client.post_json(&HttpRequest {
            url,
            headers: vec![
                ("x-goog-api-key".to_string(), preset.api_key.clone()),
                ("Content-Type".to_string(), "application/json".to_string()),
            ],
            body,
            timeout_seconds: 60,
            redaction_secret: preset.api_key.clone(),
        })?;
        if !(200..300).contains(&response.status) {
            return Err(handle_http_error(response.status, &response.body));
        }
        let content = extract_text(&response.body)?;
        parse_json_output(&content)
    }
}

fn build_request_body(
    preset: &ProviderPreset,
    request: &AiBookkeepingParseRequest,
    category_context: &str,
) -> Value {
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
            "parts": build_user_parts(request)
        }],
        "generationConfig": generation_config
    })
}

fn build_user_parts(request: &AiBookkeepingParseRequest) -> Value {
    let mut parts = vec![json!({ "text": request.normalized_text() })];
    parts.extend(request.images.iter().map(gemini_image_part));
    json!(parts)
}

fn gemini_image_part(image: &AiBookkeepingImageInput) -> Value {
    json!({
        "inline_data": {
            "mime_type": image.mime_type.trim(),
            "data": image.base64_data
        }
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

pub(crate) fn generate_content_url(base_url: &str, model: &str) -> String {
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
#[path = "tests/gemini.rs"]
mod tests;
