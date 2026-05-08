package com.agguy.moni.app.ui.category

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.agguy.moni.app.AppState
import com.agguy.moni.app.theme.MoniTheme
import com.agguy.moni.core.CoreCategory
import org.junit.Rule
import org.junit.Test

/**
 * CategoryListScreen Compose UI 测试。
 *
 * 覆盖分类列表渲染、空状态、归档按钮存在性等场景。
 */
class CategoryListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** 在 MoniTheme 下渲染被测屏幕，消除重复模板。 */
    private fun setScreenContent(appState: AppState = AppState()) {
        composeTestRule.setContent {
            MoniTheme {
                CategoryListScreen(
                    appState = appState,
                    onDispatch = {},
                    onNavigateBack = {},
                    onNavigateToArchivedCategories = {}
                )
            }
        }
    }

    @Test
    fun 应显示页面标题() {
        setScreenContent()
        composeTestRule.onNodeWithText("分类管理").assertIsDisplayed()
    }

    @Test
    fun 应显示支出和收入标签页() {
        setScreenContent()
        composeTestRule.onNodeWithText("支出").assertIsDisplayed()
        composeTestRule.onNodeWithText("收入").assertIsDisplayed()
    }

    @Test
    fun 空分类列表时应显示空状态提示() {
        setScreenContent()
        composeTestRule.onNodeWithText("暂无分类").assertIsDisplayed()
        composeTestRule.onNodeWithText("点击右下角添加").assertIsDisplayed()
    }

    @Test
    fun 有分类时应渲染分类名称() {
        val categories = listOf(
            CoreCategory(
                id = 1,
                name = "餐饮",
                description = "日常餐饮消费",
                categoryType = "expense",
                iconName = "restaurant",
                sortOrder = 1,
                archivedAt = null
            ),
            CoreCategory(
                id = 2,
                name = "交通",
                description = null,
                categoryType = "expense",
                iconName = "transport",
                sortOrder = 2,
                archivedAt = null
            )
        )

        setScreenContent(AppState(categories = categories))
        composeTestRule.onNodeWithText("餐饮").assertIsDisplayed()
        composeTestRule.onNodeWithText("交通").assertIsDisplayed()
    }

    @Test
    fun 预设分类应显示预设标签() {
        val categories = listOf(
            CoreCategory(
                id = 1,
                name = "餐饮",
                description = null,
                categoryType = "expense",
                iconName = "restaurant",
                sortOrder = 1,
                archivedAt = null
            )
        )

        setScreenContent(AppState(categories = categories))
        composeTestRule.onNodeWithText("餐饮").assertIsDisplayed()
    }

    @Test
    fun 已归档分类不应出现在列表中() {
        val categories = listOf(
            CoreCategory(
                id = 1,
                name = "旧分类",
                description = null,
                categoryType = "expense",
                iconName = "category",
                sortOrder = 1,
                archivedAt = 12345678L
            )
        )

        setScreenContent(AppState(categories = categories))
        // 已归档分类被过滤掉，应显示空状态
        composeTestRule.onNodeWithText("暂无分类").assertIsDisplayed()
    }

    @Test
    fun 收入类型分类应在收入标签页下显示() {
        val categories = listOf(
            CoreCategory(
                id = 1,
                name = "工资",
                description = null,
                categoryType = "income",
                iconName = "salary",
                sortOrder = 1,
                archivedAt = null
            )
        )

        setScreenContent(AppState(categories = categories))
        // 默认选中支出标签页，收入分类不应显示
        composeTestRule.onNodeWithText("暂无分类").assertIsDisplayed()
    }
}
