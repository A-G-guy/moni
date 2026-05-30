use serde::{Deserialize, Serialize};

/// AI 服务兼容的 API 格式。
#[derive(Clone, Debug, Deserialize, PartialEq, Eq, Serialize)]
#[serde(rename_all = "snake_case")]
pub enum ApiFormat {
    OpenAiChatCompletions,
    GeminiGenerateContent,
}

/// 模型思考程度的中立表示。
#[derive(Clone, Debug, Deserialize, PartialEq, Eq, Serialize)]
#[serde(rename_all = "snake_case")]
pub enum ThinkingLevel {
    Off,
    Low,
    Medium,
    High,
}

impl Default for ThinkingLevel {
    fn default() -> Self {
        Self::Off
    }
}

/// AI Provider 预设的完整内部表示。
#[derive(Clone, Debug, Deserialize, Serialize)]
pub struct ProviderPreset {
    pub id: i64,
    pub name: String,
    pub api_format: ApiFormat,
    pub base_url: String,
    pub api_key: String,
    pub model: String,
    #[serde(default)]
    pub thinking_level: ThinkingLevel,
    #[serde(default)]
    pub supports_vision: bool,
    #[serde(default)]
    pub is_default: bool,
    pub created_at: i64,
    pub updated_at: i64,
}

/// 保存 Provider 预设时来自前端的输入。
#[derive(Clone, Debug, Deserialize, Serialize)]
pub struct ProviderPresetSaveRequest {
    pub id: Option<i64>,
    pub name: String,
    pub api_format: ApiFormat,
    pub base_url: String,
    pub api_key: Option<String>,
    pub model: String,
    #[serde(default)]
    pub thinking_level: ThinkingLevel,
    #[serde(default)]
    pub supports_vision: bool,
    #[serde(default)]
    pub is_default: bool,
}

/// Provider 预设列表项，永远不返回明文 API key。
#[derive(Clone, Debug, Serialize)]
pub struct ProviderPresetListItem {
    pub id: i64,
    pub name: String,
    pub api_format: ApiFormat,
    pub base_url: String,
    pub masked_api_key: String,
    pub has_api_key: bool,
    pub model: String,
    pub thinking_level: ThinkingLevel,
    pub supports_vision: bool,
    pub is_default: bool,
    pub created_at: i64,
    pub updated_at: i64,
}

impl ProviderPreset {
    pub fn to_list_item(&self) -> ProviderPresetListItem {
        ProviderPresetListItem {
            id: self.id,
            name: self.name.clone(),
            api_format: self.api_format.clone(),
            base_url: self.base_url.clone(),
            masked_api_key: mask_api_key(&self.api_key),
            has_api_key: !self.api_key.trim().is_empty(),
            model: self.model.clone(),
            thinking_level: self.thinking_level.clone(),
            supports_vision: self.supports_vision,
            is_default: self.is_default,
            created_at: self.created_at,
            updated_at: self.updated_at,
        }
    }
}

/// Rust 返回给 Kotlin AI 记账页的解析结果。
#[derive(Clone, Debug, Serialize)]
pub struct AiBookkeepingParseResult {
    pub is_bookkeeping: bool,
    pub reply_text: String,
    pub card_data: Option<DraftCardDataOutput>,
    pub confidence: f64,
    pub clarification_question: Option<String>,
}

/// 与 Kotlin DraftCardData 保持一致的输出结构。
#[derive(Clone, Debug, Serialize)]
pub struct DraftCardDataOutput {
    pub amount_cents: i64,
    pub record_type: String,
    pub category_id: i64,
    pub account_id: i64,
    pub timestamp: i64,
    pub note: String,
}

fn mask_api_key(api_key: &str) -> String {
    let trimmed = api_key.trim();
    if trimmed.is_empty() {
        return String::new();
    }
    let chars: Vec<char> = trimmed.chars().collect();
    if chars.len() <= 8 {
        return "••••".to_string();
    }
    let prefix: String = chars.iter().take(4).collect();
    let suffix: String = chars
        .iter()
        .rev()
        .take(4)
        .collect::<Vec<_>>()
        .into_iter()
        .rev()
        .collect();
    format!("{prefix}••••{suffix}")
}

#[cfg(test)]
mod tests {
    use super::mask_api_key;

    #[test]
    fn masks_short_and_long_keys() {
        assert_eq!(mask_api_key(""), "");
        assert_eq!(mask_api_key("abc"), "••••");
        assert_eq!(mask_api_key("sk-1234567890"), "sk-1••••7890");
    }
}
