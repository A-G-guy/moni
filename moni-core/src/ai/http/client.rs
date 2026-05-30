use std::time::Duration;

use serde_json::Value;

use crate::ai::errors::{AiError, redact_sensitive_text};

/// AI HTTP 请求。
#[derive(Clone, Debug)]
pub struct HttpRequest {
    pub url: String,
    pub headers: Vec<(String, String)>,
    pub body: Value,
    pub timeout_seconds: u64,
    pub redaction_secret: String,
}

/// AI HTTP 响应。
#[derive(Clone, Debug)]
pub struct HttpResponse {
    pub status: u16,
    pub body: String,
}

/// 可替换的 HTTP 客户端，便于单元测试注入 fake 实现。
pub trait HttpClient: Send + Sync {
    fn post_json(&self, request: &HttpRequest) -> Result<HttpResponse, AiError>;
}

/// 基于 reqwest blocking 的默认 HTTP 客户端。
#[derive(Debug, Default)]
pub struct DefaultHttpClient;

impl HttpClient for DefaultHttpClient {
    fn post_json(&self, request: &HttpRequest) -> Result<HttpResponse, AiError> {
        let client = reqwest::blocking::Client::builder()
            .timeout(Duration::from_secs(request.timeout_seconds.max(1)))
            .build()
            .map_err(|error| AiError::Network(error.to_string()))?;
        let mut builder = client.post(&request.url).json(&request.body);
        for (name, value) in &request.headers {
            builder = builder.header(name, value);
        }
        let response = builder.send().map_err(|error| {
            AiError::Network(redact_sensitive_text(
                &error.to_string(),
                &request.redaction_secret,
            ))
        })?;
        let status = response.status().as_u16();
        let body = response
            .text()
            .map_err(|error| AiError::Network(error.to_string()))?;
        Ok(HttpResponse {
            status,
            body: redact_sensitive_text(&truncate_body(&body), &request.redaction_secret),
        })
    }
}

fn truncate_body(body: &str) -> String {
    const MAX_BODY_CHARS: usize = 8_000;
    if body.chars().count() <= MAX_BODY_CHARS {
        return body.to_string();
    }
    body.chars().take(MAX_BODY_CHARS).collect::<String>()
}
