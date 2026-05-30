use serde::{Deserialize, Serialize};
use serde_json::Value;

/// Provider-neutral function declaration，首版仅预留。
#[derive(Clone, Debug, Deserialize, Serialize)]
pub struct AiToolDeclaration {
    pub name: String,
    pub description: String,
    pub parameters: Value,
}

/// Provider-neutral function call，首版不自动执行。
#[derive(Clone, Debug, Deserialize, Serialize)]
pub struct AiToolCall {
    pub id: Option<String>,
    pub name: String,
    pub arguments: Value,
}

/// Provider-neutral function result，供后续扩展。
#[derive(Clone, Debug, Deserialize, Serialize)]
pub struct AiToolResult {
    pub id: Option<String>,
    pub name: String,
    pub output: Value,
}
