---
last_modified: "2026-07-07 18:51"
---

# AI 记账分类回退策略

## 背景

Moni 支持一级/二级分类。AI 记账需要把所有 active 分类发送给模型，让模型在结构化输出中返回 `category_id`。

## 分类上下文

Rust Core 统一生成分类上下文，不由 Kotlin 拼接。每个分类行包含：

- 层级标签：一级分类 / 二级分类
- `id`
- `type`：`expense` 或 `income`
- `name`
- `description`
- `parent_id`
- 二级分类的 `parent_name`

示例：

```text
- 一级分类 id=1 type=expense name=餐饮 description= parent_id=null
-   二级分类 id=13 type=expense name=外卖 description= parent_id=1 parent_name=餐饮
```

## 模型选择规则

1. 能确定二级分类时，返回二级分类 id。
2. 无法确定具体二级分类，或该一级分类没有二级分类时，返回一级分类 id。
3. 完全不知道分类时，返回 `category_id=-1`。

## UI 保存规则

`category_id=-1` 表示草稿未分类，只能用于提示用户补全字段。

- 草稿卡片显示“未分类/请选择分类”。
- 确认保存按钮不可用或保存前拦截。
- 用户必须手动选择分类后才能入账。

## 不做的事

本期不在 Rust 侧做额外启发式分类猜测，避免规则引擎和模型判断冲突。后续如需要，可基于历史数据或用户习惯另行规划推荐分类模块。
