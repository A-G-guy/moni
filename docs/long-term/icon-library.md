# 图标开发规范

> 本文档记录 moni 图标体系的设计决策与开发规范。代码实现参见 `android/app/src/main/java/com/agguy/moni/app/icons/` 目录。

## 架构概览

图标体系基于 **Google Material Symbols Rounded** 可变字体，通过子集化裁剪为两个静态字体文件：

| 字体文件 | 作用 | 大小 |
|---------|------|------|
| `material_symbols_outlined.ttf` | 标准态（outline） | ~142 KB |
| `material_symbols_filled.ttf` | 填充态（filled） | ~125 KB |

**为什么用字体而非 XML Vector Drawable？**

- 359 个图标总字体体积约 267 KB，同等数量的 XML 文件体积数倍于此；
- 通过 `FILL` 轴（或双静态字体）实现 outline/filled 状态切换，不需要成对资源文件；
- 新增图标只需更新 name → codepoint 映射，不需要新增 XML 文件。

## 核心文件

| 文件 | 职责 |
|------|------|
| `MaterialSymbolsFont.kt` | 定义 `MaterialSymbolsOutlined` / `MaterialSymbolsFilled` 两个 `FontFamily` |
| `SymbolIcon.kt` | `SymbolIcon()` Composable，所有图标的统一渲染入口 |
| `SymbolCodepoints.kt` | name → Unicode codepoint 映射表，向后兼容的兜底保障 |
| `SymbolGroups.kt` | 分类图标分组、中文显示名、`iconForCategory()` 关键词匹配 |

## 图标选用原则

### 1. 语义优先

图标必须与其功能或分类含义直接对应。禁止为"凑数量"而放入语义牵强或完全无关的图标。

**反面案例（已清理）：**
- `flutter_dash`（Flutter 吉祥物）曾被放入"人物社交"分组 — 与社交完全无关，已删除。

### 2. 避免重复

同一字形（相同 Unicode codepoint）在分组中只能出现一次。跨组重复需根据语义归属决定保留位置：

| 图标 | 保留分组 | 移除分组 | 理由 |
|------|---------|---------|------|
| `kitchen` | 饮食 | 家居生活 | 厨房是烹饪场所，饮食语义更强 |
| `water_drop` | 饮食 | 家居生活 | 水滴在饮食中代表水/饮料 |
| `tv` | 科技数码 | 家居生活 | 电视属于数码设备 |
| `camera` | 娱乐休闲 | 科技数码 | 相机在娱乐中代表摄影/拍照 |
| `ac_unit` | 家居生活 | 自然户外 | 空调是家电，非自然元素 |

**同 codepoint 不同名称的合并：**

Material Symbols 中部分字形有多个官方名称（如 `people` 与 `group` 均为 0xEA21）。分组中只保留一个最贴切的名称：

| 保留名称 | 删除名称 | Codepoint |
|---------|---------|-----------|
| `group` | `people` | 0xEA21 |
| `computer` | `laptop` | 0xE31E |
| `headset` | `headphones` | 0xF01F |
| `sell` | `local_offer` | 0xF05B |
| `local_dining` | `restaurant_menu` | 0xE561 |
| `landscape` | `terrain` | 0xE564 |

### 3. 中文显示名规范

- 使用简洁的中文名词，不超过 4 个字；
- 避免数字后缀（如"游乐场2"），除非图标本身有明确区分；
- 同一分组内名称不得重复。

**正面案例：** `diversity_1` → "多元人群"、`corporate_fare` → "商务差旅"

**反面案例（已修复）：** `playground_2` → "游乐场2"、`air_purifier_gen` → "净化器二代"

### 4. UI 固定图标一致性

应用内固定功能按钮的图标应统一，避免同一功能在不同页面使用不同图标：

| 功能 | 图标名称 | 说明 |
|------|---------|------|
| 返回 | `arrow_back` | 全应用统一 |
| 添加 | `add` (filled) | FAB / 新增按钮 |
| 删除 | `delete` | 删除操作 |
| 勾选/确认 | `check` | 确认、选中 |
| 编辑 | `edit` | 编辑操作 |
| 归档 | `archive` | 归档分类 |
| 恢复归档 | `unarchive` | 恢复已归档 |
| 展开/收起 | `expand_more` / `expand_less` | 列表展开控制 |
| 设置 | `settings` | 设置入口 |
| 账单 | `receipt` | 底部导航-账单 |
| 统计 | `bar_chart` | 底部导航-统计 |
| 分类管理 | `category` | 分类管理入口（非 `filter_list`）|
| 预算 | `savings` | 预算管理入口 |
| 日期选择 | `event` | 日期字段 |

**注意：** `filter_list` 语义为"筛选列表"，曾用于分类管理入口按钮，已改为更贴切的 `category`。

## 分组结构

分类图标按主题分为 12 组，共 344 个图标：

| 分组 | 图标数 | 主题范围 |
|------|--------|---------|
| 饮食 | 30 | 餐厅、饮品、食材、烹饪器具 |
| 交通出行 | 30 | 交通工具、出行服务、路况 |
| 购物消费 | 22 | 购物场所、支付方式、金融 |
| 家居生活 | 26 | 家具、家电、房屋空间 |
| 工作办公 | 29 | 办公设备、商务活动、沟通 |
| 医疗健康 | 25 | 医疗机构、药物、健康活动 |
| 教育学习 | 29 | 学校、书籍、艺术创作、编程 |
| 娱乐休闲 | 31 | 影视、音乐、游戏、运动 |
| 人物社交 | 28 | 人群、情感、活动、组织 |
| 自然户外 | 28 | 自然景观、户外运动、天气 |
| 科技数码 | 29 | 电子设备、网络、存储 |
| 其他 | 37 | 通用操作、系统图标、兜底选项 |

## 新增图标流程

### 1. 确认图标已在 Material Symbols 中

查阅 [Google Fonts - Material Symbols](https://fonts.google.com/icons)，确认图标名称与 codepoint。

### 2. 更新 codepoints 映射

在 `SymbolCodepoints.kt` 中新增条目：

```kotlin
"new_icon_name" to 0xXXXX, // 中文注释
```

### 3. 加入分组（如用于分类）

在 `SymbolGroups.kt` 对应分组中新增：

```kotlin
SymbolIconInfo("new_icon_name", 0xXXXX, "中文显示名"),
```

选择最贴切的分组，遵循"语义优先"原则。

### 4. 更新字体子集

如新增图标的 codepoint 不在当前子集字体中：

```bash
# 编辑 scripts/icon_codepoints.txt，新增 codepoint
# 运行子集化脚本
python3 scripts/subset_font.py
```

### 5. 验证

- 编译通过：`./gradlew :app:assembleRelease`
- 图标选择器中图标正常显示；
- 选中/未选中态切换正常；
- 已存储的图标名称不受影响（SymbolCodepoints.kt 保持向后兼容）。

## 向后兼容策略

- **SymbolCodepoints.kt 永不删除旧映射**：旧数据库中存储的 `iconName` 必须始终可解析；
- **SymbolGroups.kt 可以清理分组**：从选择器中移除不贴切/重复的图标，不影响已有数据的渲染；
- **UI 图标迁移需全局检查**：修改固定功能图标时，需 grep 全仓库确认所有调用点同步更新。
