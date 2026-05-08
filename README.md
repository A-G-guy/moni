# Moni

<p align="center">
  <img src="https://img.shields.io/badge/Rust-1.93.0-DEA584?logo=rust&logoColor=white" alt="Rust" />
  <img src="https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Android-API_28+-3DDC84?logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/License-Apache--2.0-blue.svg" alt="License" />
</p>

<p align="center">
  <a href="./README.zh-CN.md">简体中文</a> | English
</p>

> **A simple yet extraordinary, finely crafted bookkeeping app tailored for you.**
>
> Moni = Money + Mini. Achieve the best bookkeeping experience with the least mental burden.

---

## Tech Highlights

| Feature | Description |
|---------|-------------|
| **Rust Core** | Cross-platform business logic and global state machine with compile-time memory safety |
| **Pure Kotlin UI** | Stateless rendering layer, responsible only for UI and system capability bridging |
| **UniFFI Bindings** | Auto-generated JNI bindings with JSON-serialized communication |
| **SQLite Persistence** | File database with in-memory fallback |
| **Modular Design** | Features are toggleable, boundaries are clear, and core data safety is prioritized |

## Architecture

```
+------------------+
|   Kotlin UI      |  Compose UI + ViewModel (stateless)
+------------------+
         |  StateFlow / Effect callbacks
+------------------+
|   JNI / Bridge   |  UniFFI + JNA auto-generated bindings
+------------------+
         |  JSON strings (Intent / State / Effects)
+------------------+
|   Rust Core      |  State machine + business logic + SQLite
+------------------+
```

- **Kotlin UI**: Receives State to render UI, encapsulates user actions as Intents and dispatches them
- **JNI/Bridge**: UniFFI generates FFI bindings, JNA handles dynamic library loading
- **Rust Core**: Centralized state machine, Intent dispatch, database read/write, effect generation

## Quick Start

### Requirements

| Tool | Version |
|------|---------|
| JDK | 17+ |
| Android SDK | API 36 + NDK 29.0.14206865 |
| Rust | stable (auto-activated by `rust-toolchain.toml`) |

```bash
# Install Android targets
rustup target add aarch64-linux-android x86_64-linux-android

# Enable git hooks
git config core.hooksPath .githooks

# One-shot code quality check
./gradlew checkAll

# Run Rust tests
cargo test --workspace

# Build Release APK
./gradlew :app:assembleRelease
```

## Project Structure

```
moni/
├── android/app/          # Kotlin Android app (Compose UI)
├── moni-core/            # Rust core business logic (state machine + SQLite)
├── moni-contracts/       # Rust interface contracts and DTOs
├── docs/                 # Project documentation hub
│   ├── long-term/        # Architecture, data model, code style, etc.
│   └── short-term/       # Dev guides, FAQ, quickstart, etc.
├── config/               # Detekt and other tool configurations
├── scripts/              # Build helper scripts
└── .githooks/            # Pre-commit quality gates
```

## Documentation

| Document | Description |
|----------|-------------|
| [Development Guide](docs/short-term/development.md) | Environment setup, IDE configuration, one-shot health checks |
| [Code Style](docs/long-term/code-style.md) | Split strategy, file line limits, naming conventions |
| [Data Model](docs/long-term/data-model.md) | Category / Record / RecordType entity design |
| [Category Management](docs/long-term/category-management.md) | CRUD state machine, boundary rules, color strategy |
| [Architecture](docs/long-term/architecture.md) | Layer responsibilities, data flow, FFI communication |
| [Backup & Restore](docs/long-term/backup-guide.md) | ZIP format, dual-version management, integrity checks |

More documents are available at [docs/README.md](docs/README.md).

## License

This project is open-sourced under the [Apache-2.0](LICENSE) license.
