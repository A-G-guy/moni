# Moni 备份开发规范

本文档定义 Moni 全量无损备份/恢复功能的长期开发规范，确保备份格式、数据安全、可维护。当前对用户可见的备份文件默认由 Android 应用层透明加密；Rust 侧 ZIP 仅作为内部临时格式使用。

---

## 0. 应用层透明加密

- 用户导出、应用内自动备份、分享/SAF 写出的备份文件均为 Moni 加密备份，文件内容不是可直接解压的 ZIP。
- Android 层使用固定应用派生密钥做 AES-GCM 包装，加密目标是 Rust 生成的内部 ZIP 临时文件；任意安装的 Moni 均可自动解密。
- 当前阶段不提供用户自定义密码，也不兼容导入旧版明文 ZIP 备份。
- 解密后的明文 ZIP 只能存在于应用 cache 临时文件中，导入/检查结束必须立即清理。

## 1. 内部 ZIP 包格式规范

### 1.1 目录结构

```
manifest.json               # 机器读：元数据 + 完整性指纹
README.md                   # 人类读：自动生成说明
db/moni.db                  # VACUUM INTO 生成的干净数据库副本
settings/preferences.json   # DataStore 解析后的 JSON
attachments/                # 预留目录（当前为空，未来附件落此）
```

### 1.2 关键约定

- ZIP 内全部使用小写路径、`/` 分隔符、UTF-8 编码。
- 文件名中禁止空格与特殊字符。
- 用户可见文件名：`Moni_Backup_yyyyMMdd_HHmmss.zip`（本地时区），但文件内容为 Moni 加密封装而非明文 ZIP。

---

## 2. 双版本管理

备份包使用两个独立版本号，职责分明：

| 版本号 | 职责 | 何时变更 |
|---|---|---|
| `format_version` | ZIP 包顶层结构、manifest 字段定义 | 改目录结构、改字段语义、删字段时 +1 |
| `schema_version` | 数据库表结构（`db/moni.db` 的 DDL） | 发生破坏性 schema 变更时 +1 |

### 2.1 format_version 规则

- **向后兼容**：新增可选字段、新增 ZIP 内文件 → 不改动 `format_version`。
- **必须升级**：删除字段、改字段语义、改 ZIP 顶层目录名 → `format_version` +1。
- **导入器义务**：必须支持 `[1, MAX_SUPPORTED_FORMAT_VERSION]` 全部历史版本。

### 2.2 schema_version 规则

- **非破坏性变更**（新增列 NULLable/DEFAULT、新增表/索引）：沿用 `init_schema` 增量迁移，`schema_version` +1，旧备份直接 `init_schema` 即可兼容。
- **破坏性变更**（删除列、重命名列、改类型、改约束）：必须在 `migrations.rs` 注册迁移函数 `migrate_vN_to_vM`，导入时按 `from..to` 逐级执行。

### 2.3 版本号维护清单

当前值：

- `format_version` = `1`（`exporter.rs` 中 `const FORMAT_VERSION: u32 = 1`）
- `schema_version` = `1`（`schema.rs` 中 `pub const CURRENT_SCHEMA_VERSION: u32 = 1`）
- `MAX_SUPPORTED_FORMAT_VERSION` = `1`（`domain/backup/mod.rs`）

每次 schema 变更 PR 必须同时完成：

1. `schema.rs` 顶部更新 `pub const CURRENT_SCHEMA_VERSION: u32 = N;`。
2. 若为破坏性变更，在 `migrations.rs` 注册迁移函数。
3. 在 `moni-core/tests/backup/fixtures/` 留一份当前版本 ZIP 作为回归基线（如尚无 fixtures 目录，则新建）。

---

## 3. 数据库备份安全方法

**禁止裸拷贝 `.db` 文件**。活跃连接中的数据库可能包含未提交的 WAL 页或正在 fsync 的数据，直接 `fs::copy` 会导致备份损坏。

### 3.1 标准流程

1. `VACUUM INTO ?1` → 生成事务一致的干净副本（无碎片、无 WAL/SHM 残留）。
2. 流式读取副本 → SHA-256 → DEFLATE level 9 → ZIP entry。
3. 完成后立即删除临时副本。

### 3.2 代码审查红线

任何 PR 中出现 `fs::copy("moni.db", ...)` 或等效逻辑，**必须拒绝合并**。

---

## 4. 完整性校验

### 4.1 文件级校验

每个内部文件在写入 ZIP 时流式计算 SHA-256，指纹写入 `manifest.files[]`。

### 4.2 Manifest 自身校验

- 计算 `manifest_sha256` 时，先将 `manifest_sha256` 字段设为空字符串，再对整个结构规范化序列化后计算 SHA-256。
- 导入时先执行 `verify_manifest_integrity`，失败即报 `BackupCorrupted`。

### 4.3 导入校验流程

1. 读取 `manifest.json` → 解析。
2. `manifest_sha256` 自检。
3. `format_version` ≤ `MAX_SUPPORTED_FORMAT_VERSION`。
4. 逐文件 SHA-256 流式校验。
   - `manifest.json` 自身指纹不参与校验（`manifest skip` 分支：遍历 `manifest.files` 时遇到 `path == "manifest.json"` 直接 `continue`）。
5. 任一环节失败 → 立即终止，报 `BackupCorrupted`。

---

## 5. 导入恢复安全机制

### 5.1 恢复前快照

- 数据库：当前 `moni.db` 复制到 `{db_dir}/.pre_restore_{timestamp}.db`。
- DataStore 设置：dump JSON 到 `.pre_restore_{timestamp}.settings.json`。

### 5.2 原子替换

- 解压后的新数据库通过 `fs::rename(tmp_path, db_path)` 原子替换旧文件。
- 替换前关闭主连接，释放文件句柄。

### 5.3 行数校验（row mismatch）

解压并迁移完成后、原子替换之前，导入器会打开临时数据库，校验 `records` 与 `categories` 表的行数是否与 `manifest.stats` 声明一致。

- 若 `actual_records != expected_record_count` 或 `actual_categories != expected_category_count`，立即返回 `BackupRestoreFailed { stage: "validation", reason: "行数不匹配: ..." }`。
- 临时数据库文件在返回前被清理，不会执行原子替换，确保目标数据库不受影响。

### 5.4 失败回滚

恢复流程在 `lib.rs` 层面统一包裹回滚逻辑：

1. **关闭连接**：恢复前先将主连接切换为内存连接，释放 `db_path` 的文件句柄。
2. **创建快照**：将当前 `db_path` 复制到 `.pre_restore_{timestamp}.db`。
3. **执行恢复**：调用 `importer::backup_restore(..., snapshot_path)`。
4. **恢复成功**：重新打开数据库连接 → 刷新内存状态 → 删除快照 → 清理临时文件。
5. **恢复失败**：从快照复制回 `db_path` → 重新打开连接（失败则回退到内存连接）→ 删除快照 → 向上传播原始错误。

回滚后数据状态与恢复前完全一致。

---

## 6. DataStore 设置备份

### 6.1 导出格式

```json
{
  "schema": 1,
  "currency_symbol": "¥",
  "theme_mode": "system",
  "dynamic_color": false,
  "seed_color": 4284612842
}
```

### 6.2 兼容性

- `restoreFromJson` 先检查 `"schema"` 字段，版本不匹配时静默跳过（保留默认设置，不抛异常）。
- 新增设置项时：导出端写入新字段，导入端若旧备份无此字段则使用默认值。

---

## 7. 测试要求

### 7.1 Rust 测试

| 类型 | 文件 | 覆盖内容 |
|---|---|---|
| 单元测试 | `tests/backup_manifest_test.rs` | manifest 序列化、sha256 计算、完整性校验、格式版本校验、ZIP 内读取/缺失条目 |
| e2e 测试 | `tests/backup_e2e_test.rs` | 导出→导入→数据一致性、损坏 ZIP 检测、篡改检测（文件指纹不匹配） |
| inspect 测试 | `tests/backup_inspect_test.rs` | `backup_inspect` 正常读取、缺失文件错误、非 ZIP 文件错误、进度监听器回调 |
| 行数校验测试 | `tests/backup_restore_row_mismatch_test.rs` | manifest.stats 与实际数据库行数不匹配时的 `db_validation` 失败路径 |

### 7.2 回归基线

每次 `format_version` 或 `schema_version` 升级后，在 `tests/fixtures/` 保留一份该版本的标准 ZIP，供未来导入兼容性测试使用。

---

## 8. 常见问题

**Q: 为什么不使用 zstd 而是 ZIP DEFLATE？**
> SQLite 文本数据 DEFLATE level 9 已能达到 75-85% 压缩率，zstd 的提升有限但会引入 NDK 依赖，权衡后选择 ZIP。

**Q: 导入后为什么要重启应用？**
> 数据库文件被替换后，所有持有旧 Connection 的 ViewModel 状态失效。重启是最安全、最彻底的清理方式。

**Q: 10,000 条记录以上的性能如何？**
> VACUUM INTO 的时间复杂度为 O(n)，10 万条记录通常在 1 秒内完成。ZIP 压缩在 Rust 端使用 `spawn_blocking`，不阻塞主线程。
