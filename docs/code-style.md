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

## 命名规范

- **变量名描述内容，函数名描述动作**：命名即文档，看到名字即可理解其用途。
- **严禁无意义单字母、缩写或拼音**：除极短作用域的循环变量（如 `i`）外，禁止 `a`、`b`、`tmp` 等无意义命名；禁止拼音缩写（如 `je` 代表 `金额`）。
- **Rust**：采用 `snake_case`（函数、变量）、`PascalCase`（类型、枚举）、`SCREAMING_SNAKE_CASE`（常量）。
- **Kotlin**：采用 `camelCase`（函数、变量）、`PascalCase`（类、接口）、`SCREAMING_SNAKE_CASE`（常量）。
- **Composable 函数**：使用 `PascalCase`，与常规函数区分，以明确其 UI 声明式语义。

## 注释规范

- **注释只解释“为什么 (Why)”，不解释“是什么 (What)”**：代码本身说明做了什么，注释应说明为何这么做、设计权衡、潜在陷阱。
- **Public 接口必须包含标准文档注释**：参数、返回值、异常必须明确说明。
- **待办标记统一格式**：`// TODO: [具体描述]`，禁止无描述的裸 `// TODO`。
- **异常与日志**：严禁静默吞没异常，关键分支与异常捕获处必须附带上下文日志。

## 测试代码规范

- **物理隔离**：测试文件严禁与源码平铺。
  - Rust：必须放入 `<crate>/tests/` 目录，或使用 `#[cfg(test)]` 模块置于 `src/` 下的独立 `tests` 子模块。
  - Kotlin：必须放入 `src/test/java/` 对应包路径。
- **命名一致**：测试文件与源码文件命名严格对应。例如 `state.rs` 的测试对应 `tests/state_tests.rs`；`AppViewModel.kt` 的测试对应 `AppViewModelTest.kt`。
- **逢变必测**：核心逻辑的增改必须同步覆盖测试用例，严禁提交“零测试”业务代码。
- **测试独立**：单个测试用例之间不得存在执行顺序依赖，确保可并行运行。

## Git 提交规范

遵循 [Conventional Commits](https://www.conventionalcommits.org/) 规范，`<type>` 仅限以下 7 种：

| 类型 | 用途 |
|------|------|
| `feat` | 新功能 |
| `fix` | 缺陷修复 |
| `docs` | 文档变更（含 README、注释） |
| `style` | 代码风格调整（不影响逻辑的格式化、空行等） |
| `refactor` | 重构（既不修复 bug 也不添加功能的代码改动） |
| `test` | 测试相关（新增、修改测试代码） |
| `chore` | 构建、工具链、依赖升级等杂项 |

格式：

```text
<type>: <short description in lowercase>

- <action verb> <detailed change 1>
- <action verb> <detailed change 2>
```

## 工具链与格式化配置

### Rust

- **Edition**：`2024`（`moni-core`、`moni-contracts` 的 `Cargo.toml` 以及 `rustfmt.toml` 均指定）。
- **工具链**：`stable` channel，组件包含 `rustfmt`、`clippy`、`rust-src`。
- **格式化**（`rustfmt.toml`）：
  - `max_width = 100`
  - `hard_tabs = false`，`tab_spaces = 4`
  - `newline_style = "Unix"`
  - 启用字段初始化简写与 `?` 简写
- **Clippy**（根目录 `Cargo.toml` `[workspace.lints.clippy]`）：
  - `correctness`、`perf`、`suspicious` 级别为 `deny`
  - `complexity`、`style`、`pedantic` 级别为 `warn`
  - 针对性放宽：`module_name_repetitions`、`missing_errors_doc`、`missing_panics_doc`、`must_use_candidate`、`return_self_not_must_use`、`multiple_crate_versions`

### Kotlin / Android

- **AGP / Gradle**：`compileSdk = 36`，`minSdk = 28`，JDK 17。
- **NDK**：`29.0.14206865`。
- **Detekt**（`config/detekt/detekt.yml`）：
  - `LongMethod`：阈值 `200` 行（Compose `@Composable` 声明式 DSL 适当放宽）
  - `LongParameterList`：函数参数 `12`，构造参数 `10`
  - `TooManyFunctions`：每类 `25`，每文件 `30`
  - `MaxLineLength`：`140`
  - `ReturnCount`：`max = 4`
  - `FunctionNaming`：允许 PascalCase Composable 函数名
  - `ktlint`：启用 Android 模式，尾随逗号默认关闭

## 预测性拆分（提前行动）

**禁止被动等待拦截**：当文件行数达到预警阈值（Rust 300 行 / Kotlin 250 行 / Composable 80 行）时，主动规划拆分，不要等到 500 行硬上限才行动。
- 新增功能前评估：该功能应该放在现有文件还是新文件？
- 修改文件前查看当前行数，若已接近阈值，优先拆分再添加新代码
- 禁止"先凑合写进去，以后再拆"的惰性做法
