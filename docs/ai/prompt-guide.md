# AI 记账提示词与结构化输出策略

## 目标

AI 记账只做单轮解析：Rust Core 每次仅发送本次用户文字、可选图片和当前可用分类上下文，不拼接历史聊天记录，也不让模型发起追问式多轮对话。

## JSON Output 约束

结构化解析必须依赖供应商 API 的 JSON Output / Structured Output 功能，而不是只靠提示词约束：

- OpenAI-compatible Chat Completions：请求体必须包含 `response_format.type=json_schema`，并启用 `json_schema.strict=true`。
- Gemini generateContent：请求体必须包含 `generationConfig.responseMimeType=application/json` 与 `generationConfig.responseSchema`。

提示词只用于解释业务语义；解析入口始终是 API 返回的结构化 JSON，再由 Rust schema 反序列化与校验。

## 分类选择规则

Rust Core 会把 active 分类按层级拼接给模型，每个分类包含：

- `id`
- `type`：`expense` 或 `income`
- `name`
- `description`
- `parent_id` / `parent_name`

模型选择分类时遵循：

1. 能确定二级分类时，直接返回二级分类 id。
2. 不能确定二级分类，或该一级分类没有二级分类时，返回一级分类 id。
3. 完全无法判断分类时，返回 `category_id=-1`。
4. `category_id=-1` 只允许进入草稿卡片，不允许直接入账；用户必须手动选择分类。

## 备注提取规则

备注 `note` 应提取用户文字或图片票据中的具体可读信息，例如商户名、商品名、餐品名或消费场景。

示例：

- 输入：小微家盖饭的发票
- 期望备注：`小微家盖饭`

不要只写“餐饮”“外卖”这类泛化分类名。

## 单轮边界

模型不得依赖历史消息。若用户本轮输入信息不足，模型应尽量返回已知字段；无法确定的字段使用约定默认值，例如 `category_id=-1`、`timestamp=0`。
