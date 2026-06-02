use std::sync::Mutex;

use serde_json::json;

use super::{OpenAiChatAdapter, chat_completions_url};
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
                "choices": [{
                    "message": {
                        "content": "{\"is_bookkeeping\":true,\"reply_text\":\"ok\",\"amount_cents\":1200,\"record_type\":\"expense\",\"category_id\":1,\"timestamp\":null,\"note\":\"午餐\",\"confidence\":0.9,\"clarification_question\":null}"
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
    let preset = preset(false);
    let request = AiBookkeepingParseRequest::text_only("午餐 12 元");
    let result = OpenAiChatAdapter
        .parse_bookkeeping(&http, &preset, &request, "1 餐饮 expense")
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

#[test]
fn builds_multi_image_content_parts() {
    let http = FakeHttpClient {
        request_body: Mutex::new(None),
    };
    let request = AiBookkeepingParseRequest {
        text: "发票".to_string(),
        images: vec![
            AiBookkeepingImageInput {
                mime_type: "image/jpeg".to_string(),
                base64_data: "aaaa".to_string(),
                original_size_bytes: None,
            },
            AiBookkeepingImageInput {
                mime_type: "image/png".to_string(),
                base64_data: "bbbb".to_string(),
                original_size_bytes: None,
            },
        ],
        sent_at: None,
    };
    OpenAiChatAdapter
        .parse_bookkeeping(&http, &preset(true), &request, "1 餐饮 expense")
        .expect("parse");
    let body = http
        .request_body
        .lock()
        .expect("lock")
        .clone()
        .expect("body");
    let content = body["messages"][1]["content"].as_array().expect("parts");
    assert_eq!(content.len(), 3);
    assert_eq!(content[1]["type"], "image_url");
    assert_eq!(body["response_format"]["type"], "json_schema");
}

fn preset(supports_vision: bool) -> ProviderPreset {
    ProviderPreset {
        id: 1,
        name: "OpenAI".to_string(),
        api_format: ApiFormat::OpenAiChatCompletions,
        base_url: "https://api.openai.com/v1".to_string(),
        api_key: "key".to_string(),
        model: "gpt-4o-mini".to_string(),
        thinking_level: ThinkingLevel::Off,
        supports_vision,
        is_default: true,
        created_at: 0,
        updated_at: 0,
    }
}
