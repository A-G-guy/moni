# 本地开发体检手册

本文档梳理 Moni 项目的本地开发与质量保障流程。Moni 采用 Rust 内核 + Kotlin Android UI 的混合栈，本地体检也分两侧。

## 1. 一次性环境准备

### 1.1 工具链

| 工具 | 版本要求 | 备注 |
| --- | --- | --- |
| JDK | 17+ | Gradle 编译用，AGP 9.x 要求 |
| Android SDK | API 36 + NDK 29.0.14206865 | 见 `android/app/build.gradle.kts` |
| Rust toolchain | stable（项目自动激活） | 由 `rust-toolchain.toml` 控制 |
| `cargo`、`rustfmt`、`clippy` | 由 toolchain 自带 | — |

Android 目标三元组（cargo 编译 cdylib）：
- `aarch64-linux-android`
- `x86_64-linux-android`

执行 `rustup target add aarch64-linux-android x86_64-linux-android` 安装。

### 1.2 启用项目 git hooks

```bash
git config core.hooksPath .githooks
```

预提交钩子会跑：
1. 单文件 500 行硬上限检查
2. 对暂存的 `.rs` 文件跑 `cargo fmt --check` 与 `cargo clippy`
3. 对暂存的 `.kt`/`.kts` 文件跑 `./gradlew detekt`

紧急绕过：`git commit --no-verify`（仅用于真正紧急情况，提交后必须立刻补救）。

## 2. 一键体检

```bash
./gradlew checkAll       # 同时检查 Kotlin（detekt）+ Rust（fmt --check + clippy）
cargo test --workspace   # Rust 单元 / 集成测试
./gradlew test           # Kotlin 单元测试
```

如需 Android 设备插桩测试：
```bash
./gradlew connectedAndroidTest
```

## 3. 单工具运行

### 3.1 Kotlin 侧

| 命令 | 用途 |
| --- | --- |
| `./gradlew detekt` | 跑 detekt 静态分析 + ktlint 包装规则（auto-correct 已开启） |
| `./gradlew detekt --auto-correct` | 同上，但显式声明（默认行为） |
| `./gradlew detektGenerateConfig` | 生成默认 detekt 配置（首次配置时） |

### 3.2 Rust 侧

| 命令 | 用途 |
| --- | --- |
| `cargo fmt --all` | 自动格式化所有 crate |
| `cargo fmt --all -- --check` | 仅检查格式，不写文件（CI 与 hook 用） |
| `cargo clippy --workspace --all-targets -- -D warnings` | 所有 crate + 所有 target（含测试）跑 clippy，警告即错误 |
| `cargo ci` | `.cargo/config.toml` 中的别名，等同上一行 |
| `cargo fmtck` | `.cargo/config.toml` 中的别名，等同 fmt --check |

## 4. IDE 集成（实时反馈）

### 4.1 IntelliJ IDEA / Android Studio

- **detekt 插件**：`Plugins → Marketplace → Detekt`（保存即提示）
  - 配置：`Settings → Tools → Detekt`，指向 `config/detekt/detekt.yml`
- **ktlint 插件**：可选，detekt-rules-ktlint-wrapper 已涵盖 ktlint 规则。如果一定要装，关闭 detekt 的 formatting 模块避免重复。

### 4.2 VSCode

- **rust-analyzer**：跑 `cargo check` / `clippy` 实时反馈
- **Even Better TOML**：`Cargo.toml` 高亮
- **Kotlin (Fwcd) + Detekt CLI**：可选

## 5. CI 友好命令清单

未来若引入 GitHub Actions / CI，推荐 job：

```yaml
- run: ./gradlew detekt --no-configuration-cache
- run: ./gradlew test --no-configuration-cache
- run: cargo fmt --all -- --check
- run: cargo clippy --workspace --all-targets -- -D warnings
- run: cargo test --workspace
- run: ./gradlew :app:assembleRelease
```

## 6. 常见问题排查

### 6.1 detekt 报错过多
跑 `./gradlew detektBaseline` 生成 `detekt-baseline.xml`，提交到 git。之后只对新代码报错。但项目的目标是清零，不推荐建立 baseline，除非引入大量历史代码。

### 6.2 clippy `pedantic` 太严
在根 `Cargo.toml` 的 `[workspace.lints.clippy]` 中针对性 `xxx = "allow"`。不建议整组关闭。

### 6.3 cargo fmt 自动改了大量文件
项目使用 `rustfmt.toml` 中的设置（max_width=100、edition=2024）。如果 IDE 用了不同设置，请把 IDE 配置同步成项目设置。

### 6.4 pre-commit hook 太慢
hook 增量调用 detekt 与 cargo，但 detekt 启动 JVM 仍较慢。可以：
- 临时禁用：`git commit --no-verify`（紧急情况）
- 永久优化：考虑在 hook 中改为只跑 `--changed-files` 模式（detekt 2.x 支持）

### 6.5 worktree 内 ./gradlew 失败
新 worktree 首次跑 Gradle 会下载依赖，请耐心。一旦 `.gradle/` 缓存建立，后续会快得多。

## 7. 体检流水线一览

```
┌─────────────────┐  ┌──────────────────┐  ┌─────────────────────┐
│  IDE 实时反馈   │→│  pre-commit hook │→│  ./gradlew checkAll │
│  detekt/RA      │  │  增量 fmt/lint   │  │  全量 detekt+clippy │
└─────────────────┘  └──────────────────┘  └─────────────────────┘
   写代码时           提交时              发版前 / CI 触发
```

---

## 附：本文档背后的工具版本

- detekt 2.0.0-alpha.3（`dev.detekt`，2026-04-24 发布）
- ktlint-wrapper 同版本
- Rust workspace lints（Rust 1.74+ 特性）
- Gradle 9.4.1 + AGP 9.2.0
