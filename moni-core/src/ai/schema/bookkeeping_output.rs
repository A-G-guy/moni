use serde::Deserialize;

use crate::ai::domain::{AiBookkeepingParseResult, DraftCardDataOutput};
use crate::ai::errors::AiError;

/// 模型必须输出的记账解析结构。
#[derive(Clone, Debug, Deserialize)]
pub struct BookkeepingModelOutput {
    pub is_bookkeeping: bool,
    #[serde(default)]
    pub reply_text: String,
    pub amount_cents: Option<i64>,
    pub record_type: Option<String>,
    pub category_id: Option<i64>,
    #[serde(default)]
    pub account_id: Option<i64>,
    pub timestamp: Option<i64>,
    pub note: Option<String>,
    #[serde(default = "default_confidence")]
    pub confidence: f64,
    #[serde(default)]
    pub clarification_question: Option<String>,
}

/// 解析并校验模型返回 JSON。
pub fn parse_bookkeeping_output(json_text: &str) -> Result<AiBookkeepingParseResult, AiError> {
    let cleaned = strip_markdown_fence(json_text);
    let output: BookkeepingModelOutput = serde_json::from_str(cleaned).map_err(|error| {
        AiError::Parse(format!(
            "{error}; raw={}",
            cleaned.chars().take(200).collect::<String>()
        ))
    })?;
    validate_output(output)
}

fn validate_output(output: BookkeepingModelOutput) -> Result<AiBookkeepingParseResult, AiError> {
    let confidence = output.confidence.clamp(0.0, 1.0);
    if !output.is_bookkeeping {
        return Ok(AiBookkeepingParseResult {
            is_bookkeeping: false,
            reply_text: non_empty_reply(&output.reply_text, "我没有识别到明确的记账信息。"),
            card_data: None,
            confidence,
            clarification_question: output.clarification_question,
        });
    }

    let amount_cents = output
        .amount_cents
        .filter(|amount| *amount > 0)
        .ok_or_else(|| AiError::InvalidOutput("记账结果缺少有效金额".to_string()))?;
    let record_type = normalize_record_type(output.record_type.as_deref())?;
    let card_data = DraftCardDataOutput {
        amount_cents,
        record_type,
        category_id: output.category_id.unwrap_or(1),
        account_id: output.account_id.unwrap_or(-1),
        timestamp: output.timestamp.unwrap_or(0).max(0),
        note: output.note.unwrap_or_default(),
    };
    Ok(AiBookkeepingParseResult {
        is_bookkeeping: true,
        reply_text: non_empty_reply(&output.reply_text, "已为你整理成待确认的记账卡片。"),
        card_data: Some(card_data),
        confidence,
        clarification_question: output.clarification_question,
    })
}

fn normalize_record_type(raw: Option<&str>) -> Result<String, AiError> {
    match raw
        .unwrap_or("expense")
        .trim()
        .to_ascii_lowercase()
        .as_str()
    {
        "expense" | "支出" => Ok("expense".to_string()),
        "income" | "收入" => Ok("income".to_string()),
        other => Err(AiError::InvalidOutput(format!("未知收支类型: {other}"))),
    }
}

fn non_empty_reply(reply: &str, fallback: &str) -> String {
    let trimmed = reply.trim();
    if trimmed.is_empty() {
        fallback.to_string()
    } else {
        trimmed.to_string()
    }
}

fn strip_markdown_fence(text: &str) -> &str {
    let trimmed = text.trim();
    if !trimmed.starts_with("```") {
        return trimmed;
    }
    let without_start = trimmed
        .trim_start_matches("```")
        .trim_start_matches("json")
        .trim_start_matches("JSON")
        .trim();
    without_start.trim_end_matches("```").trim()
}

fn default_confidence() -> f64 {
    0.0
}

#[cfg(test)]
mod tests {
    use super::parse_bookkeeping_output;

    #[test]
    fn parses_valid_bookkeeping_json() {
        let result = parse_bookkeeping_output(
            r#"{"is_bookkeeping":true,"reply_text":"ok","amount_cents":3500,"record_type":"expense","category_id":1,"confidence":0.9}"#,
        )
        .expect("parse");
        assert!(result.is_bookkeeping);
        assert_eq!(result.card_data.expect("card").amount_cents, 3500);
    }

    #[test]
    fn rejects_invalid_amount() {
        let error = parse_bookkeeping_output(
            r#"{"is_bookkeeping":true,"amount_cents":0,"record_type":"expense"}"#,
        )
        .expect_err("invalid");
        assert!(error.to_string().contains("金额"));
    }

    #[test]
    fn parses_non_bookkeeping_json() {
        let result = parse_bookkeeping_output(r#"{"is_bookkeeping":false,"reply_text":"你好"}"#)
            .expect("parse");
        assert!(!result.is_bookkeeping);
        assert!(result.card_data.is_none());
    }
}
