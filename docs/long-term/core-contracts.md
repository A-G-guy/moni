# Kotlin ↔ Rust 核心数据契约

## 1. 契约概述

采用 **JSON 字符串** 作为跨语言媒介：

- **简单**：无需额外 schema 定义，双方直接依赖各自语言的序列化库。
- **调试友好**：日志中可直接打印原始 JSON，便于抓包排查。
- **自动转换**：`snake_case`（Rust）↔ `camelCase`（Kotlin）由 `serde` 与 `kotlinx.serialization` 自动处理，开发者无感知。

> 交互模式：Kotlin 将 `CoreIntent` 序列化为 JSON 字符串传入 Rust FFI；Rust 处理完毕后返回 `CoreUpdate`（含状态 JSON + 副作用列表）。

---

## 2. Intent 协议表

### 2.1 记录（record）

| Intent | 必需字段 | 可选字段 | 返回值说明 |
|--------|---------|---------|-----------|
| `RecordCreate` | `amountCents`, `recordType`, `categoryId`, `note` | `timestamp` | `records`, `recordGroups` 刷新；`monthlySummaries` 刷新 |
| `RecordUpdate` | `id` | `amountCents`, `recordType`, `categoryId`, `note` | 同上 |
| `RecordDelete` | `id` | — | 同上 |
| `RecordList` | — | `page`(0), `pageSize`(50) | `records` 填充 |
| `RecordListByMonth` | `yearMonth` | — | `recordGroups` 填充 |
| `RecordGet` | `id` | — | `ui.selectedRecordId` 设置 |

### 2.2 分类（category）

| Intent | 必需字段 | 可选字段 | 返回值说明 |
|--------|---------|---------|-----------|
| `CategoryCreate` | `name`, `categoryType`, `iconName` | `description`, `parentId` | `categories` 刷新 |
| `CategoryUpdate` | `id` | `name`, `description`, `iconName`, `parentId`, `clearParentId`(false) | 同上 |
| `CategoryArchive` | `id` | — | 同上 |
| `CategoryUnarchive` | `id` | — | 同上 |
| `CategoryList` | — | — | `categories` 填充 |

### 2.3 统计（stats）

| Intent | 必需字段 | 可选字段 | 返回值说明 |
|--------|---------|---------|-----------|
| `StatsMonthlySummary` | — | `months`(6) | `monthlySummaries` 填充 |
| `StatsCategoryBreakdown` | `yearMonth` | `aggregateByParent`(false) | `currentMonthBreakdown` 填充 |

### 2.4 设置（settings）

| Intent | 必需字段 | 可选字段 | 返回值说明 |
|--------|---------|---------|-----------|
| `SettingsUpdateCurrency` | `symbol` | — | `settings.currencySymbol` 更新 |

### 2.5 开发者（dev）

| Intent | 必需字段 | 可选字段 | 返回值说明 |
|--------|---------|---------|-----------|
| `DevClearAllData` | — | — | 全局状态重置为默认值 |
| `DevGenerateMockData` | `count`, `preset` | — | 全量数据刷新 |
| `DevSeedPresets` | — | — | 预设分类数据写入，`categories` 刷新 |

### 2.6 UI 导航

| Intent | 必需字段 | 可选字段 | 返回值说明 |
|--------|---------|---------|-----------|
| `NavigateTo` | `screen` | — | `ui.activeTab` 更新 |
| `DismissError` | — | — | `ui.errorMessage` 置空 |

---

## 3. Effect 协议表

| kind | payload 结构 | 触发时机 |
|------|-------------|---------|
| `show_snackbar` | `{ "message": String }` | 操作成功/失败需提示用户时 |
| `navigate` | `{ "screen": String }` | 业务逻辑要求跳转页面时 |
| `log` | `{ "level": String, "message": String }` | 需要向 Android 日志系统输出时 |

> 所有 effect 的 `payloadJson` 均为 JSON 字符串，由 Kotlin 侧反序列化后消费。

---

## 4. State JSON 关键字段

```
records          List<CoreRecord>        当前页记录列表
recordGroups     List<CoreRecordGroup>   按日分组记录
categories       List<CoreCategory>      全部分类（含已归档）
monthlySummaries List<CoreMonthlySummary> 近 N 月收支汇总
currentMonthBreakdown List<CoreCategoryBreakdown> 当月分类占比
budgets          List<CoreBudget>        当前月份预算列表
overviewMetrics  CoreOverviewMetrics?    月度概览指标
settings         CoreSettings            应用设置（currencySymbol）
ui               CoreUiState             界面状态
  ├─ activeTab           String          当前标签页
  ├─ selectedRecordId    Long?           当前选中记录
  └─ errorMessage        String?         错误提示文本
```

### 4.1 `overviewMetrics` 预算口径

| 字段 | 说明 |
|------|------|
| `todayExpense` | 当前查看月份为本月时，表示今日已支出；其他月份为 `null`。 |
| `dailyAvg` | 已发生支出按已过天数摊销后的日均支出；未来月份为 `null`。 |
| `dailyRemaining` | 当月冻结的日消费额度。当前月设置月总预算时，按 `(月总预算 - 本月总支出 + 今日支出) / 含今日剩余天数` 计算，等价于把昨日及以前支出与未来已安排支出从预算中扣除后，平摊到包含今天的剩余天数。因此：**记今天的账不会导致 `dailyRemaining` 变化**，而记未来或补记过去会实时扣减；只要每天满足 `todayExpense <= dailyRemaining`，即可保证月总预算不超支。若今天是月末，则退化为剩余预算。未设置月总预算时，按 `(月结余 + 今日支出) / 含今日剩余天数` 计算。 |

---

## 5. 错误契约

- **绝不抛异常**：Rust 侧所有错误在 `dispatch_intent` 内捕获，不会穿透 FFI 边界。
- **统一写入 state**：失败时 `ui.errorMessage` 被设置为可读错误文本，Kotlin 侧通过解析 `stateJson` 读取并展示。
- **消除方式**：Kotlin 发送 `DismissError` Intent，`errorMessage` 置 `null`。

---

## 6. 序列化约定

| 项目 | Rust（serde） | Kotlin（kotlinx.serialization） |
|------|--------------|-------------------------------|
| 字段命名 | `snake_case` | `camelCase` |
| 枚举 tag | `type`（snake_case 值） | `type`（@SerialName 映射） |
| 空值处理 | `Option<T>` → 省略或 `null` | 可空类型 `T?` |
| 默认值 | 由 `Default` trait 提供 | 由构造参数默认值提供 |

> 双方枚举值通过 `@SerialName` / `#[serde(rename = "...")]` 显式对齐，确保跨语言一致性。
