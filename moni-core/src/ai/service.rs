use std::collections::BTreeMap;

use rusqlite::Connection;

use crate::ai::domain::{
    AiBookkeepingImageInput, AiBookkeepingParseRequest, AiBookkeepingParseResult, ApiFormat,
};
use crate::ai::errors::AiError;
use crate::ai::http::{DefaultHttpClient, HttpClient};
use crate::ai::preset_repo;
use crate::ai::providers::ProviderAdapter;
use crate::ai::providers::gemini::GeminiAdapter;
use crate::ai::providers::openai_chat::OpenAiChatAdapter;
use crate::db::category_repo;

const MAX_IMAGE_COUNT: usize = 8;
const MAX_SINGLE_IMAGE_BASE64_BYTES: usize = 5 * 1024 * 1024;
const MAX_TOTAL_IMAGE_BASE64_BYTES: usize = 20 * 1024 * 1024;
const SUPPORTED_IMAGE_MIME_TYPES: &[&str] = &["image/jpeg", "image/png", "image/webp"];

/// AI 记账服务编排层。
#[derive(Debug, Default)]
pub struct AiBookkeepingService;

impl AiBookkeepingService {
    pub fn parse_with_default(
        conn: &Connection,
        input: &str,
    ) -> Result<AiBookkeepingParseResult, AiError> {
        let request = AiBookkeepingParseRequest::text_only(input);
        Self::parse_request_with_default(conn, request)
    }

    pub fn parse_request_with_default(
        conn: &Connection,
        request: AiBookkeepingParseRequest,
    ) -> Result<AiBookkeepingParseResult, AiError> {
        let preset = preset_repo::get_default(conn)?.ok_or(AiError::NoDefaultPreset)?;
        if preset.api_key.trim().is_empty() {
            return Err(AiError::MissingApiKey);
        }
        let category_context = build_category_context(conn)?;
        let http_client = DefaultHttpClient;
        parse_request_with_client(&http_client, &preset, request, &category_context)
    }

    pub fn test_connection(conn: &Connection, id: i64) -> Result<serde_json::Value, AiError> {
        let preset = preset_repo::get(conn, id)?.ok_or(AiError::PresetNotFound(id))?;
        if preset.api_key.trim().is_empty() {
            return Err(AiError::MissingApiKey);
        }
        let http_client = DefaultHttpClient;
        let result = parse_request_with_client(
            &http_client,
            &preset,
            AiBookkeepingParseRequest::text_only("测试：午餐花了 1 元"),
            "- 一级分类 id=1 type=expense name=餐饮 description= parent_id=null",
        )?;
        Ok(serde_json::json!({
            "ok": true,
            "is_bookkeeping": result.is_bookkeeping,
            "reply_text": result.reply_text
        }))
    }
}

pub fn parse_with_client(
    http_client: &dyn HttpClient,
    preset: &crate::ai::domain::ProviderPreset,
    input: &str,
    category_context: &str,
) -> Result<AiBookkeepingParseResult, AiError> {
    parse_request_with_client(
        http_client,
        preset,
        AiBookkeepingParseRequest::text_only(input),
        category_context,
    )
}

pub fn parse_request_with_client(
    http_client: &dyn HttpClient,
    preset: &crate::ai::domain::ProviderPreset,
    request: AiBookkeepingParseRequest,
    category_context: &str,
) -> Result<AiBookkeepingParseResult, AiError> {
    validate_request(preset, &request)?;
    match preset.api_format {
        ApiFormat::OpenAiChatCompletions => {
            OpenAiChatAdapter.parse_bookkeeping(http_client, preset, &request, category_context)
        }
        ApiFormat::GeminiGenerateContent => {
            GeminiAdapter.parse_bookkeeping(http_client, preset, &request, category_context)
        }
    }
}

pub(crate) fn build_category_context(conn: &Connection) -> Result<String, AiError> {
    let categories = category_repo::list_active(conn)
        .map_err(|error| AiError::InvalidPreset(format!("读取分类失败: {error}")))?;
    if categories.is_empty() {
        return Ok("暂无可用分类；无法确定时 category_id=-1，用户会在卡片中手动选择。".to_string());
    }

    let children_by_parent = group_children_by_parent(&categories);
    let mut lines = Vec::new();
    for category in categories
        .iter()
        .filter(|category| category.parent_id.is_none())
    {
        lines.push(format_category_line("一级分类", category, None));
        if let Some(children) = children_by_parent.get(&category.id) {
            for child in children {
                lines.push(format_category_line(
                    "  二级分类",
                    child,
                    Some(&category.name),
                ));
            }
        }
    }
    Ok(lines.join("\n"))
}

fn group_children_by_parent(
    categories: &[moni_contracts::category::Category],
) -> BTreeMap<i64, Vec<&moni_contracts::category::Category>> {
    let mut grouped: BTreeMap<i64, Vec<&moni_contracts::category::Category>> = BTreeMap::new();
    for category in categories
        .iter()
        .filter(|category| category.parent_id.is_some())
    {
        if let Some(parent_id) = category.parent_id {
            grouped.entry(parent_id).or_default().push(category);
        }
    }
    grouped
}

fn format_category_line(
    label: &str,
    category: &moni_contracts::category::Category,
    parent_name: Option<&str>,
) -> String {
    let record_type = match category.category_type {
        moni_contracts::record::RecordType::Income => "income",
        moni_contracts::record::RecordType::Expense => "expense",
    };
    let parent_id = category
        .parent_id
        .map_or_else(|| "null".to_string(), |id| id.to_string());
    let parent = parent_name.map_or(String::new(), |name| format!(" parent_name={name}"));
    format!(
        "- {label} id={} type={} name={} description={} parent_id={}{}",
        category.id,
        record_type,
        category.name,
        category.description.as_deref().unwrap_or(""),
        parent_id,
        parent,
    )
}

fn validate_request(
    preset: &crate::ai::domain::ProviderPreset,
    request: &AiBookkeepingParseRequest,
) -> Result<(), AiError> {
    if request.text.trim().is_empty() && request.images.is_empty() {
        return Err(AiError::InvalidInput("请输入文字或选择图片".to_string()));
    }
    if !request.images.is_empty() && !preset.supports_vision {
        return Err(AiError::VisionUnsupported);
    }
    validate_images(&request.images)
}

fn validate_images(images: &[AiBookkeepingImageInput]) -> Result<(), AiError> {
    if images.len() > MAX_IMAGE_COUNT {
        return Err(AiError::InvalidImageInput(format!(
            "最多支持 {MAX_IMAGE_COUNT} 张图片"
        )));
    }
    let mut total_size = 0usize;
    for image in images {
        validate_image(image)?;
        total_size += image.base64_data.len();
    }
    if total_size > MAX_TOTAL_IMAGE_BASE64_BYTES {
        return Err(AiError::InvalidImageInput("图片总体积过大".to_string()));
    }
    Ok(())
}

fn validate_image(image: &AiBookkeepingImageInput) -> Result<(), AiError> {
    let mime_type = image.mime_type.trim().to_ascii_lowercase();
    if !SUPPORTED_IMAGE_MIME_TYPES.contains(&mime_type.as_str()) {
        return Err(AiError::InvalidImageInput(format!(
            "不支持的图片类型: {mime_type}"
        )));
    }
    if image.base64_data.is_empty() || image.base64_data.len() > MAX_SINGLE_IMAGE_BASE64_BYTES {
        return Err(AiError::InvalidImageInput("单张图片为空或过大".to_string()));
    }
    if !image.base64_data.chars().all(is_base64_char) {
        return Err(AiError::InvalidImageInput(
            "图片 base64 内容非法".to_string(),
        ));
    }
    Ok(())
}

fn is_base64_char(ch: char) -> bool {
    ch.is_ascii_alphanumeric() || matches!(ch, '+' | '/' | '=' | '-' | '_')
}

#[cfg(test)]
#[path = "tests/service.rs"]
mod tests;
