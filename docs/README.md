# Moni 文档中心

> moni 是一个简单但不普通、精致且量身定做的记账软件，Rust 内核 + Kotlin Android UI。

## 长期文档（核心资产）

| 文档 | 说明 |
|------|------|
| [code-style.md](./long-term/code-style.md) | 代码规范与拆分指引：单文件 500 行上限、Rust/Kotlin 拆分策略、预测性拆分原则 |
| [data-model.md](./long-term/data-model.md) | 核心数据模型设计：Category / Record / RecordType 实体定义、Schema 迁移策略、DTO 转换链路 |
| [category-management.md](./long-term/category-management.md) | 分类管理设计：CRUD 状态机、边界规则（预设/自定义/归档/二级分类）、颜色策略、Intent 交互 |
| [icon-library.md](./long-term/icon-library.md) | 图标库扩展指南：Material Symbols Rounded 集成、命名规范、分组结构、新增流程 |
| [backup-guide.md](./long-term/backup-guide.md) | 全量无损备份/恢复开发规范：ZIP 格式、双版本管理、完整性校验、安全方法、测试要求 |

## 短期文档（沟通备忘）

| 文档 | 说明 |
|------|------|
| [development.md](./short-term/development.md) | 本地开发体检手册：工具链准备、一键 checkAll、IDE 集成、CI 命令、FAQ |

---

> 文档与代码同频共振，核心代码变更后请同步更新关联文档。
