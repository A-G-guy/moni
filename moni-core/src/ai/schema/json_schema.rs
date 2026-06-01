use serde_json::{Value, json};

/// OpenAI-compatible Chat Completions 的 structured output 配置。
pub fn openai_response_format() -> Value {
    json!({
        "type": "json_schema",
        "json_schema": {
            "name": "moni_bookkeeping_extraction",
            "strict": true,
            "schema": bookkeeping_schema(true)
        }
    })
}

/// Gemini generateContent 的 responseSchema。
pub fn gemini_response_schema() -> Value {
    let mut schema = bookkeeping_schema(false);
    // Gemini responseSchema 的 `type` 是单值枚举，不接受 JSON Schema 的
    // `type: ["string", "null"]` 联合类型；用空字符串表达“无需澄清”。
    schema["properties"]["clarification_question"] = json!({
        "type": "string",
        "description": "需要澄清时的问题；无需澄清时为空字符串"
    });
    schema
}

fn bookkeeping_schema(openai_strict: bool) -> Value {
    let mut schema = json!({
        "type": "object",
        "properties": {
            "is_bookkeeping": {
                "type": "boolean",
                "description": "用户输入是否包含可记账的信息"
            },
            "reply_text": {
                "type": "string",
                "description": "给用户看的简短中文回复"
            },
            "amount_cents": {
                "type": "integer",
                "description": "金额，单位为分；非记账时为 0"
            },
            "record_type": {
                "type": "string",
                "enum": ["expense", "income"],
                "description": "支出 expense 或收入 income；非记账时默认 expense"
            },
            "category_id": {
                "type": "integer",
                "description": "分类 ID；不确定时为 -1"
            },
            "account_id": {
                "type": "integer",
                "description": "账户 ID；不确定时为 -1"
            },
            "timestamp": {
                "type": "integer",
                "description": "记账时间戳，秒；不确定时为 0"
            },
            "note": {
                "type": "string",
                "description": "备注；非记账时为空字符串"
            },
            "confidence": {
                "type": "number",
                "description": "0 到 1 的置信度"
            },
            "clarification_question": {
                "type": ["string", "null"],
                "description": "需要澄清时的问题，否则为 null"
            }
        },
        "required": [
            "is_bookkeeping",
            "reply_text",
            "amount_cents",
            "record_type",
            "category_id",
            "account_id",
            "timestamp",
            "note",
            "confidence",
            "clarification_question"
        ]
    });
    if openai_strict {
        schema["additionalProperties"] = json!(false);
    }
    schema
}

#[cfg(test)]
#[path = "tests/json_schema.rs"]
mod tests;
