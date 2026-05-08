# Moni

<p align="center">
  <img src="https://img.shields.io/badge/Rust-1.93.0-DEA584?logo=rust&logoColor=white" alt="Rust" />
  <img src="https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Android-API_28+-3DDC84?logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/License-Apache--2.0-blue.svg" alt="License" />
</p>

<p align="center">
  简体中文 | <a href="./README.md">English</a>
</p>

> **简单但不普通、精致且量身定做的记账软件。**
>
> Moni = Money + Mini，以最小的心智负担，获得最佳的记账体验。

---

## 技术亮点

| 特性 | 说明 |
|------|------|
| **Rust 重内核** | 跨平台业务逻辑与全局状态机，编译期内存安全 |
| **Kotlin 纯 UI** | 无状态渲染层，仅负责界面与系统能力桥接 |
| **UniFFI 绑定** | 自动生成 JNI 绑定，JSON 序列化通信 |
| **SQLite 持久化** | 文件数据库 + 内存降级兜底 |
| **模块化设计** | 功能可开关，边界清晰，核心数据安全优先 |

## 架构分层

```
+------------------+
|  Kotlin UI 层    |  Compose 界面 + ViewModel（无状态）
+------------------+
         |  StateFlow / Effect 回调
+------------------+
|   JNI / Bridge   |  UniFFI + JNA 自动生成绑定
+------------------+
         |  JSON 字符串（Intent / State / Effects）
+------------------+
|   Rust Core 层   |  状态机 + 业务逻辑 + SQLite
+------------------+
```

- **Kotlin UI**：接收 State 渲染界面，将用户操作封装为 Intent 下发
- **JNI/Bridge**：UniFFI 生成 FFI 绑定，JNA 动态库加载
- **Rust Core**：集中式状态机、Intent 分发、数据库读写、副作用生成

## 快速开始

### 环境要求

| 工具 | 版本 |
|------|------|
| JDK | 17+ |
| Android SDK | API 36 + NDK 29.0.14206865 |
| Rust | stable（由 `rust-toolchain.toml` 自动激活） |

```bash
# 安装 Android 目标
rustup target add aarch64-linux-android x86_64-linux-android

# 启用 git hooks
git config core.hooksPath .githooks

# 一键代码质量检查
./gradlew checkAll

# 运行 Rust 测试
cargo test --workspace

# 构建 Release APK
./gradlew :app:assembleRelease
```

## 项目结构

```
moni/
├── android/app/          # Kotlin Android 应用（Compose UI）
├── moni-core/            # Rust 核心业务逻辑（状态机 + SQLite）
├── moni-contracts/       # Rust 接口契约与 DTO
├── docs/                 # 项目文档中心
│   ├── long-term/        # 架构、数据模型、代码规范等核心资产
│   └── short-term/       # 开发指南、FAQ 等沟通备忘
├── config/               # Detekt 等工具配置
├── scripts/              # 构建辅助脚本
└── .githooks/            # 预提交质量门控
```

## 文档导航

| 文档 | 说明 |
|------|------|
| [开发指南](docs/short-term/development.md) | 环境准备、IDE 配置、一键体检 |
| [代码规范](docs/long-term/code-style.md) | 拆分策略、文件行数上限、命名约定 |
| [数据模型](docs/long-term/data-model.md) | Category / Record / RecordType 实体设计 |
| [分类管理](docs/long-term/category-management.md) | CRUD 状态机、边界规则、颜色策略 |
| [架构设计](docs/long-term/architecture.md) | 分层职责、数据流向、FFI 通信方式 |
| [备份恢复](docs/long-term/backup-guide.md) | ZIP 格式、双版本管理、完整性校验 |

更多文档请访问 [docs/README.md](docs/README.md)。

## 开源协议

本项目采用 [Apache-2.0](LICENSE) 协议开源。
