# 贡献指南

感谢你对 Moni 项目的关注！在提交贡献前，请阅读以下规范。

## 代码规范

- 函数 < 50 行，类 < 300 行，源文件 < 500 行
- 使用卫语句（Guard Clauses）尽早返回，杜绝 `if-else` 嵌套超 3 层
- 严禁将数据库实体直出前端，必须经 DTO 转换
- 面向接口编程，业务逻辑中禁止硬编码实例化复杂服务对象

完整规范见 [docs/long-term/code-style.md](docs/long-term/code-style.md)。

## 提交信息规范

遵循 [Conventional Commits](https://www.conventionalcommits.org/)，`<type>` 仅限：

`feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

示例：

```
feat: 新增预算超支预警提示

- 在 BudgetOverview 中添加超支状态计算
- 超支时自动切换进度条颜色为警告色
```

## 质量门控

提交前会自动运行以下检查：

1. 单文件 500 行硬上限
2. `cargo fmt --check` + `cargo clippy`
3. `./gradlew detekt`

手动触发全量检查：

```bash
./gradlew checkAll       # Kotlin + Rust
cargo test --workspace   # Rust 测试
```

紧急绕过：`git commit --no-verify`（仅用于真正紧急情况，提交后必须立刻补救）。

## 不接收的 PR 类型

- 未经讨论的重大架构变更
- 引入新的外部依赖（除非有充分理由）
- 零测试的核心逻辑修改

## 报告问题

请提供：复现步骤、预期行为、实际行为、环境信息（Android 版本、Rust 版本）。
