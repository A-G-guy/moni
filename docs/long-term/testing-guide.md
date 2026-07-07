---
last_modified: "2026-07-07 18:51"
---

# 测试规范

## 为什么写测试

- **减少人工测试依赖**：自动化测试覆盖核心路径，避免每次改动后手动回归。
- **保证重构安全**：Rust 内核与 Kotlin UI 层频繁重构，测试是唯一的回归网。
- **文档化边界行为**：测试用例即契约，描述合法输入、非法输入与预期输出。

## Rust 测试规范

### 目录与隔离

- 集成测试统一放在 `moni-core/tests/` 目录，**严禁与源码平铺**。
- 每个测试文件对应一个源码模块，命名格式为 `<module>_test.rs`（如 `dispatch_category_test.rs` 对应 `dispatch/category.rs`）。

### 共享 Helper

- `tests/common/mod.rs` 提供两个初始化函数：
  - `setup_core()` — 创建内存数据库的 `MoniCore` 实例。
  - `setup_core_with_presets()` — 在 `setup_core()` 基础上预置分类数据。
- 所有集成测试顶部声明 `mod common;` 并调用上述 helper。

### 运行命令

```bash
# 运行全部测试
cargo test --workspace

# 查看覆盖率摘要
cargo llvm-cov --workspace --summary-only
```

## Kotlin 测试规范

### 目录结构

- JVM 单元测试放在 `android/app/src/test/java/com/agguy/moni/...` 下，按包名镜像源码目录。
- 纯 JVM 范围，**避开 `@Composable`、`Context`、JNI 调用**；这些留给仪器化测试。

### Gradle 配置

- `build.gradle.kts` 中已开启 `testOptions.unitTests.isReturnDefaultValues = true`，确保无 Android 依赖的纯 JVM 测试可直接运行。

### 运行命令

```bash
./gradlew :app:testDebugUnitTest --no-daemon
```

## 测试命名与断言规范

### 命名

- **Rust**：使用反引号包裹的描述性名称，如 `fn test_category_create_empty_name()`。
- **Kotlin**：使用 backtick 命名，如 `` fun `default theme settings uses system mode`() ``。

### 断言

- **禁止无意义占位**：如 `assert!(result.is_err())` 仅验证“出错”，不验证“错在哪里”。
- **错误路径必须断言具体文案或类型**：
  - Rust 侧通过 `state["ui"]["errorMessage"]` 断言业务错误文案。
  - 或通过 `match` / `assert_eq!` 断言具体 `CoreError` 变体及其字段。
- **成功路径必须断言数据形态**：不仅验证无错，还要验证返回数据的结构、字段值、数量。

## E2E 策略

### Rust 侧

- 通过 `MoniCore::dispatch` 发送 JSON intent，结合 `snapshot_json()` 获取完整状态快照做集成测试。
- 备份/恢复等复杂流程使用临时目录（`std::env::temp_dir()`），测试结束后由系统清理。

### Android 侧

- 仪器化测试通过 `./gradlew connectedAndroidTest` 在真机/模拟器上运行。
- 覆盖 Compose UI 交互、Context 依赖、主题切换等无法纯 JVM 测试的场景。

## 覆盖率目标

- 当前行覆盖 **91.67%**，区域覆盖 **86.52%**。
- 新功能增改必须同步补充测试，持续推高覆盖率，禁止提交“零测试”业务代码。
