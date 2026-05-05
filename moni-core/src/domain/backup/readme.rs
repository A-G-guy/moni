/// 生成人类可读的 README.md 内容。
#[must_use]
pub fn generate_readme(
    app_version: &str,
    created_at: &str,
    record_count: u64,
    category_count: u64,
    settings_count: u64,
) -> String {
    format!(
        r#"# Moni 备份文件

此文件由 Moni 应用自动生成，包含您的完整记账数据与设置。

## 基本信息

- **应用版本**: {app_version}
- **备份时间**: {created_at}
- **备份格式版本**: 1

## 包含数据

- **记账记录**: {record_count} 条
- **分类**: {category_count} 个
- **设置项**: {settings_count} 项

## 文件说明

- `manifest.json`: 备份元数据与完整性校验指纹
- `db/moni.db`: SQLite 数据库（包含所有记账记录与分类）
- `settings/preferences.json`: 应用偏好设置（主题、货币符号等）
- `attachments/`: 附件目录（当前版本预留）

## 如何导入

1. 在 Moni 应用的「设置」→「导入数据」中选择此 ZIP 文件
2. 确认导入后应用将自动重启
3. 请勿手动修改或重命名 ZIP 内任何文件，否则导入校验会失败

---
*由 Moni 自动生成，请勿手动编辑。*
"#
    )
}
