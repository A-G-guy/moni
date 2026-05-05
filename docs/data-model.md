# 数据模型设计

> 文档只解释设计决策（Why），不解释代码实现（What）。代码即文档。

## 核心实体

### `RecordType` — 收支类型

```rust
pub enum RecordType {
    Income,
    Expense,
}
```

- 仅两个变体，使用 `snake_case` 序列化（`"income"` / `"expense"`），与数据库 `CHECK` 约束和 Kotlin `RecordType` 枚举保持一致。
- 作为分类和记录共用的类型标记，避免重复定义。

### `Record` — 记账记录

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | `i64` | PK, AUTOINCREMENT | |
| `amount_cents` | `i64` | `CHECK(amount_cents > 0)` | 金额单位：分（1元 = 100分），避免浮点精度问题 |
| `record_type` | `RecordType` | `CHECK` | 与 `category_type` 可不一致（允许分类类型变更后历史记录保留原样） |
| `category_id` | `i64` | FK → `categories(id)`, `ON DELETE RESTRICT` | 禁止级联删除，防止误删有历史记录的分类 |
| `note` | `String` | `DEFAULT ''` | 备注，单行文本 |
| `created_at` | `i64` | Unix 秒 | 用户可指定（回溯记账），非自动生成 |
| `updated_at` | `i64` | Unix 秒 | 最后修改时间 |

**设计权衡**：
- 金额用 `分` 而非 `元×100` 浮点转换：彻底杜绝浮点精度问题，前端展示层统一做除法转换。
- `created_at` 允许用户指定：支持回溯记账（如补记昨天的消费），而非强制 `NOW()`。
- `ON DELETE RESTRICT`：分类有历史记录时禁止物理删除，引导用户走「归档」流程。

### `Category` — 分类

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | `i64` | PK, AUTOINCREMENT | |
| `name` | `String` | NOT NULL | 分类名称 |
| `description` | `Option<String>` | NULL | 选填，语义补充说明 |
| `category_type` | `RecordType` | `CHECK` | 收入 / 支出 |
| `icon_name` | `String` | NOT NULL, DEFAULT `'help'` | 图标标识，映射到 `res/drawable/ic_{name}.xml` |
| `color_hex` | `String` | NOT NULL, DEFAULT `'#808080'` | **已废弃**：颜色统一由 `record_type` 决定，保留字段兼容旧数据 |
| `sort_order` | `i32` | NOT NULL, DEFAULT `0` | 排序权重，预设分类按业务语义排序，自定义分类固定 `999` |
| `parent_id` | `Option<i64>` | FK → `categories(id)`, NULL | 父分类 ID，NULL 表示一级分类，非 NULL 表示二级分类 |
| `is_preset` | `bool` | NOT NULL, DEFAULT `0` | 预设分类不可编辑 name/type、不可归档 |
| `archived_at` | `Option<i64>` | NULL | **NULL = 活跃**，非 NULL = 已归档 |
| `created_at` | `i64` | Unix 秒 | |
| `updated_at` | `i64` | Unix 秒 | |

**设计权衡**：
- 去色（移除用户自定义颜色）：颜色统一按 `record_type` 分色（支出红 / 收入绿），减少用户心智负担，保持视觉一致性。
- `description` 选填：为后续扩展（如分类备注、预算说明）预留语义空间，不强制填写避免增加录入成本。
- `archived_at` 软删除替代物理删除：归档分类不可再被新建记录选中，但历史记录正常展示，保证数据完整性。
- `parent_id` 单表自引用：仅支持单一层级（不允许三级分类），父分类必须是一级分类且与子分类同类型，保持实现简洁。

---

## Schema 迁移策略

采用**幂等 ALTER TABLE**，确保新旧版本数据库均可安全升级：

```rust
pub fn init_schema(conn: &Connection) -> Result<(), rusqlite::Error> {
    conn.execute_batch(SCHEMA_SQL)?;

    // 2026-05-04：添加 description 列
    let has_description = /* pragma_table_info 检测 */;
    if has_description == 0 {
        conn.execute("ALTER TABLE categories ADD COLUMN description TEXT NULL", [])?;
    }

    // 2026-05-04：添加 archived_at 列
    let has_archived_at = /* pragma_table_info 检测 */;
    if has_archived_at == 0 {
        conn.execute("ALTER TABLE categories ADD COLUMN archived_at INTEGER NULL", [])?;
    }

    // 2026-05-05：添加 parent_id 列
    let has_parent_id = /* pragma_table_info 检测 */;
    if has_parent_id == 0 {
        conn.execute(
            "ALTER TABLE categories ADD COLUMN parent_id INTEGER NULL REFERENCES categories(id) ON DELETE RESTRICT",
            [],
        )?;
    }

    Ok(())
}
```

**原则**：
- 新表用 `CREATE TABLE IF NOT EXISTS`，新索引用 `CREATE INDEX IF NOT EXISTS`。
- 新增列先通过 `pragma_table_info` 检测存在性，避免重复 `ALTER` 报错。
- 不删除旧列（如 `color_hex`），保持向后兼容。

---

## DTO 转换链路

### 为什么需要 DTO？

数据库实体（`Category`、`Record`）包含内部时间戳（`created_at`、`updated_at`）和实现细节，不应直出前端。DTO 层负责：
- **脱敏**：剥离内部字段；
- **展开**：`RecordDto` 携带 `category_name`，避免前端二次查询；
- **分组**：`RecordDayGroup` 将记录按日期聚合并计算日汇总，前端直接渲染。

### 转换路径

```
Category (contracts) ──→ CategoryDto (moni-core/dto)
                              │
                              └──→ JSON ──→ CoreCategory (Kotlin)

Record (contracts) ────→ RecordDto (moni-core/dto)
                              │
                              └──→ JSON ──→ CoreRecord (Kotlin)
```

### 关键 DTO

- `CategoryDto`：前端消费用，不含 `created_at` / `updated_at`。
- `RecordDto`：展开关联分类名称，前端列表/详情直接消费。
- `RecordDayGroup`：按日期分组，含 `income_cents` / `expense_cents` 日汇总。

### 分组逻辑

`group_records_by_date` 使用 `BTreeMap<String, Vec<RecordDto>>` 按键（`YYYY-MM-DD`）自然排序，再 `rev()` 倒序输出，保证列表按日期从新到旧排列。

---

## 变更历史

| 日期 | 变更 | 影响 |
|------|------|------|
| 2026-05-04 | 去色：移除用户自定义颜色，统一按 `record_type` 分色 | `color_hex` 字段废弃但保留兼容 |
| 2026-05-04 | 加描述：新增 `description` 列 | 分类可添加语义说明 |
| 2026-05-04 | 加归档：新增 `archived_at` 列 | 支持软删除（归档）语义 |
| 2026-05-05 | 加层级：新增 `parent_id` 列 | 支持二级分类（子分类）语义 |
