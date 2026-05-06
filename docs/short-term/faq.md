# 常见问题

> 开发过程中高频出现的问题与解决方式。

| 问题 | 答案 |
|------|------|
| `./gradlew` 第一次运行很慢？ | 首次下载 Gradle 与项目依赖，耐心等待；缓存建立后后续会快得多。 |
| `cargo test` 在 worktree 中编译失败？ | worktree 无 target 缓存，首次编译较慢，重新完整编译一次即可。 |
| detekt 报错过多？ | 见 `development.md` 6.1，可生成 baseline 过渡，但目标是清零。 |
| clippy `pedantic` 太严？ | 见 `development.md` 6.2，在根 `Cargo.toml` 中针对性 `allow`，不建议整组关闭。 |
| `cargo fmt` 自动改了大量文件？ | IDE 设置与 `rustfmt.toml` 不一致，将 IDE 配置同步为项目设置（max_width=100、edition=2024）。 |
| pre-commit hook 太慢？ | 见 `development.md` 6.4，临时用 `git commit --no-verify` 绕过，或考虑后续改为 `--changed-files` 模式。 |
| 测试覆盖率怎么看？ | `cargo llvm-cov --workspace --summary-only`。 |
| Android 单元测试报 `R.drawable` 找不到？ | 确保是 JVM 单元测试；仪器化测试需要真机/模拟器。 |
| UniFFI 绑定生成失败？ | 先确认 `cargo build -p moni-core --release` 成功，再检查 `ANDROID_SDK_ROOT` / NDK 路径。 |
| 新增分类图标后编译通过但界面不显示？ | 检查 `MoniIcons.kt` 和 `CategoryIcons.kt` 是否都已注册新图标。 |
