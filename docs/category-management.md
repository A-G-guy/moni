# 分类管理设计

> 文档只解释设计决策（Why），不解释代码实现（What）。代码即文档。

## 状态机

```
                    ┌─────────────┐
                    │   初始态    │
                    │  (未加载)   │
                    └──────┬──────┘
                           │ CategoryList Intent
                           ▼
                    ┌─────────────┐
     ┌─────────────│   活跃态    │◄────────────────┐
     │             │  (正常展示) │                 │
     │             └──────┬──────┘                 │
     │                    │                        │
     │   ┌────────────────┼────────────────┐      │
     │   │                │                │      │
     ▼   ▼                ▼                ▼      │
┌─────────┐      ┌─────────────┐    ┌──────────┐ │
│ 创建中  │      │   编辑中    │    │ 归档确认 │ │
│(AddDialog)│   │(UpdateIntent)│   │(AlertDialog)│
└────┬────┘      └──────┬──────┘    └────┬─────┘ │
     │                  │                │       │
     │ CategoryCreate   │ CategoryUpdate │       │
     ▼                  ▼                ▼       │
┌─────────┐      ┌─────────────┐    ┌──────────┐ │
│ 创建成功 │      │  编辑成功   │    │ 已归档   │─┘
└─────────┘      └─────────────┘    └──────────┘
                                         │
                                         │ CategoryUnarchive
                                         ▼
                                  ┌─────────────┐
                                  │  取消归档   │
                                  │  (恢复活跃) │
                                  └─────────────┘
```

- **UI 层**管理「创建中」「归档确认」等临时弹窗状态；
- **Rust 内核**管理分类的持久化状态（活跃/归档），通过 `CoreUpdate` 回写内存态。

---

## CRUD 链路

### 创建（Create）

1. 用户点击 FAB → 弹出 `AddCategoryDialog`；
2. 填写名称 + 选择图标（`AvailableCategoryIcons` 列表）；
3. 发送 `CategoryCreate` Intent → Rust 内核；
4. 校验：名称非空、图标非空、描述长度 ≤ 200；
5. 插入数据库，`sort_order = 999`（排在所有预设之后）；
6. 刷新内存态 `AppState.categories`，回传 `CoreUpdate`；
7. UI 展示 Snackbar「分类添加成功」。

### 编辑（Update）

1. 用户长按自定义分类（或点击编辑按钮）→ 进入编辑态；
2. 发送 `CategoryUpdate` Intent → Rust 内核；
3. 校验：**预设分类拒绝编辑**；名称非空；描述长度 ≤ 200；
4. 仅允许更新 `name` / `description` / `icon_name`，`category_type` 不可变更；
5. 数据库 `COALESCE` 更新（仅传非 NULL 字段）；
6. 刷新内存态对应条目，回传 `CoreUpdate`；
7. UI 展示 Snackbar「分类更新成功」。

### 归档（Archive）

1. 用户点击自定义分类的归档按钮 → 弹出确认对话框；
2. 发送 `CategoryArchive` Intent → Rust 内核；
3. 校验：**预设分类不可归档**；已归档分类拒绝重复归档；
4. 设置 `archived_at = NOW()`；
5. 刷新内存态对应条目的 `archived_at`，回传 `CoreUpdate`；
6. UI 展示 Snackbar「分类已归档」。

### 取消归档（Unarchive）

1. 用户进入「已归档」列表（或管理界面）→ 点击恢复；
2. 发送 `CategoryUnarchive` Intent → Rust 内核；
3. 校验：未归档分类拒绝操作；
4. 设置 `archived_at = NULL`；
5. 刷新内存态，回传 `CoreUpdate`；
6. UI 展示 Snackbar「分类已恢复」。

---

## 边界规则

| 规则 | 说明 | 原因 |
|------|------|------|
| 预设分类不可编辑 `name` / `type` | `is_preset = true` 时 `CategoryUpdate` 拒绝 | 保证预设分类语义稳定，避免用户破坏内置分类体系 |
| 预设分类不可归档 | `is_preset = true` 时 `CategoryArchive` 拒绝 | 核心分类（餐饮、交通等）必须始终可用 |
| 自定义分类可编辑 `name` / `description` / `icon` | `is_preset = false` 时允许 | 给予用户充分的自定义空间 |
| 禁止物理删除 | 不提供 `CategoryDelete` Intent | 历史记录关联分类，物理删除会破坏数据完整性；归档是更安全的替代方案 |
| 归档不删除历史记录 | `archived_at` 仅影响新建记录时的可选列表 | 保证历史数据完整可查 |
| 描述长度 ≤ 200 | `CATEGORY_DESCRIPTION_MAX_LEN = 200` | 防止过长描述影响列表展示性能 |

---

## 颜色策略

分类颜色**不由用户自定义**，统一按 `record_type` 分色：

- **支出（Expense）**：红色系 — 浅色 `#B33A3A` / 暗色 `#E89999`
- **收入（Income）**：绿色系 — 浅色 `#3A8A6E` / 暗色 `#7AC9A8`

**为什么去色？**
- 减少用户心智负担：不需要为每个分类思考配色；
- 保持视觉一致性：所有支出分类统一红色，所有收入分类统一绿色，一眼可辨；
- 降低实现复杂度：无需颜色选择器、无需存储颜色字段、无需处理暗色适配。

---

## 与 Rust 内核的 Intent 交互

### Kotlin → Rust

```kotlin
// 创建分类
CoreIntent.CategoryCreate(
    name = "奶茶",
    description = null,
    categoryType = RecordType.EXPENSE,
    iconName = "restaurant"
)

// 更新分类
CoreIntent.CategoryUpdate(
    id = 13,
    name = "奶茶咖啡",
    description = "日常饮品",
    iconName = "restaurant"
)

// 归档 / 取消归档
CoreIntent.CategoryArchive(id = 13)
CoreIntent.CategoryUnarchive(id = 13)

// 加载列表
CoreIntent.CategoryList
```

### Rust → Kotlin

Rust 内核处理 Intent 后返回 `CoreUpdate`：

```json
{
  "stateJson": "{...AppState（含更新后的 categories 列表）...}",
  "effects": [
    { "kind": "show_snackbar", "payloadJson": "{\"message\":\"分类已归档\"}" }
  ]
}
```

UI 层解析 `stateJson` 为 `CoreAppState`，刷新分类列表；同时执行 `show_snackbar` effect 展示反馈。

### 错误处理

| 错误类型 | 场景 | UI 反馈 |
|----------|------|---------|
| `CategoryNotFound` | 操作已不存在的分类 | Snackbar「分类不存在」 |
| `CategoryAlreadyArchived` | 重复归档 | Snackbar「分类已被归档」 |
| `CategoryNotArchived` | 对未归档分类取消归档 | Snackbar「分类未归档」 |
| `InvalidInput` | 名称空 / 描述过长 / 图标空 | Snackbar 显示具体错误信息 |
