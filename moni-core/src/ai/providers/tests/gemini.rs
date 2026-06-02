use std::sync::Mutex;

use serde_json::json;

use super::{GeminiAdapter, generate_content_url};
use crate::ai::domain::{
    AiBookkeepingImageInput, AiBookkeepingParseRequest, ApiFormat, ProviderPreset, ThinkingLevel,
};
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
                "candidates": [{
                    "content": {
                        "parts": [{
                            "text": "{\"is_bookkeeping\":true,\"reply_text\":\"ok\",\"amount_cents\":3500,\"record_type\":\"expense\",\"category_id\":1,\"timestamp\":null,\"note\":\"午餐\",\"confidence\":0.9,\"clarification_question\":null}"
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
    let request = AiBookkeepingParseRequest::text_only("午餐 35");
    let result = GeminiAdapter
        .parse_bookkeeping(
            &FakeHttpClient::default(),
            &preset(false),
            &request,
            "1 餐饮 expense",
        )
        .expect("parse");
    assert!(result.is_bookkeeping);
    assert_eq!(result.card_data.expect("card").amount_cents, 3500);
}

#[test]
fn builds_multi_image_parts() {
    let http = FakeHttpClient::default();
    let request = AiBookkeepingParseRequest {
        text: "发票".to_string(),
        images: vec![
            AiBookkeepingImageInput {
                mime_type: "image/jpeg".to_string(),
                base64_data: "aaaa".to_string(),
                original_size_bytes: None,
            },
            AiBookkeepingImageInput {
                mime_type: "image/webp".to_string(),
                base64_data: "bbbb".to_string(),
                original_size_bytes: None,
            },
        ],
        sent_at: None,
    };
    GeminiAdapter
        .parse_bookkeeping(&http, &preset(true), &request, "1 餐饮 expense")
        .expect("parse");
    let body = http
        .request_body
        .lock()
        .expect("lock")
        .clone()
        .expect("body");
    let parts = body["contents"][0]["parts"].as_array().expect("parts");
    assert_eq!(parts.len(), 3);
    assert_eq!(parts[1]["inline_data"]["mime_type"], "image/jpeg");
    assert_eq!(
        body["generationConfig"]["responseMimeType"],
        "application/json"
    );
}

impl Default for FakeHttpClient {
    fn default() -> Self {
        Self {
            request_body: Mutex::new(None),
        }
    }
}

fn preset(supports_vision: bool) -> ProviderPreset {
    ProviderPreset {
        id: 1,
        name: "Gemini".to_string(),
        api_format: ApiFormat::GeminiGenerateContent,
        base_url: "https://generativelanguage.googleapis.com/v1beta".to_string(),
        api_key: "key".to_string(),
        model: "gemini-2.5-flash".to_string(),
        thinking_level: ThinkingLevel::Off,
        supports_vision,
        is_default: true,
        created_at: 0,
        updated_at: 0,
    }
}
