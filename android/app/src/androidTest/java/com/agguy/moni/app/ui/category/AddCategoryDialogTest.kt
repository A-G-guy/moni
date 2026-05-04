package com.agguy.moni.app.ui.category

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.agguy.moni.app.theme.MoniTheme
import com.agguy.moni.core.RecordType
import org.junit.Rule
import org.junit.Test

/**
 * AddCategoryDialog Compose UI 测试。
 *
 * 覆盖表单校验、字段存在性等交互场景。
 */
class AddCategoryDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** 在 MoniTheme 下渲染被测对话框，消除重复模板。 */
    private fun setDialogContent() {
        composeTestRule.setContent {
            MoniTheme {
                AddCategoryDialog(
                    categoryType = RecordType.EXPENSE,
                    onConfirm = { _, _ -> },
                    onDismiss = {}
                )
            }
        }
    }

    @Test
    fun 对话框应显示标题和名称输入框() {
        setDialogContent()
        composeTestRule.onNodeWithText("添加分类").assertIsDisplayed()
        composeTestRule.onNodeWithText("分类名称").assertIsDisplayed()
    }

    @Test
    fun 空名称时确认按钮应禁用() {
        setDialogContent()
        composeTestRule.onNodeWithText("添加").assertIsNotEnabled()
    }

    @Test
    fun 输入有效名称后确认按钮应启用() {
        setDialogContent()
        composeTestRule.onNodeWithText("分类名称").performTextInput("餐饮")
        composeTestRule.onNodeWithText("添加").assertIsEnabled()
    }

    @Test
    fun 输入空白字符后确认按钮仍应禁用() {
        setDialogContent()
        composeTestRule.onNodeWithText("分类名称").performTextInput("   ")
        composeTestRule.onNodeWithText("添加").assertIsNotEnabled()
    }

    @Test
    fun 对话框应显示图标选择区域() {
        setDialogContent()
        composeTestRule.onNodeWithText("选择图标").assertIsDisplayed()
    }

    @Test
    fun 对话框应显示取消按钮() {
        setDialogContent()
        composeTestRule.onNodeWithText("取消").assertIsDisplayed()
    }

    @Test
    fun 输入超长名称后确认按钮仍应启用() {
        setDialogContent()
        val longName = "a".repeat(200)
        composeTestRule.onNodeWithText("分类名称").performTextInput(longName)
        composeTestRule.onNodeWithText("添加").assertIsEnabled()
    }
}
