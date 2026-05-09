package com.agguy.moni.app.components

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/**
 * 统一返回导航处理器。
 *
 * 支持多条件优先级管理，按照传入顺序由内到外匹配，
 * 第一个 `enabled = true` 的条件会被执行。
 *
 * 使用示例：
 * ```
 * BackNavigationHandler(
 *     isSearchMode to { exitSearch() },
 *     isFilterOpen to { closeFilter() }
 * )
 * ```
 *
 * @param handlers 可变参数，每个 Pair 包含一个布尔条件和对应的返回处理动作。
 *                 按照"由内到外"的顺序传入，后面的条件优先级更高。
 */
@Composable
fun BackNavigationHandler(
    vararg handlers: Pair<Boolean, () -> Unit>
) {
    val active = handlers.findLast { it.first }
    BackHandler(enabled = active != null) {
        active?.second?.invoke()
    }
}
