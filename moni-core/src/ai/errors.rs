use crate::core::error::CoreError;

/// AI 模块内部错误。
#[derive(Debug, thiserror::Error)]
pub enum AiError {
    #[error("未配置默认 AI provider")]
    NoDefaultPreset,
    #[error("AI provider 预设不存在: id={0}")]
    PresetNotFound(i64),
    #[error("AI provider 预设无效: {0}")]
    InvalidPreset(String),
    #[error("AI 记账输入无效: {0}")]
    InvalidInput(String),
    #[error("预设中 API key 为空")]
    MissingApiKey,
    #[error("网络错误: {0}")]
    Network(String),
    #[error("认证失败: HTTP {0}")]
    Auth(u16),
    #[error("请求被限流，请稍后重试")]
    RateLimited,
    #[error("服务端错误: HTTP {status_code}: {detail}")]
    Server { status_code: u16, detail: String },
    #[error("响应 JSON 解析失败: {0}")]
    Parse(String),
    #[error("模型返回不符合记账结构: {0}")]
    InvalidOutput(String),
    #[error("当前 AI 预设不支持图片识别")]
    VisionUnsupported,
    #[error("图片输入无效: {0}")]
    InvalidImageInput(String),
    #[error("模型拒绝处理: {0}")]
    Refusal(String),
}

impl From<AiError> for CoreError {
    fn from(error: AiError) -> Self {
        match error {
            AiError::NoDefaultPreset
            | AiError::MissingApiKey
            | AiError::InvalidInput(_)
            | AiError::VisionUnsupported
            | AiError::InvalidImageInput(_) => CoreError::InvalidInput(error.to_string()),
            other => CoreError::Internal(format!("AI 处理失败: {other}")),
        }
    }
}

/// 对可能包含敏感信息的文本进行日志脱敏。
pub fn redact_sensitive_text(text: &str, api_key: &str) -> String {
    let trimmed_key = api_key.trim();
    if trimmed_key.is_empty() {
        return text.to_string();
    }
    text.replace(trimmed_key, "[REDACTED_API_KEY]")
}
