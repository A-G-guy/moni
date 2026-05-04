# 图标库扩展指南

> 文档只解释设计决策（Why），不解释代码实现（What）。代码即文档。

## 当前规模

图标库基于 **Google Material Symbols Rounded**（Weight 400），以 Android Vector Drawable 形式托管。

- **唯一图标基数**：25 个（含标准 + Filled 双态，共 50 个 XML 文件）
- **命名规范**：`ic_{name}.xml`（标准态）/ `ic_{name}_filled.xml`（选中态）
- **尺寸统一**：`24dp × 24dp`，`viewportWidth="960" viewportHeight="960"`

## 分组结构

| 分组 | 图标 | 用途 |
|------|------|------|
| **导航** | receipt, bar_chart, settings | BottomBar 三个主 Tab |
| **操作** | add, check, delete, arrow_back, filter_list | 通用操作按钮 |
| **字段/输入** | attach_money, event, category, edit | 表单输入辅助图标 |
| **分类** | restaurant, directions_car, shopping_bag, payments, home, sports_esports, local_hospital, school | 分类管理 + 记录详情中的分类选择 |
| **其他** | dark_mode, palette, tune, info, help | 设置页、空态、回退图标 |

## 命名规范

1. **文件名**：`ic_{snake_case_name}.xml` / `ic_{snake_case_name}_filled.xml`
2. **Kotlin 常量**：`MoniIcons.{PascalCaseName}` / `MoniIcons.{PascalCaseName}Filled`
3. **数据库存储**：`icon_name` 字段使用 snake_case（如 `"restaurant"`、`"directions_car"`），与文件名去掉前缀后缀后一致

**为什么双态？**
- 标准态用于未选中/常规展示；Filled 态用于选中/激活态。
- 与 Material 3 的 `NavigationBar` 选中态行为一致，提供清晰的视觉反馈。

## 如何新增图标

### 1. 下载 SVG

访问 [Google Fonts - Material Symbols](https://fonts.google.com/icons)，选择：
- **Style**: Rounded
- **Weight**: 400
- **Fill**: 0（标准态）/ 1（Filled 态）

下载 SVG 文件。

### 2. 转 Vector Drawable

使用 Android Studio 的 **Vector Asset Studio**：

```
File → New → Vector Asset → Local file (SVG) → Next → Finish
```

或命令行（需 `vd-tool`）：

```bash
vd-tool -c -in input.svg -out output.xml
```

### 3. 放入资源目录

```
android/app/src/main/res/drawable/
├── ic_{name}.xml          # 标准态
└── ic_{name}_filled.xml   # Filled 态
```

### 4. 注册到 MoniIcons

在 `MoniIcons.kt` 中新增常量：

```kotlin
val NewIcon = R.drawable.ic_new_icon
val NewIconFilled = R.drawable.ic_new_icon_filled
```

### 5. 注册到 CategoryIcons（如用于分类）

在 `CategoryIcons.kt` 中：

```kotlin
// 1. 添加名称映射（如需要中英文匹配）
"new_icon", "新分类" -> MoniIcons.NewIcon

// 2. 添加到可选图标列表
val AvailableCategoryIcons: List<Pair<String, Int>> = listOf(
    // ... 现有图标
    "new_icon" to MoniIcons.NewIcon,
)
```

### 6. 验证

- 编译通过：`./gradlew :android:app:compileDebugSources`
- 在对应界面确认图标渲染正常（标准态 + Filled 态）

## IconPicker 的使用方式

`AddCategoryDialog` 内部使用 `FlowRow` 展示 `AvailableCategoryIcons`，用户点击选择：

```kotlin
AvailableCategoryIcons.forEachIndexed { index, (iconName, iconRes) ->
    IconOption(
        iconRes = iconRes,
        isSelected = selectedIconIndex == index,
        onClick = { selectedIconIndex = index }
    )
}
```

- 选中态使用 `primaryContainer` 背景 + `primary` 边框；
- 未选中态使用 `surfaceContainerHighest` 背景；
- 最终存入数据库的是 `iconName`（snake_case 字符串），而非资源 ID。

**为什么存名称而非资源 ID？**
- 资源 ID 在编译期可能变化，不适合持久化存储；
- 名称是稳定的字符串标识，跨版本兼容；
- 支持运行时通过 `iconForCategory(name)` 做中英文回退匹配。
