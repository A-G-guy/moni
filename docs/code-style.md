# Moni 代码规范与拆分指引

## 单文件 500 行上限（硬性门槛）

任何提交到仓库的源文件（`.rs`、`.kt`、`.gradle.kts`）不得超过 **500 行**。超过即视为不合格提交，必须在提交前拆分。

**预提交检查机制**：
- `.githooks/pre-commit` 中配置行数扫描脚本，自动拦截超出行数限制的文件
- 提交前运行：确保 hooks 路径正确（`git config core.hooksPath .githooks`）

## 拆分指引（按语言/层）

### Rust 侧拆分策略

| 预警信号 | 拆分方向 |
|---------|---------|
| `lib.rs` 超过 200 行 | 将 `MoniCore` 实现移入 `core/api.rs`，`lib.rs` 仅保留模块声明和 `uniffi::setup_scaffolding!()` |
| `core/dispatch.rs` 超过 300 行 | 按意图领域拆分：`intent_host.rs`、`intent_workbench.rs`、`intent_settings.rs` 等 |
| `models/state.rs` 超过 200 行 | 按子域拆分：`state_app.rs`（根状态）、`state_thread.rs`、`state_host.rs` 等 |
| `effects.rs` 超过 150 行 | 按 effect 类型拆分：`effects_bridge.rs`、`effects_persist.rs`、`effects_timer.rs` 等 |
| 单个模块文件超过 300 行 | 提取纯函数到 `helpers.rs` 或按职责拆分子模块 |

**通用 Rust 拆分原则**：
- 一个 `.rs` 文件只负责一个职责（单一职责）
- 优先按业务领域拆分（`host/`、`workbench/`、`settings/` 子目录）
- 工具函数超过 3 处复用即提取到 `shared.rs` 或 `helpers.rs`
- 类型定义超过 5 个即提取到独立 `types.rs` 或子目录 `models/{domain}/`

### Kotlin 侧拆分策略

| 预警信号 | 拆分方向 |
|---------|---------|
| `AppViewModel.kt` 超过 200 行 | 提取领域专用 ViewModel：`HostViewModel`、`WorkbenchViewModel` 等 |
| 单个 Screen Composable 超过 200 行 | 拆分为：Screen（布局骨架）+ 子 Composable（输入区、列表区、详情区） |
| `CoreEffectRunner.kt` 超过 300 行 | 按 effect 类型拆分为：`EffectRunnerBridge.kt`、`EffectRunnerPersist.kt` 等 |
| `RustCoreController.kt` 超过 200 行 | 提取 JSON 编解码器为独立 `BridgeJson.kt`，提取序列化器注册到独立文件 |
| 单个 Composable 超过 100 行 | 拆分为更小的纯展示 Composable + 状态持有 Composable |

**通用 Kotlin 拆分原则**：
- Composable 函数尽量控制在 50 行以内，复杂 UI 拆分为嵌套 Composable
- ViewModel 只保留状态管理和派发逻辑，业务转换逻辑提取到 `usecase/` 或 `transform/` 包
- `data/` 包按数据源拆分：`local/`（DataStore、数据库）、`remote/`（HTTP/WebSocket）、`repository/`（聚合层）
- 禁止在 `core/` 包中直接写 Android 平台代码（如 `Context` 操作），平台相关逻辑下沉到 `platform/` 包

## 预测性拆分（提前行动）

**禁止被动等待拦截**：当文件行数达到预警阈值（Rust 300 行 / Kotlin 250 行 / Composable 80 行）时，主动规划拆分，不要等到 500 行硬上限才行动。
- 新增功能前评估：该功能应该放在现有文件还是新文件？
- 修改文件前查看当前行数，若已接近阈值，优先拆分再添加新代码
- 禁止"先凑合写进去，以后再拆"的惰性做法
