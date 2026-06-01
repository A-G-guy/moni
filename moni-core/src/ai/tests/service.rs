use serde_json::json;

use super::{parse_request_with_client, parse_with_client};
use crate::ai::domain::{
    AiBookkeepingImageInput, AiBookkeepingParseRequest, ApiFormat, ProviderPreset, ThinkingLevel,
};
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

#[test]
fn rejects_images_when_preset_does_not_support_vision() {
    let request = AiBookkeepingParseRequest {
        text: "发票".to_string(),
        images: vec![AiBookkeepingImageInput {
            mime_type: "image/jpeg".to_string(),
            base64_data: "abcd".to_string(),
            original_size_bytes: Some(3),
        }],
    };
    let error = parse_request_with_client(&FakeHttpClient, &preset(), request, "1 餐饮 expense")
        .expect_err("vision unsupported");
    assert!(matches!(error, AiError::VisionUnsupported));
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
