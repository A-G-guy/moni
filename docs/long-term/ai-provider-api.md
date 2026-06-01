# AI Provider API 与记账 JSON Output 契约

## 目标

Moni 的 AI 记账请求与响应处理统一放在 Rust Core 中实现，Kotlin 仅负责设置页、状态展示与 UniFFI 调用。这样可以保证 API 格式差异、密钥脱敏、JSON 输出校验和错误分类都集中在一个可复用模块中。

## Provider 预设

用户可在 AI 设置页保存多个预设。每个预设包含：

- API 格式：`open_ai_chat_completions` 或 `gemini_generate_content`
- Base URL：用户可自定义，支持官方与兼容服务
- API Key：保存与请求时使用；列表只返回脱敏 key
- 模型名
- 思考程度：`off/low/medium/high`，由各 adapter 做 best-effort 映射
- 是否支持读图：由用户手动设置；AI 记账页据此显示图片入口，Rust Core 也会据此决定是否允许带图片请求

## API 格式

### OpenAI-compatible Chat Completions

目标路径为 `{base_url}/chat/completions`。请求核心字段：

```json
{
  "model": "gpt-4o-mini",
  "messages": [
    { "role": "system", "content": "记账提取提示词" },
    { "role": "user", "content": "午餐花了 35 元" }
  ],
  "response_format": {
    "type": "json_schema",
    "json_schema": {
      "name": "moni_bookkeeping_extraction",
      "strict": true,
      "schema": {}
    }
  }
}
```

响应从 `choices[0].message.content` 读取结构化 JSON。首版不使用 legacy `/v1/completions`，也不实现 OpenAI Responses API。

带图片时，用户消息 `content` 改为多 part 数组，包含文本 part 与一个或多个 `image_url` data URI part；同时必须继续携带 `response_format.type=json_schema`，不能退化为纯文本解析。

### Gemini generateContent

目标路径为 `{base_url}/models/{model}:generateContent`。请求核心字段：

```json
{
  "systemInstruction": {
    "role": "user",
    "parts": [{ "text": "记账提取提示词" }]
  },
  "contents": [
    { "role": "user", "parts": [{ "text": "午餐花了 35 元" }] }
  ],
  "generationConfig": {
    "responseMimeType": "application/json",
    "responseSchema": {}
  }
}
```

响应从 `candidates[0].content.parts[].text` 读取结构化 JSON。

带图片时，用户消息 `parts` 包含文本 part 与一个或多个 `inline_data { mime_type, data }` part；同时必须继续携带 `generationConfig.responseMimeType=application/json` 与 `responseSchema`，不能退化为纯文本解析。

## 记账输出结构

模型输出必须通过 Rust 校验后才会转换为草稿卡片：

```json
{
  "is_bookkeeping": true,
  "reply_text": "已整理成待确认的记账卡片。",
  "amount_cents": 3500,
  "record_type": "expense",
  "category_id": 1,
  "account_id": -1,
  "timestamp": 0,
  "note": "午餐",
  "confidence": 0.9,
  "clarification_question": null
}
```

校验规则：

- `amount_cents` 必须大于 0 才能生成卡片。
- `record_type` 仅允许 `expense` 或 `income`。
- 不确定的 `category_id/account_id/timestamp` 分别使用 `-1/-1/0`。
- `category_id=-1` 只能进入草稿态，用户必须手动选择分类后才能入账。
- 分类选择遵循二级优先：能确定二级分类时返回二级分类 id；无法确定二级或该一级无二级时返回一级分类 id；完全未知时返回 `-1`。
- 备注 `note` 优先提取文字或图片票据中的具体商户、商品、餐品或场景，例如“小微家盖饭”。
- 非记账内容必须返回 `is_bookkeeping=false`，不得生成卡片。
- 模型返回非法 JSON、缺字段或类型错误时，Rust 返回可理解错误，Kotlin 不生成草稿卡片。

## Function Calling

首版仅保留中立 DTO：`AiToolDeclaration`、`AiToolCall`、`AiToolResult`。AI 记账不会自动执行工具调用或直接落账，仍必须由用户确认草稿卡片。

## 安全与隐私

- API Key 不写入日志，不在预设列表中明文返回。
- HTTP 错误 body 会截断并进行密钥脱敏。
- 未配置默认预设时，AI 记账页提示用户去 AI 设置页配置。
- 首版不支持 streaming。
- 图片可能包含发票、订单截图等隐私信息；图片 base64 不得写入日志。

## 关联文档

- [AI 记账提示词与结构化输出策略](../ai/prompt-guide.md)
- [AI 记账图片输入协议](../ai/image-input-protocol.md)
- [AI 记账分类回退策略](../ai/category-fallback-strategy.md)
