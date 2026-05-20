# AI 智能记账页面实施计划

## TL;DR

> **核心目标**：在 moni 应用中新增一个基于对话式交互的 AI 记账页面，实现"用户输入-AI识别-卡片微调-确认入账"的极简闭环。
>
> **范围边界**：仅实现 UI/UX 和能力框架，不接入真实 AI 模型、不做参数处理和提示词工程。模拟 AI 响应用于演示交互流程。
>
> **Deliverables**：
> - 新增 `chat_messages` 数据库表及迁移
> - 聊天消息数据模型和 Repository 层
> - AI 记账页面主 Screen（`AiBookkeepingScreen`）
> - 智能草稿卡片组件（`SmartDraftCard`）
> - 聊天消息气泡组件（用户气泡 + AI 卡片容器）
> - 底部动态输入栏（`DynamicInputBar`）
> - 首页 AI 入口 FAB
> - 导航路由集成
> - 完整的 Compose UI 测试覆盖
>
> **Estimated Effort**：Large（约 20-25 个任务，分 4 波执行）
> **Parallel Execution**：YES - 4 个执行波次
> **Critical Path**：数据库 Schema → 数据模型 → 导航/FAB → 主页面 → 卡片组件 → 输入栏 → 集成测试

---

## Context

### Original Request
用户希望根据参考设计文档，在 moni 记账应用中实现一个 AI 智能记账页面。该页面采用对话式 UI，用户通过自然语言输入消费信息，AI 解析后生成可编辑的记账卡片，用户确认后保存入账。

### Interview Summary
**关键讨论与决策**：
- **入口位置**：在首页（账单列表页）现有记账 FAB 的上方新增一个 AI 星星 FAB，点击进入 AI 记账页面
- **组件复用策略**：完全复用现有编辑器组件（`CustomNumPad`、`CategoryGridPager`、`RecordEditorState` 等）
- **多模态按钮处理**：语音和图片按钮显示但 disabled，点击提示"即将上线"
- **账单历史跳转**：右上角"账单历史"按钮跳转到现有的 `RecordListScreen`
- **聊天历史持久化**：保存到数据库，支持历史记录查看
- **范围限制**：不做真实 AI 模型接入、参数处理、提示词工程。模拟 AI 响应用于演示完整交互流程

**研究发现**：
- 项目使用 **Jetpack Compose + Material 3 Expressive** 构建 UI
- 采用 **MVI 架构**（`AppState` + `Event` 模式，Rust Core 通过 UniFFI 暴露）
- 编辑器子组件（`CustomNumPad`、`CategoryGridPager`）**已完全解耦**，可直接复用
- 数据库使用 **rusqlite 手动 SQL Schema**，当前版本 7
- 导航使用 **Compose Navigation**，`MoniNavHost` 集中管理路由
- 已有完整的 **主题系统**（预设颜色、动态主题、深色模式）
- 测试基础设施完善（`src/test` 单元测试 + `src/androidTest` 仪器测试）

### Metis Review
**识别的差距与解决方案**：
1. **聊天消息序列化**：定义消息类型枚举（文本/卡片/系统消息），使用 JSON 存储卡片数据
2. **模型能力设置**：暂时 hardcode 为"仅文本"模式，预留设置接口供后续扩展
3. **批量保存 UI**：参考方案中的"一键保存全部"按钮在单卡片场景隐藏，多卡片时显示
4. **草稿卡片视觉**：呼吸灯效果、已入账印章等需要独立实现，不依赖复用组件
5. **数据库迁移**：新增 `chat_messages` 表需要递增 schema 版本并编写迁移逻辑

---

## Work Objectives

### Core Objective
在 moni 应用中新增一个全功能的 AI 智能记账页面，包含对话式交互界面、可编辑的智能草稿卡片、动态输入栏和完整的聊天历史持久化，所有 UI 元素适配现有 Material 3 主题系统。

### Concrete Deliverables
1. **数据库层**：`chat_messages` 表定义、Repository 接口、Kotlin 数据类
2. **导航集成**：新增 `Screen.AiBookkeeping` 路由，首页 AI FAB 入口
3. **主页面**：`AiBookkeepingScreen` - 聊天历史列表 + 输入栏布局
4. **消息组件**：`UserMessageBubble`（用户气泡）、`AiMessageContainer`（AI 卡片容器）
5. **智能卡片**：`SmartDraftCard` - 可编辑的记账卡片，支持分类/金额/账户/时间/备注编辑
6. **输入栏**：`DynamicInputBar` - 文本输入 + 多模态按钮（disabled 状态）
7. **状态管理**：`AiBookkeepingState` / `AiBookkeepingViewModel` - 管理聊天状态、卡片状态机
8. **视觉效果**：呼吸灯动画、已入账印章、主题色适配
9. **测试覆盖**：单元测试 + Compose UI 测试

### Definition of Done
- [ ] 用户可以从首页通过 AI FAB 进入 AI 记账页面
- [ ] 用户可以输入文本，看到模拟的 AI 响应和智能卡片
- [ ] 用户可以点击卡片字段进行编辑（复用现有编辑器组件）
- [ ] 用户点击"确认保存"后卡片变为只读状态并显示"已入账"印章
- [ ] 聊天历史在页面重开后仍然保留
- [ ] 所有 UI 元素适配浅色/深色主题和动态主题色
- [ ] 多模态按钮显示但不可交互
- [ ] 所有测试通过

### Must Have
- [ ] 对话式聊天界面（用户气泡 + AI 卡片）
- [ ] 智能草稿卡片（可编辑字段 + 保存/取消）
- [ ] 卡片状态机（待确认 → 已保存 → 已失效）
- [ ] 底部动态输入栏（文本输入 + disabled 多模态按钮）
- [ ] 聊天历史持久化到数据库
- [ ] 主题适配（浅色/深色/动态主题色）
- [ ] 首页 AI 入口 FAB
- [ ] 导航路由集成

### Must NOT Have (Guardrails)
- [ ] **不做真实 AI 模型接入**（使用模拟数据演示交互）
- [ ] **不做参数处理和提示词工程**
- [ ] **不做语音/图片的真实功能**（仅 UI 占位）
- [ ] **不修改现有记账编辑器的核心逻辑**（仅复用 UI 组件）
- [ ] **不引入新的第三方库**（除非必要且经过确认）
- [ ] **不影响现有底部导航结构**（AI 记账是独立页面，不是新 Tab）

---

## Verification Strategy

> **ZERO HUMAN INTERVENTION** - ALL verification is agent-executed.

### Test Decision
- **Infrastructure exists**: YES
- **Automated tests**: Tests-after（先实现功能，后补充测试）
- **Framework**: JUnit 4 + Compose UI Test (`ComposeContentTestRule`)
- **Test locations**: `src/test/` for unit tests, `src/androidTest/` for UI tests

### QA Policy
Every task MUST include agent-executed QA scenarios.
Evidence saved to `.sisyphus/evidence/task-{N}-{scenario-slug}.{ext}`.

- **Frontend/UI**: Use Compose Testing (`composeTestRule`) - 查找节点、模拟点击、断言状态
- **Database**: Use Rust 测试或 Android 仪器测试验证查询
- **Navigation**: 使用 `NavHostController` 测试导航事件

---

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (基础层 - 数据库与数据模型，可立即并行启动):
├── Task 1: 数据库 Schema 更新（Rust 层） [quick]
├── Task 2: 聊天消息 Kotlin 数据模型 [quick]
├── Task 3: Repository 接口与实现 [quick]
└── Task 4: 导航路由定义与 FAB 入口 [quick]

Wave 2 (核心 UI 组件，依赖 Wave 1):
├── Task 5: AI 记账页面主框架 [unspecified-high]
├── Task 6: 用户消息气泡组件 [quick]
├── Task 7: AI 消息容器组件 [quick]
├── Task 8: 动态输入栏组件 [quick]
└── Task 9: 智能草稿卡片外壳 [unspecified-high]

Wave 3 (卡片交互与状态机，依赖 Wave 2):
├── Task 10: 卡片内字段编辑集成（分类/金额/账户/时间/备注） [deep]
├── Task 11: 卡片状态机实现（Draft/Saved/Expired） [deep]
├── Task 12: 视觉效果（呼吸灯、印章、主题适配） [visual-engineering]
├── Task 13: 聊天历史列表与自动滚动 [unspecified-high]
└── Task 14: 模拟 AI 响应逻辑 [quick]

Wave 4 (集成与测试，依赖 Wave 3):
├── Task 15: ViewModel 状态管理集成 [deep]
├── Task 16: 页面级集成（导航 + 主题 + 边缘情况） [unspecified-high]
├── Task 17: 单元测试 [quick]
├── Task 18: Compose UI 测试 [unspecified-high]
└── Task 19: 构建验证与 APK 打包 [quick]

Wave FINAL (审查验证):
├── Task F1: 计划合规审计 [oracle]
├── Task F2: 代码质量审查 [unspecified-high]
├── Task F3: 实机 QA 测试 [unspecified-high]
└── Task F4: 范围保真度检查 [deep]
```

### Dependency Matrix

| Task | Blocked By | Blocks |
|------|-----------|--------|
| 1 (Schema) | - | 3, 15, 17 |
| 2 (Data Model) | - | 3, 5, 6, 7 |
| 3 (Repository) | 1, 2 | 5, 15 |
| 4 (Navigation) | - | 5, 16 |
| 5 (Main Screen) | 2, 3, 4 | 13, 15, 16 |
| 6 (User Bubble) | 2 | 5 |
| 7 (AI Container) | 2 | 5, 9 |
| 8 (Input Bar) | - | 5 |
| 9 (Card Shell) | - | 10, 11, 12 |
| 10 (Field Editing) | 9 | 11, 15 |
| 11 (State Machine) | 9, 10 | 15 |
| 12 (Visual Effects) | - | 16 |
| 13 (Chat List) | 5 | 16 |
| 14 (Mock AI) | 2 | 15 |
| 15 (ViewModel) | 1, 3, 5, 10, 11, 14 | 16, 17 |
| 16 (Integration) | 4, 5, 12, 13, 15 | 17, 18, 19 |
| 17 (Unit Tests) | 1, 15, 16 | 19 |
| 18 (UI Tests) | 16 | 19 |
| 19 (Build) | 17, 18 | F1-F4 |

### Agent Dispatch Summary

- **Wave 1**: **4** 个任务 → 全部 `quick`（基础架构，短平快）
- **Wave 2**: **5** 个任务 → `quick` + `unspecified-high`（UI 组件搭建）
- **Wave 3**: **5** 个任务 → `deep` + `visual-engineering`（核心交互逻辑）
- **Wave 4**: **5** 个任务 → `deep` + `unspecified-high` + `quick`（集成与测试）
- **Wave FINAL**: **4** 个审查任务 → 并行执行

---

## TODOs

### Wave 1: 基础层（数据库与数据模型）

- [x] **1. 数据库 Schema 更新（Rust 层）**

  **What to do**:
  - 在 Rust Core 的 `schema.rs` 中新增 `chat_messages` 表定义
  - 递增 `CURRENT_SCHEMA_VERSION` 从 7 到 8
  - 添加 v7→v8 的迁移逻辑
  - 表结构：id, session_id, message_type, content, card_data_json, status, created_at

  **Must NOT do**: 不修改现有表结构，不引入 ORM

  **Recommended Agent Profile**: `quick`

  **Parallelization**: YES (Wave 1, 与 Task 2-4 并行)
  - **Blocks**: Task 3 (Repository)
  - **Blocked By**: None

  **References**:
  - `moni-core/src/schema.rs` - 现有 schema 定义
  - `moni-core/src/lib.rs` - 数据库初始化

  **Acceptance Criteria**:
  - [ ] `cargo check` 通过
  - [ ] `CURRENT_SCHEMA_VERSION` == 8
  - [ ] 迁移函数存在

  **QA Scenarios**:
  - `cargo check` 编译成功 → `.sisyphus/evidence/task-1-cargo-check.txt`

  **Commit**: `feat(db): add chat_messages table schema and v8 migration`

- [x] **2. 聊天消息 Kotlin 数据模型**

  **What to do**:
  - 创建 `ChatMessage` 数据类（id, sessionId, messageType, content, cardData, status, createdAt）
  - 创建 `MessageType` 枚举（USER_TEXT, AI_CARD, AI_TEXT, SYSTEM）
  - 创建 `CardStatus` 枚举（DRAFT, SAVED, EXPIRED）
  - 创建 `DraftCardData` 数据类（amountCents, recordType, categoryId, accountId, timestamp, note）

  **Must NOT do**: 不引入复杂序列化库，不包含 AI 相关字段

  **Recommended Agent Profile**: `quick`

  **Parallelization**: YES (Wave 1, 与 Task 1, 3, 4 并行)
  - **Blocks**: Task 3 (Repository), Task 5 (Main Screen)
  - **Blocked By**: None

  **References**:
  - `moni-contracts/src/record.rs` - Record 结构
  - `moni-contracts/src/category.rs` - Category 结构

  **Acceptance Criteria**:
  - [ ] 所有数据类编译通过
  - [ ] 包含合理的默认值

  **QA Scenarios**:
  - `./gradlew :app:compileDebugKotlin` 成功 → `.sisyphus/evidence/task-2-compile.txt`

  **Commit**: `feat(model): add chat message data models`

- [x] **3. Repository 接口与实现**

  **What to do**:
  - 创建 `ChatRepository` 接口（getMessages, saveMessage, updateMessageStatus, deleteMessage, clearSession）
  - 创建 `ChatRepositoryImpl`（使用 Rust Core 数据库操作）
  - 在 Rust Core 添加 CRUD 函数并通过 UniFFI 暴露
  - 支持按 session_id 分页查询

  **Must NOT do**: 不实现复杂查询，不添加缓存层

  **Recommended Agent Profile**: `unspecified-high`

  **Parallelization**: YES (Wave 1, 依赖 Task 1+2)
  - **Blocks**: Task 5 (Main Screen), Task 15 (ViewModel)
  - **Blocked By**: Task 1 (Schema), Task 2 (Data Model)

  **References**:
  - `moni-core/src/record_repo.rs` - Record Repository 模式
  - `moni-core/src/lib.rs` - UniFFI 暴露

  **Acceptance Criteria**:
  - [ ] Rust CRUD 通过 `cargo test`
  - [ ] Kotlin Repository 编译通过
  - [ ] 基础 CRUD 可正常调用

  **QA Scenarios**:
  - `cargo test chat` 通过 → `.sisyphus/evidence/task-3-rust-test.txt`
  - `./gradlew :app:compileDebugKotlin` 成功 → `.sisyphus/evidence/task-3-kotlin-compile.txt`

  **Commit**: `feat(repo): add chat message repository and rust CRUD`

- [x] **4. 导航路由定义与 FAB 入口**

  **What to do**:
  - 在 `Screen.kt` 中新增 `Screen.AiBookkeeping` 路由
  - 在 `MoniNavHost.kt` 中注册路由
  - 修改 `RecordListScreen` 在 FAB 上方新增 AI 星星 FAB
  - AI FAB 使用 `Icons.Default.Star`（或 `Icons.Default.SmartToy`）图标
  - 点击导航到 AI 记账页面

  **Must NOT do**: 不修改底部导航栏，不影响现有 FAB

  **Recommended Agent Profile**: `quick`

  **Parallelization**: YES (Wave 1, 与 Task 1-3 并行)
  - **Blocks**: Task 5 (Main Screen), Task 16 (Integration)
  - **Blocked By**: None

  **References**:
  - `navigation/Screen.kt` - Screen 密封类
  - `navigation/MoniNavHost.kt` - 路由注册
  - `ui/record/RecordListScreen.kt` - 现有 FAB

  **Acceptance Criteria**:
  - [ ] 导航路由编译通过
  - [ ] 首页显示 AI FAB
  - [ ] 点击正确导航

  **QA Scenarios**:
  - Compose UI Test: 查找 AI FAB，点击，断言路由 → `.sisyphus/evidence/task-4-fab-navigation.png`

  **Commit**: `feat(nav): add AI bookkeeping route and FAB entry`

### Wave 2: 核心 UI 组件

- [x] **5. AI 记账页面主框架**

  **What to do**:
  - 创建 `AiBookkeepingScreen.kt` 主页面
  - 结构：顶部导航栏 + 聊天历史区 + 底部输入栏
  - 使用 `Scaffold`，`TopAppBar` 标题"记账助手"
  - 聊天区使用 `LazyColumn`，支持自动滚动
  - 背景使用中性色（浅色 #F5F6F7 / 深色 #121212）

  **Must NOT do**: 不实现消息数据和交互逻辑（仅框架）

  **Recommended Agent Profile**: `unspecified-high`

  **Parallelization**: NO (Wave 2, 依赖 Wave 1)
  - **Blocks**: Task 13 (Chat List), Task 15 (ViewModel)
  - **Blocked By**: Task 2 (Data Model), Task 3 (Repository), Task 4 (Navigation)

  **References**:
  - `ui/record/RecordListScreen.kt` - 页面布局参考
  - `ui/settings/SettingsScreen.kt` - Scaffold 参考

  **Acceptance Criteria**:
  - [ ] 页面可正常打开
  - [ ] 导航栏包含返回、标题、账单历史按钮
  - [ ] 背景为中性色
  - [ ] 聊天区域可滚动

  **QA Scenarios**:
  - Compose UI Test: 断言标题、按钮、输入栏存在 → `.sisyphus/evidence/task-5-screen-structure.png`

  **Commit**: `feat(ui): add AI bookkeeping screen framework`

- [x] **6. 用户消息气泡组件**

  **What to do**:
  - 创建 `UserMessageBubble.kt`
  - 气泡位于右侧（end alignment），圆角设计
  - 背景色：当前主题色的极浅色（10% 饱和度）
  - 文字颜色：`MaterialTheme.colorScheme.onSurface`
  - 遵循 Material 3 规范的内边距和字体

  **Must NOT do**: 不支持图片/语音气泡（仅文本）

  **Recommended Agent Profile**: `quick`

  **Parallelization**: YES (Wave 2)
  - **Blocks**: Task 5 (Main Screen)
  - **Blocked By**: Task 2 (Data Model)

  **References**:
  - Material 3 Card / Surface
  - `theme/Color.kt` - 主题色访问

  **Acceptance Criteria**:
  - [ ] 气泡预览正确
  - [ ] 背景色随主题色变化
  - [ ] 文本正确渲染

  **QA Scenarios**:
  - Compose UI Test: 传入测试文本，断言右对齐和样式 → `.sisyphus/evidence/task-6-user-bubble.png`

  **Commit**: `feat(ui): add user message bubble component`

- [x] **7. AI 消息容器组件**

  **What to do**:
  - 创建 `AiMessageContainer.kt`
  - 容器位于左侧（start alignment）
  - 支持两种内容：文本消息 和 智能卡片
  - 文本消息：白色/深色背景卡片
  - 卡片消息：嵌套 `SmartDraftCard`
  - 统一的边距和排版

  **Must NOT do**: 不实现卡片内部逻辑（仅外壳）

  **Recommended Agent Profile**: `quick`

  **Parallelization**: YES (Wave 2)
  - **Blocks**: Task 5 (Main Screen)
  - **Blocked By**: Task 2 (Data Model)

  **References**:
  - Material 3 Card
  - 现有 `MoniCard`（如存在）

  **Acceptance Criteria**:
  - [ ] 可包裹文本和卡片
  - [ ] 左侧对齐
  - [ ] 边距一致

  **QA Scenarios**:
  - Compose UI Test: 传入文本，断言左对齐和显示 → `.sisyphus/evidence/task-7-ai-container.png`

  **Commit**: `feat(ui): add AI message container component`

- [x] **8. 动态输入栏组件**

  **What to do**:
  - 创建 `DynamicInputBar.kt`
  - 左侧：`[+]` 扩展按钮（disabled，展开相册/相机子按钮）
  - 中间：文本输入框，支持多行
  - 右侧：`[麦克风]` disabled + `[发送]` 按钮
  - 多模态按钮 disabled：灰色图标，点击 Toast"即将上线"
  - Placeholder: "说点什么，如：请朋友喝奶茶32元"
  - 输入框高度自适应

  **Must NOT do**: 不实现真实图片/语音功能，不实现发送逻辑（仅回调）

  **Recommended Agent Profile**: `quick`

  **Parallelization**: YES (Wave 2)
  - **Blocks**: Task 5 (Main Screen)
  - **Blocked By**: None

  **References**:
  - Material 3 `TextField`, `IconButton`
  - `components/MoniTextField.kt`（如存在）

  **Acceptance Criteria**:
  - [ ] 可输入文本
  - [ ] 发送按钮在输入非空时可用
  - [ ] 多模态按钮 disabled
  - [ ] Placeholder 正确显示

  **QA Scenarios**:
  - Compose UI Test: 输入文本，断言发送按钮状态，点击麦克风无响应 → `.sisyphus/evidence/task-8-input-bar.png`

  **Commit**: `feat(ui): add dynamic input bar with disabled multimodal buttons`

- [x] **9. 智能草稿卡片外壳**

  **What to do**:
  - 创建 `SmartDraftCard.kt`
  - 头部：分类 ICON + 收支类型切换
  - 中部：超大字号金额 + 账户 + 时间 + 备注
  - 尾部：取消按钮 + 保存按钮
  - 白色/深色背景，圆角 16dp

  **Must NOT do**: 不实现字段编辑交互逻辑，不实现状态机动画

  **Recommended Agent Profile**: `unspecified-high`

  **Parallelization**: YES (Wave 2)
  - **Blocks**: Task 10 (Field Editing), Task 11 (State Machine)
  - **Blocked By**: None

  **References**:
  - `RecordEditorPanel.kt` - 编辑器布局
  - `RecordEditorState.kt` - 状态字段
  - Material 3 Card, Button

  **Acceptance Criteria**:
  - [ ] 布局与参考设计一致
  - [ ] 所有字段区域可点击
  - [ ] 取消/保存按钮显示正确

  **QA Scenarios**:
  - Compose UI Test: 传入测试数据，断言金额、图标、按钮 → `.sisyphus/evidence/task-9-card-shell.png`

  **Commit**: `feat(ui): add smart draft card shell layout`

### Wave 3: 卡片交互与状态机

- [ ] **10. 卡片内字段编辑集成**

  **What to do**:
  - 复用 `CustomNumPad` 实现金额编辑
  - 复用 `CategoryGridPager` 实现分类选择
  - 复用现有时间选择器实现时间编辑
  - 复用现有账户选择弹窗实现账户编辑
  - 备注编辑：点击变为可编辑文本框
  - 编辑完成后更新卡片数据状态

  **Must NOT do**: 不修改 `CustomNumPad`/`CategoryGridPager` 内部实现，不引入新选择器

  **Recommended Agent Profile**: `deep`

  **Parallelization**: YES (Wave 3, 依赖 Task 9)
  - **Blocks**: Task 11 (State Machine), Task 15 (ViewModel)
  - **Blocked By**: Task 9 (Card Shell)

  **References**:
  - `CustomNumPad.kt` - 数字键盘 API
  - `CategoryGridPager.kt` - 分类网格 API
  - 现有时间选择器组件
  - 现有账户选择弹窗

  **Acceptance Criteria**:
  - [ ] 点击金额弹出数字键盘，编辑后更新
  - [ ] 点击分类弹出九宫格，选择后更新
  - [ ] 点击时间弹出选择器，选择后更新
  - [ ] 点击账户弹出弹窗，选择后更新
  - [ ] 备注可编辑

  **QA Scenarios**:
  - Compose UI Test: 点击金额，输入50，断言显示50.00 → `.sisyphus/evidence/task-10-amount-edit.png`
  - Compose UI Test: 点击分类，选择交通，断言图标更新 → `.sisyphus/evidence/task-10-category-edit.png`

  **Commit**: `feat(ui): integrate field editors into smart draft card`

- [ ] **11. 卡片状态机实现（Draft / Saved / Expired）**

  **What to do**:
  - **Draft 态**：卡片边缘呼吸灯光晕（主题色），所有字段可编辑，显示取消/保存按钮
  - **Saved 态**：亮度降低 20%，按钮消失，右下角显示"已入账"印章，右上角显示铅笔图标
  - **Expired 态**：灰色半透明，所有热区失效，无按钮
  - 状态转换：Draft→Saved（点击保存），Saved→Draft（点击铅笔），Draft→Expired（超过24小时或新对话）
  - 使用 `animate*AsState` 实现切换动画

  **Must NOT do**: 不使用复杂定时器（时间戳比较），已失效卡片不删除

  **Recommended Agent Profile**: `deep`

  **Parallelization**: YES (Wave 3, 依赖 Task 9)
  - **Blocks**: Task 15 (ViewModel)
  - **Blocked By**: Task 9 (Card Shell)

  **References**:
  - Compose Animation API
  - `RecordEditorState.kt` - 状态管理
  - Material 3 主题色

  **Acceptance Criteria**:
  - [ ] Draft 态有呼吸灯效果
  - [ ] 保存后变为 Saved 态（印章显示）
  - [ ] 点击铅笔返回 Draft 态
  - [ ] Expired 态不可交互
  - [ ] 状态切换有动画

  **QA Scenarios**:
  - Compose UI Test: 断言 Draft 态有呼吸灯，点击保存，断言 Saved 态 → `.sisyphus/evidence/task-11-save-card.png`
  - Compose UI Test: 点击铅笔，断言返回 Draft 态 → `.sisyphus/evidence/task-11-edit-again.png`

  **Commit**: `feat(ui): implement smart draft card state machine`

- [ ] **12. 视觉效果（呼吸灯、印章、主题适配）**

  **What to do**:
  - **呼吸灯**：`InfiniteTransition` 实现边框颜色周期性变化（主题色 30%→60% 透明度，周期2秒）
  - **已入账印章**：旋转 -15 度的"已入账"文字（红色/主题色），覆盖右下角
  - **主题适配**：所有动态元素随主题色变化
  - **深色模式**：验证所有效果在深色模式下可见

  **Must NOT do**: 不使用复杂自定义绘制（优先 Compose 内置动画）

  **Recommended Agent Profile**: `visual-engineering`

  **Parallelization**: YES (Wave 3)
  - **Blocks**: Task 16 (Integration)
  - **Blocked By**: None

  **References**:
  - Compose Animation 文档
  - `theme/Color.kt` - 主题色系统
  - `ThemeMode` - 主题切换

  **Acceptance Criteria**:
  - [ ] 呼吸灯动画流畅（周期 2 秒）
  - [ ] 印章文字清晰可读
  - [ ] 主题切换时动态元素跟随变化
  - [ ] 深色模式下效果正常

  **QA Scenarios**:
  - Compose UI Test: 切换主题色，断言卡片边框颜色变化 → `.sisyphus/evidence/task-12-theme-switch.png`

  **Commit**: `feat(ui): add visual effects and theme adaptation`

- [ ] **13. 聊天历史列表与自动滚动**

  **What to do**:
  - 在 `AiBookkeepingScreen` 的 `LazyColumn` 中集成真实消息数据
  - 消息按时间顺序排列（用户右对齐，AI 左对齐）
  - 自动滚动到底部（新消息时）
  - 支持分页加载历史（滚动到顶部触发）
  - 空状态显示引导提示

  **Must NOT do**: 不实现消息搜索，不实现消息删除/编辑

  **Recommended Agent Profile**: `unspecified-high`

  **Parallelization**: YES (Wave 3, 依赖 Task 5)
  - **Blocks**: Task 16 (Integration)
  - **Blocked By**: Task 5 (Main Screen)

  **References**:
  - Compose `LazyColumn`, `LazyListState`
  - `ChatRepository.getMessages()` - 分页查询

  **Acceptance Criteria**:
  - [ ] 消息按时间顺序显示
  - [ ] 新消息自动滚动到底部
  - [ ] 空状态显示引导
  - [ ] 可分页加载历史

  **QA Scenarios**:
  - Compose UI Test: 发送消息，断言列表滚动到底部 → `.sisyphus/evidence/task-13-auto-scroll.png`

  **Commit**: `feat(ui): integrate chat history list with auto-scroll`

- [ ] **14. 模拟 AI 响应逻辑**

  **What to do**:
  - 创建 `MockAiService` - 模拟 AI 解析（仅 UI 演示用）
  - 简单文本解析规则：
    - 提取金额（正则匹配数字 + "元/块"）
    - 提取分类关键词映射到现有分类
    - 时间解析（"今天"、"昨天"、"前天"、具体日期）
    - 生成 `DraftCardData` 和 AI 回复文本
  - 模拟延迟：0.5 秒骨架屏后显示结果
  - 非记账文本识别：无法解析金额时返回礼貌文本回复

  **Must NOT do**: 不接入真实 LLM API，不做复杂 NLP，不处理图片/语音

  **Recommended Agent Profile**: `quick`

  **Parallelization**: YES (Wave 3)
  - **Blocks**: Task 15 (ViewModel)
  - **Blocked By**: Task 2 (Data Model)

  **References**:
  - `CoreRecord`, `CoreCategory` - 数据模型
  - 现有分类列表（关键词映射）
  - Kotlin Regex API

  **Acceptance Criteria**:
  - [ ] "中午吃麦当劳花了35" → 餐饮/35元/当前时间
  - [ ] "昨天打车45" → 交通/45元/昨天
  - [ ] "今天天气真好" → 礼貌回复，无卡片
  - [ ] 解析失败 → 空白模板卡片（金额空，分类未分类）

  **QA Scenarios**:
  - 单元测试: 调用 parse("请朋友喝奶茶32元")，断言 amount=3200, category=餐饮 → `.sisyphus/evidence/task-14-mock-parse-test.txt`

  **Commit**: `feat(logic): add mock AI parsing service`

### Wave 4: 集成与测试

- [ ] **15. ViewModel 状态管理集成**

  **What to do**:
  - 创建 `AiBookkeepingViewModel`
  - 状态：消息列表、输入文本、当前编辑卡片、加载状态
  - 处理操作：发送消息、保存卡片、取消卡片、编辑字段
  - 协调 `ChatRepository` 和 `MockAiService`
  - 使用 `viewModelScope` 管理协程

  **Must NOT do**: 不引入新的状态管理库，不在 ViewModel 中处理 UI 细节

  **Recommended Agent Profile**: `deep`

  **Parallelization**: NO (Wave 4, 依赖 Wave 3)
  - **Blocks**: Task 16 (Integration)
  - **Blocked By**: Task 1 (Schema), Task 3 (Repository), Task 5 (Main Screen), Task 10 (Field Editing), Task 11 (State Machine), Task 14 (Mock AI)

  **References**:
  - `AppViewModel.kt` - 现有 ViewModel 模式
  - `RecordEditorState.kt` - 编辑器状态管理
  - Android `ViewModel`, `StateFlow`

  **Acceptance Criteria**:
  - [ ] 发送消息后列表更新
  - [ ] AI 响应后卡片正确显示
  - [ ] 保存后状态更新并持久化
  - [ ] 页面旋转后状态保留

  **QA Scenarios**:
  - Compose UI Test: 输入"午饭25元"，发送，等待响应，保存，断言 Saved 态 → `.sisyphus/evidence/task-15-full-flow.png`

  **Commit**: `feat(vm): add AiBookkeepingViewModel`

- [ ] **16. 页面级集成（导航 + 主题 + 边缘情况）**

  **What to do**:
  - 集成所有组件到 `AiBookkeepingScreen`
  - 连接 ViewModel 和 UI
  - 边缘情况处理：
    - 识别失败：空白模板卡片，金额自动激活
    - 字段缺失：预测默认值，字段半透明显示
    - 非记账文本：AI 礼貌回复，无卡片
    - 空输入：发送按钮禁用
  - "账单历史"按钮导航到 `RecordListScreen`
  - 确保导航返回操作正确

  **Must NOT do**: 不实现新页面或功能（仅连接现有组件）

  **Recommended Agent Profile**: `unspecified-high`

  **Parallelization**: NO (Wave 4)
  - **Blocks**: Task 17 (Unit Tests), Task 18 (UI Tests), Task 19 (Build)
  - **Blocked By**: Task 4 (Navigation), Task 5 (Main Screen), Task 12 (Visual Effects), Task 13 (Chat List), Task 15 (ViewModel)

  **References**:
  - `MoniNavHost.kt` - 导航模式
  - `AiBookkeepingViewModel` - 状态管理
  - 参考方案中的边缘情况处理

  **Acceptance Criteria**:
  - [ ] 所有组件正确协作
  - [ ] 边缘情况正确处理
  - [ ] 导航流畅
  - [ ] 主题在所有状态下正确

  **QA Scenarios**:
  - Compose UI Test: 输入"今天天气真好"，断言 AI 文本回复，断言无卡片 → `.sisyphus/evidence/task-16-edge-case.png`

  **Commit**: `feat(integration): integrate all components and edge cases`

- [ ] **17. 单元测试**

  **What to do**:
  - 为 `MockAiService` 编写单元测试（各种输入解析）
  - 为 `ChatRepository` 编写单元测试（CRUD 操作）
  - 为 ViewModel 编写单元测试（状态流转、用户操作）
  - 使用 `kotlinx-coroutines-test`
  - 使用 MockK 模拟依赖

  **Must NOT do**: 不测试 Compose UI（留到仪器测试），不测试 Rust 代码

  **Recommended Agent Profile**: `quick`

  **Parallelization**: YES (Wave 4, 依赖 Task 15+16)
  - **Blocks**: Task 19 (Build)
  - **Blocked By**: Task 1 (Schema), Task 15 (ViewModel), Task 16 (Integration)

  **References**:
  - 现有单元测试文件（`src/test/`）
  - `AppStateTest.kt` - 状态测试
  - `CoreIntentTest.kt` - 意图测试

  **Acceptance Criteria**:
  - [ ] 所有新增单元测试通过
  - [ ] 测试覆盖率 >= 70%（核心业务逻辑）

  **QA Scenarios**:
  - `./gradlew :app:test` 通过 → `.sisyphus/evidence/task-17-unit-tests.txt`

  **Commit**: `test: add unit tests for AI bookkeeping logic`

- [ ] **18. Compose UI 测试**

  **What to do**:
  - 编写 Compose UI 测试验证关键交互：
    - 导航到 AI 记账页面
    - 发送消息并查看响应
    - 编辑卡片字段
    - 保存卡片
    - 验证状态切换
  - 使用 `composeTestRule`
  - 使用内存数据库或 Mock Repository

  **Must NOT do**: 不测试所有边缘情况（覆盖核心流程），不使用真实数据库

  **Recommended Agent Profile**: `unspecified-high`

  **Parallelization**: YES (Wave 4, 依赖 Task 16)
  - **Blocks**: Task 19 (Build)
  - **Blocked By**: Task 16 (Integration)

  **References**:
  - `CategoryListScreenTest.kt` - 现有 UI 测试
  - `MoniCardTest.kt` - 组件测试
  - Compose Testing 文档

  **Acceptance Criteria**:
  - [ ] UI 测试编译并通过
  - [ ] 核心交互流程覆盖

  **QA Scenarios**:
  - `./gradlew :app:connectedAndroidTest` 通过 → `.sisyphus/evidence/task-18-ui-tests.txt`

  **Commit**: `test: add compose UI tests for AI bookkeeping screen`

- [ ] **19. 构建验证与 APK 打包**

  **What to do**:
  - 运行 `./gradlew :app:assembleDebug`
  - 确保无编译错误和警告
  - 检查 ProGuard/R8 规则
  - 构建 debug 签名的 arm64 release APK 到 `/home/agguy/share/moni-release`
  - 提交所有更改到 git

  **Must NOT do**: 不引入新的编译警告或错误，不修改构建配置（除非必要）

  **Recommended Agent Profile**: `quick`

  **Parallelization**: NO (Wave 4 最后执行)
  - **Blocks**: Wave FINAL (F1-F4)
  - **Blocked By**: Task 17 (Unit Tests), Task 18 (UI Tests)

  **References**:
  - `android/app/build.gradle.kts` - 构建配置
  - 根目录 `build.gradle.kts` - Rust 集成
  - 项目 AGENTS.md - APK 输出路径

  **Acceptance Criteria**:
  - [ ] `./gradlew :app:assembleDebug` 成功
  - [ ] 无新增编译警告
  - [ ] APK 输出到指定目录
  - [ ] 所有 git 更改已提交

  **QA Scenarios**:
  - `./gradlew :app:assembleDebug` BUILD SUCCESSFUL → `.sisyphus/evidence/task-19-build-success.txt`
  - `ls /home/agguy/share/moni-release/*.apk` 文件存在 → `.sisyphus/evidence/task-19-apk-exists.txt`

  **Commit**: `chore: build debug APK and finalize`

---

## Final Verification Wave

> 4 个审查代理并行执行。全部通过后才算完成。

- [ ] **F1. 计划合规审计** — `oracle`

  检查 Must Have / Must NOT Have：
  - 对话式聊天界面（用户气泡 + AI 卡片）
  - 智能草稿卡片（可编辑字段 + 保存/取消）
  - 卡片状态机（Draft/Saved/Expired）
  - 底部动态输入栏（文本输入 + disabled 多模态按钮）
  - 聊天历史持久化
  - 主题适配（浅色/深色/动态主题色）
  - 首页 AI 入口 FAB
  - 导航路由集成
  - 无真实 AI 模型接入
  - 无参数处理和提示词工程
  - 无语音/图片真实功能

  输出：`Must Have [N/N] | Must NOT Have [N/N] | Tasks [N/N] | VERDICT`

- [ ] **F2. 代码质量审查** — `unspecified-high`

  运行 `./gradlew :app:lint` + `./gradlew :app:detekt`（如有）
  检查新增文件：
  - 无 `as Any` / `@Suppress` 滥用
  - 无空 catch 块
  - 无 `println` / `Log.d` 遗留
  - 无未使用导入
  - 命名符合项目规范
  - 函数 < 50 行，类 < 300 行

  输出：`Build [PASS/FAIL] | Lint [PASS/FAIL] | Files [N clean/N issues] | VERDICT`

- [ ] **F3. 实机 QA 测试** — `unspecified-high`

  在真实设备或模拟器上执行：
  - 进入 AI 记账页面
  - 发送多条记账消息
  - 编辑卡片各字段
  - 保存卡片
  - 验证状态切换
  - 切换主题色验证适配
  - 验证深色模式
  - 测试返回导航
  - 截图保存到 `.sisyphus/evidence/final-qa/`

  输出：`Scenarios [N/N pass] | Integration [N/N] | Edge Cases [N tested] | VERDICT`

- [ ] **F4. 范围保真度检查** — `deep`

  对比计划和实际实现：
  - 所有 Must Have 已实现（无遗漏）
  - 无超出范围的代码（无模型接入、无提示词）
  - Must NOT do 全部遵守
  - 无跨任务污染

  输出：`Tasks [N/N compliant] | Contamination [CLEAN/N issues] | VERDICT`

---

## Commit Strategy

采用 **Wave-based 分组提交**策略：

| 批次 | 提交信息 | 包含任务 |
|------|---------|---------|
| 1 | `feat(db): add chat_messages table schema and v8 migration` | Task 1 |
| 2 | `feat(model): add chat message data models` | Task 2 |
| 3 | `feat(repo): add chat message repository and rust CRUD` | Task 3 |
| 4 | `feat(nav): add AI bookkeeping route and FAB entry` | Task 4 |
| 5 | `feat(ui): add AI bookkeeping screen and components` | Task 5-9 |
| 6 | `feat(ui): integrate card editors and state machine` | Task 10-14 |
| 7 | `feat(integration): integrate all components and edge cases` | Task 15-16 |
| 8 | `test: add unit and UI tests for AI bookkeeping` | Task 17-18 |
| 9 | `chore: build debug APK and finalize` | Task 19 |

---

## Success Criteria

### Verification Commands
```bash
# Rust 编译检查
cd moni-core && cargo check

# Kotlin 编译检查
cd android && ./gradlew :app:compileDebugKotlin

# 单元测试
./gradlew :app:test

# UI 测试
./gradlew :app:connectedAndroidTest

# 完整构建
./gradlew :app:assembleDebug

# APK 输出验证
ls /home/agguy/share/moni-release/*.apk
```

### Final Checklist
- [ ] 所有 Must Have 已实现
- [ ] 所有 Must NOT Have 已遵守
- [ ] 所有 Wave 1-4 任务完成
- [ ] Final Verification Wave (F1-F4) 全部通过
- [ ] 代码质量检查通过（lint + 规范）
- [ ] 测试覆盖率 >= 70%
- [ ] 构建成功，APK 输出到指定目录
- [ ] 所有更改已提交到 git
- [ ] 无新增编译警告或错误
