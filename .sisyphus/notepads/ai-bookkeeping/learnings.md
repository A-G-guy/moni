# AI 记账 Chat Repository 实现记录

## 日期: 2026-05-20

## 完成事项

### Rust 层
- 创建了 `moni-core/src/db/chat_repo.rs`，包含：
  - `ChatMessageRow` 结构体（带 `Serialize` derive）
  - `insert`, `get_by_session`, `update_status`, `delete_by_id`, `clear_session` 五个 CRUD 函数
  - 使用参数化查询防止 SQL 注入
  - `get_by_session` 按 `created_at DESC` 排序
- 修改了 `moni-core/src/db/mod.rs`，暴露 `chat_repo` 模块
- 在 `moni-core/src/lib.rs` 的 `MoniCore` impl 中添加了五个 `async` 暴露方法：
  - `chat_insert` → 返回 `i64`
  - `chat_get_by_session` → 返回 JSON 字符串（`Vec<ChatMessageRow>` 序列化）
  - `chat_update_status` → 返回 `()`
  - `chat_delete` → 返回 `()`
  - `chat_clear_session` → 返回 `()`
  - 统一使用 `spawn_blocking` + `Arc<Mutex>` 模式

### Kotlin 层
- 创建了 `ChatRepository.kt` 接口，定义五个 suspend 方法
- 创建了 `ChatRepositoryImpl.kt` 实现：
  - 通过 `MoniCore` 直接调用 UniFFI 暴露的方法
  - 使用 `BridgeJson` + kotlinx.serialization 反序列化 Rust 返回的 JSON
  - 内部定义 `ChatMessageDto` 用于 JSON 映射
  - `DraftCardData` 未添加 `@Serializable`，使用手动 JSON 构造/解析避免修改模型层

## 关键决策

1. **chat_get_by_session 返回 JSON 字符串**：UniFFI 不直接支持复杂结构体列表，所以 Rust 层序列化为 JSON 字符串，Kotlin 层反序列化。
2. **手动处理 DraftCardData JSON**：为避免修改模型类，在 `ChatRepositoryImpl` 外定义了 `encodeDraftCardData` / `decodeDraftCardData` 两个私有函数手动处理 JSON。
3. **错误处理**：Rusqlite 错误统一转换为 `CoreError::Database(...)`。

## 验证结果
- `cargo check` ✅ 通过
- `./gradlew :app:compileDebugKotlin` ✅ 通过
- LSP 诊断：`chat_repo.rs` 和 `lib.rs` 无错误；`ChatRepositoryImpl.kt` 的 LSP 误报是因为 UniFFI 生成代码不在 LSP 源码路径中，Gradle 编译已通过。

## 注意事项
- `rusqlite::params![]` 宏用于混合类型参数绑定（如 `&str` + `i64`）。
- Kotlin 中 `jsonPrimitive.content` 获取字符串值（不是 `contentOrNull`）。
