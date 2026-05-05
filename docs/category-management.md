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
│(EditorSheet)│  │(UpdateIntent)│   │(AlertDialog)│
└────┬────┘      └──────┬──────┘    └────┬─────┘ │
     │                  │                │       │
     │ CategoryCreate   │ CategoryUpdate │       │
     ▼                  ▼                ▼       │
┌─────────┐      ┌─────────────┐    ┌──────────┐ │
│ 创建成功 │      │  编辑成功   │    │ 已归档   │─┘
└─────────┘      └─────────────┘    └──────────┘
     │    │              │                │
     │    │              │ 设置/清除 parent│ CategoryUnarchive
     │    │              ▼                ▼
     │    │       ┌─────────────┐  ┌─────────────┐
     │    │       │ 设为子分类  │  │  取消归档   │
     │    │       │  (二级分类) │  │  (恢复活跃) │
     │    │       └──────┬──────┘  └─────────────┘
     │    │              │
     │    │              │ 清除 parent_id
     │    │              ▼
     │    │       ┌─────────────┐
     │    └──────►│ 变回一级    │
     │            │  (一级分类) │
     │            └─────────────┘
     │
     │ 创建时带 parent_id
     ▼
┌─────────┐
│ 创建子分类│
│ (二级)   │
└─────────┘
```

- **UI 层**管理「创建中」「归档确认」等临时弹窗状态；
- **Rust 内核**管理分类的持久化状态（活跃/归档），通过 `CoreUpdate` 回写内存态。

---

## CRUD 链路

### 创建（Create）

1. 用户点击 FAB → 弹出 `CategoryEditorSheet`（ModalBottomSheet）；
2. 填写名称 + 选择图标（点击「选择图标」行 → 弹出 `IconPickerSheet`，支持搜索和分组）；
3. （可选）选择父分类 → 作为二级分类创建；不选则作为一级分类；
4. 发送 `CategoryCreate` Intent → Rust 内核；
5. 校验：名称非空、图标非空、描述长度 ≤ 200、父分类合法（同类型、一级分类、未归档）；
6. 插入数据库，`sort_order = 999`（排在所有预设之后）；
7. 刷新内存态 `AppState.categories`，回传 `CoreUpdate`；
8. UI 展示 Snackbar「分类添加成功」。

### 编辑（Update）

1. 用户点击分类列表项的编辑按钮 → 弹出 `CategoryEditorSheet`；
2. 发送 `CategoryUpdate` Intent → Rust 内核；
3. 校验：名称非空；描述长度 ≤ 200；父分类变更合法；
4. 允许更新 `name` / `description` / `icon_name` / `parent_id`，`category_type` 不可变更；
5. 数据库 `COALESCE` 更新（仅传非 NULL 字段），`parent_id` 通过 `clear_parent_id` 标志控制清空语义；
6. 刷新内存态对应条目，回传 `CoreUpdate`；
7. UI 展示 Snackbar「分类更新成功」。

### 归档（Archive）

1. 用户点击分类的归档按钮 → 弹出确认对话框；
2. 发送 `CategoryArchive` Intent → Rust 内核；
3. 校验：已归档分类拒绝重复归档；
4. 设置 `archived_at = NOW()`；
5. 刷新内存态对应条目的 `archived_at`，回传 `CoreUpdate`；
6. UI 展示 Snackbar「分类已归档」。

### 取消归档（Unarchive）

1. 用户在分类管理页点击右上角菜单 → 「已归档分类」→ 进入二级页面；
2. 点击恢复按钮 → 弹出确认对话框；
3. 发送 `CategoryUnarchive` Intent → Rust 内核；
4. 校验：未归档分类拒绝操作；
5. 设置 `archived_at = NULL`；
6. 刷新内存态，回传 `CoreUpdate`；
7. UI 展示 Snackbar「分类已恢复」。

---

## 边界规则

| 规则 | 说明 | 原因 |
|------|------|------|
| 启动时不自动插入预设分类 | `init.rs` 不再调用 `seed_presets()` | 所有分类一视同仁，用户从零开始建立自身体系；预设分类纳入开发者选项手动重置 |
| 所有分类均可编辑 / 归档 | `is_preset` 不再影响业务逻辑 | 即使是预设分类，用户也可按需修改或归档 |
| 禁止物理删除 | 不提供 `CategoryDelete` Intent | 历史记录关联分类，物理删除会破坏数据完整性；归档是更安全的替代方案 |
| 归档不删除历史记录 | `archived_at` 仅影响新建记录时的可选列表 | 保证历史数据完整可查 |
| 已归档分类在二级页面管理 | 分类管理主页面只展示活跃分类 | 保持主页面整洁，归档操作低频，放入二级页面避免干扰 |
| 描述长度 ≤ 200 | `CATEGORY_DESCRIPTION_MAX_LEN = 200` | 防止过长描述影响列表展示性能 |
| 仅支持单一层级 | 父分类的 `parent_id` 必须为 NULL | 保持数据模型简洁，避免无限嵌套带来的 UI 和统计复杂度 |
| 类型一致性 | 父子分类必须同 `record_type` | 防止支出分类下出现收入子分类，确保统计聚合逻辑正确 |
| 父分类必须活跃 | 已归档分类不能作为父分类 | 避免活跃子分类挂在已归档父分类下的语义矛盾 |
| 不能自引用 | `parent_id != id` | 防止循环依赖 |
| 有子分类的分类不能设为二级 | 通过 `has_children` 检查拦截 | 防止三级分类的出现 |
| 有子分类的一级分类不能直接归档 | 归档前必须先归档所有子分类 | 避免孤儿子分类（无活跃父分类）的语义问题 |

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
// 创建一级分类
CoreIntent.CategoryCreate(
    name = "奶茶",
    description = null,
    categoryType = RecordType.EXPENSE,
    iconName = "restaurant",
    parentId = null
)

// 创建二级分类（parentId 指向一级分类）
CoreIntent.CategoryCreate(
    name = "珍珠奶茶",
    description = null,
    categoryType = RecordType.EXPENSE,
    iconName = "coffee",
    parentId = 1
)

// 更新分类（修改名称和父分类）
CoreIntent.CategoryUpdate(
    id = 13,
    name = "奶茶咖啡",
    description = "日常饮品",
    iconName = "restaurant",
    parentId = 2,
    clearParentId = false
)

// 取消二级分类（变回一级分类）
CoreIntent.CategoryUpdate(
    id = 13,
    parentId = null,
    clearParentId = true
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
