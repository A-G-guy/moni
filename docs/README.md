# Moni 文档中心

> moni 是一个简单但不普通、精致且量身定做的记账软件，Rust 内核 + Kotlin Android UI。

[← 返回项目首页](../README.md)

---

## 长期文档（核心资产）

| 文档 | 说明 |
|------|------|
| [architecture.md](./long-term/architecture.md) | 整体架构设计：分层职责、数据流向、FFI 通信方式、内存/文件数据库权衡 |
| [code-style.md](./long-term/code-style.md) | 代码规范与拆分指引：单文件 500 行上限、Rust/Kotlin 拆分策略、预测性拆分原则 |
| [core-contracts.md](./long-term/core-contracts.md) | 核心接口契约：Intent / State / Effect 定义、DTO 转换链路、序列化规范 |
| [data-model.md](./long-term/data-model.md) | 核心数据模型设计：Category / Record / RecordType 实体定义、Schema 迁移策略 |
| [category-management.md](./long-term/category-management.md) | 分类管理设计：CRUD 状态机、边界规则（预设/自定义/归档/二级分类）、颜色策略、Intent 交互 |
| [icon-library.md](./long-term/icon-library.md) | 图标库扩展指南：Material Symbols Rounded 集成、命名规范、分组结构、新增流程 |
| [backup-guide.md](./long-term/backup-guide.md) | 全量无损备份/恢复开发规范：ZIP 格式、双版本管理、完整性校验、安全方法、测试要求 |
| [testing-guide.md](./long-term/testing-guide.md) | 测试策略与指南：测试目录结构、映射规则、核心逻辑覆盖要求 |
| [ai-provider-api.md](./long-term/ai-provider-api.md) | AI Provider 预设、OpenAI/Gemini API 格式、记账 JSON Output 与安全边界 |

## 短期文档（沟通备忘）

| 文档 | 说明 |
|------|------|
| [development.md](./short-term/development.md) | 本地开发体检手册：工具链准备、一键 checkAll、IDE 集成、CI 命令、FAQ |
| [quickstart.md](./short-term/quickstart.md) | 快速入门：首次 clone 后的最小可用步骤 |
| [changelog.md](./short-term/changelog.md) | 版本变更记录 |
| [faq.md](./short-term/faq.md) | 常见问题解答 |

---

> 文档与代码同频共振，核心代码变更后请同步更新关联文档。
