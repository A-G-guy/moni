package com.agguy.moni.core

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

/**
 * 用于反序列化 Rust 返回的 JSON（camelCase）。
 */
internal val BridgeJson = Json {
    ignoreUnknownKeys = true
    prettyPrint = false
    encodeDefaults = true
}

/**
 * 用于序列化 Kotlin Intent 为 JSON（snake_case，与 Rust 侧字段名一致）。
 * 编码方向不启用 ignoreUnknownKeys，确保 Kotlin 发送的字段 Rust 必须能识别。
 */
@OptIn(ExperimentalSerializationApi::class)
internal val BridgeJsonEncode = Json {
    prettyPrint = false
    encodeDefaults = true
    namingStrategy = JsonNamingStrategy.SnakeCase
}
