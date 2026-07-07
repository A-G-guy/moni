---
last_modified: "2026-07-07 18:51"
---

# 快速开始

> 目标：10 分钟内编译并运行项目。

## 1. 克隆仓库

```bash
git clone <仓库地址>
cd moni
```

## 2. 安装工具链

- JDK 17+（Gradle / AGP 9.x 要求）
- Android SDK（API 36）+ NDK `29.0.14206865`
- Rust stable（由 `rust-toolchain.toml` 自动激活）
- Android 目标三元组：`rustup target add aarch64-linux-android x86_64-linux-android`

## 3. 编译 Rust 内核

```bash
cargo build -p moni-core --release
```

## 4. 编译 Android App

```bash
./gradlew :app:assembleDebug
```

Gradle 会自动生成 UniFFI 绑定并同步 JNI 动态库。

## 5. 运行测试

```bash
cargo test --workspace
./gradlew :app:testDebugUnitTest
```

## 6. 安装到设备

连接真机或启动模拟器后：

```bash
./gradlew :app:installDebug
```

## 7. 启用 git hooks（可选）

```bash
git config core.hooksPath .githooks
```

hook 会在提交前增量检查格式与静态分析。
