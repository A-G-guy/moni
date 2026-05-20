# SmartDraftCard 状态机实现笔记

## 2025-05-20

### 完成内容
实现了 SmartDraftCard 组件的三种状态机（Draft / Saved / Expired）。

### 关键实现点

1. **状态参数扩展**
   - 新增 `onEditClick: () -> Unit = {}` 参数，用于 Saved 态的铅笔图标点击回调
   - 保留原有 `cardStatus: CardStatus = CardStatus.DRAFT` 参数

2. **Draft 态特征**
   - 呼吸灯边框：使用 `rememberInfiniteTransition` + `animateFloat` 实现透明度 0.3f → 0.6f 周期变化
   - 边框颜色通过 `animateColorAsState` 平滑过渡
   - 所有字段可点击编辑（`clickable` 仅在 `isEditable = true` 时应用）
   - 显示"取消"和"确认保存"按钮

3. **Saved 态特征**
   - 卡片整体亮度降低 20%（`Modifier.alpha(0.8f)`）
   - 操作按钮隐藏（使用 `AnimatedVisibility`）
   - 右下角显示"已入账"印章（红色/主题色文字，-15 度旋转）
   - 右上角显示铅笔图标按钮（点击触发 `onEditClick`）
   - 所有字段不可点击编辑

4. **Expired 态特征**
   - 卡片灰色半透明（`Modifier.alpha(0.5f)`）
   - 所有按钮消失
   - 所有字段不可点击
   - 显示"已失效"灰色文字

5. **动画实现**
   - `animateFloatAsState`：控制卡片整体透明度，duration = 300ms
   - `animateColorAsState`：控制边框颜色过渡
   - `AnimatedVisibility`：控制按钮显示/隐藏
   - `rememberInfiniteTransition`：Draft 态呼吸灯效果

### 遇到的坑

- **Compose 动画 API 包路径**：
  - `animateFloatAsState` 在 `androidx.compose.animation.core`
  - `animateColorAsState` 在 `androidx.compose.animation`（不在 `.core` 下）
  - 混淆这两个包路径会导致编译错误

### 代码设计

- 将状态判断逻辑集中在 `SmartDraftCard` 主函数中，通过 `isEditable` 布尔值传递给子组件
- 子组件保持纯展示逻辑，不直接依赖 `CardStatus`
- 使用 `AnimatedVisibility` 而非条件渲染，确保按钮进出有平滑动画
