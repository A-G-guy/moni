---
last_modified: "2026-07-07 18:51"
---

# Moni 整体架构

## 1. 架构分层

```
+------------------+
|  Kotlin UI 层    |  Compose 界面 + ViewModel（无状态）
+------------------+
         |  StateFlow / Effect 回调
+------------------+
|   JNI / Bridge   |  UniFFI + JNA 自动生成绑定
+------------------+
         |  JSON 字符串（Intent / State / Effects）
+------------------+
|   Rust Core 层   |  状态机 + 业务逻辑 + SQLite
+------------------+
```

## 2. 各层职责

| 层级 | 职责 |
|------|------|
| Kotlin UI | 纯展示层：接收 State 渲染界面，将用户操作封装为 Intent 下发。 |
| JNI/Bridge | 通信层：UniFFI 生成 FFI 绑定，JNA 负责动态库加载，JSON 做序列化媒介。 |
| Rust Core | 内核层：集中式状态机、Intent 分发、数据库读写、副作用生成。 |

## 3. 数据流向

```
用户操作
   ↓
Kotlin 封装 CoreIntent → JSON
   ↓
Rust dispatch(intent_json) → 匹配 Intent → 执行业务 → 更新 AppState
   ↓
Rust 返回 CoreUpdate { state_json, effects }
   ↓
Kotlin 反序列化为 CoreMutation
   ↓
State 注入 StateFlow → Compose 自动刷新
Effects 交给 CoreEffectRunner 执行（如 Snackbar、导航）
```

## 4. 为什么 Rust 做内核？

- **跨平台潜力**：Rust 不依赖 Android 运行时，未来可复用到 iOS / Desktop。
- **状态机集中**：单一 `AppCoreRuntime` 持有全部状态，避免多端逻辑漂移。
- **性能与可靠性**：SQLite + 零成本抽象，编译期保证内存安全，减少运行时崩溃。

## 5. 为什么 Kotlin 只做无状态 UI？

- **减少平台锁定**：业务逻辑在 Rust 中，Kotlin 仅负责平台特有能力（通知、文件、主题）。
- **可测试性**：ViewModel 只转发 Intent 和订阅 State，无需覆盖复杂业务分支。
- **Compose 最佳实践**：UI 完全由不可变 State 驱动，无副作用泄漏。

## 6. FFI 通信方式

- **UniFFI + JNA**：Rust 侧用 `#[uniffi::export]` 标注，`uniffi-bindgen` 生成 Kotlin 绑定。
- **JSON 序列化**：Intent、State、Effect payload 均用 JSON 传递。
- **不用 Protobuf**：项目早期追求简单、人类可读、调试方便；JSON 在移动场景下性能足够。

## 7. 关键设计权衡：内存数据库 vs 文件数据库

| 模式 | 触发条件 | 用途 |
|------|----------|------|
| `:memory:` | `MoniCore::new()` / 文件初始化失败回退 | 测试、首次安装无权限时兜底 |
| 文件数据库 | `initialize_with_db(db_path)` | 正式运行，数据持久化 |

Rust 侧 `AppCoreRuntime` 同时持有 `AppState`（内存快照）和 `rusqlite::Connection`（数据库连接）。初始化时优先尝试打开文件数据库；失败则降级到内存模式，确保应用不因数据库异常而崩溃。
