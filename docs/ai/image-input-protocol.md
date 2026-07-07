---
last_modified: "2026-07-07 18:51"
---

# AI 记账图片输入协议

## 能力开关

图片入口由用户在 AI 预设中手动设置的 `supports_vision` 控制：

- `supports_vision=true`：AI 记账页显示图片入口，允许文字、图片、文字+图片单轮发送。
- `supports_vision=false`：AI 记账页不显示图片入口；Rust Core 也会拒绝带图片的请求。

## Kotlin 与 Rust 分工

Android Kotlin 负责：

- 调用系统图片选择器选择多张图片。
- 将图片压缩为 JPEG。
- 将压缩后的 bytes 编码为 base64。
- 把 `mime_type/base64_data/original_size_bytes` 传给 Rust Core。

Rust Core 负责核心逻辑：

- 校验图片数量、单图大小、总大小、mime 类型和 base64 字符合法性。
- 根据预设的 `api_format` 构造供应商请求体。
- 确保图片请求仍使用供应商 JSON Output / Structured Output。

## 请求 DTO

```json
{
  "text": "发票，备注按店名写",
  "images": [
    {
      "mime_type": "image/jpeg",
      "base64_data": "...",
      "original_size_bytes": 123456
    }
  ],
  "sent_at": 1780329600
}
```

## 供应商协议

### OpenAI-compatible Chat Completions

用户消息 `content` 使用多 part 数组：

```json
[
  { "type": "text", "text": "发票，备注按店名写" },
  {
    "type": "image_url",
    "image_url": {
      "url": "data:image/jpeg;base64,..."
    }
  }
]
```

同时保留 `response_format.type=json_schema`。

### Gemini generateContent

用户消息 `parts` 使用文本 part + 多个 `inline_data` part：

```json
[
  { "text": "发票，备注按店名写" },
  {
    "inline_data": {
      "mime_type": "image/jpeg",
      "data": "..."
    }
  }
]
```

同时保留 `generationConfig.responseMimeType=application/json` 与 `responseSchema`。

## 隐私与限制

图片可能包含发票、订单截图等隐私信息。发送图片意味着图片会传给用户配置的 AI Provider。实现上不得记录图片 base64 和 API Key 日志。
