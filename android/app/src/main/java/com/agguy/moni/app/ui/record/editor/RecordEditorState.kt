package com.agguy.moni.app.ui.record.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.agguy.moni.core.CoreCategory
import com.agguy.moni.core.CoreRecord
import com.agguy.moni.core.RecordType
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 记账编辑器状态持有者。
 *
 * 管理记录编辑过程中的所有 UI 状态，支持屏幕旋转恢复。
 */
class RecordEditorState(saved: List<Any?>?) {

    var recordType by mutableStateOf(
        (saved?.getOrNull(0) as? String)?.let { parseRecordType(it) } ?: RecordType.EXPENSE
    )
        private set

    var selectedCategoryId by mutableLongStateOf(saved?.getOrNull(1) as? Long ?: -1L)
        private set

    /** 当前在二级视图中的父分类 ID */
    var selectedParentCategoryId by mutableLongStateOf(saved?.getOrNull(2) as? Long ?: -1L)
        private set

    /** 原始输入表达式，如 "15.5+20" */
    var amountExpression by mutableStateOf(saved?.getOrNull(3) as? String ?: "")
        private set

    /** 已计算/确认的金额（分），用于保存 */
    var confirmedAmountCents by mutableLongStateOf(saved?.getOrNull(4) as? Long ?: 0L)
        private set

    var note by mutableStateOf(saved?.getOrNull(5) as? String ?: "")
        private set

    var timestamp by mutableLongStateOf(
        saved?.getOrNull(6) as? Long
            ?: LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond()
    )
        private set

    var isNoteEditing by mutableStateOf(false)
        private set

    var currentGridPage by mutableIntStateOf(0)

    var isInSubCategoryView by mutableStateOf(false)
        private set

    /** 当前是否正在输入小数部分 */
    private var isInDecimalMode by mutableStateOf(false)

    /** 当前小数位数（0-2） */
    private var decimalDigits by mutableIntStateOf(0)

    /** 当前显示文本：表达式或计算结果 */
    val displayText: String
        get() = when {
            amountExpression.isNotEmpty()
                && confirmedAmountCents > 0
                && !ExpressionEvaluator.hasPendingOperation(amountExpression) -> {
                val yuan = confirmedAmountCents / 100
                val fen = confirmedAmountCents % 100
                if (fen == 0L) yuan.toString() else "%.2f".format(yuan + fen / 100.0)
            }
            amountExpression.isNotEmpty() -> amountExpression
            else -> "0"
        }

    /** 当前选中的分类（用于底部面板显示） */
    fun selectedCategory(categories: List<CoreCategory>): CoreCategory? {
        return categories.find { it.id == selectedCategoryId }
    }

    /** 获取选中分类的完整名称（含父分类前缀） */
    fun selectedCategoryFullName(categories: List<CoreCategory>): String? {
        val category = selectedCategory(categories) ?: return null
        return if (category.parentId != null) {
            val parent = categories.find { it.id == category.parentId }
            if (parent != null) "${parent.name} - ${category.name}" else category.name
        } else {
            category.name
        }
    }

    /** 切换收入/支出类型 */
    fun updateType(type: RecordType) {
        if (recordType == type) return
        recordType = type
        selectedCategoryId = -1L
        selectedParentCategoryId = -1L
        amountExpression = ""
        confirmedAmountCents = 0L
        isInDecimalMode = false
        decimalDigits = 0
        isInSubCategoryView = false
        currentGridPage = 0
    }

    /** 选中分类 */
    fun selectCategory(categoryId: Long) {
        selectedCategoryId = categoryId
    }

    /** 进入某一级分类的二级视图 */
    fun enterSubView(parentId: Long) {
        selectedParentCategoryId = parentId
        isInSubCategoryView = true
    }

    /** 返回一级分类视图 */
    fun exitSubView() {
        selectedParentCategoryId = -1L
        isInSubCategoryView = false
    }

    /** 输入数字 */
    fun appendDigit(digit: String) {
        if (digit == ".") {
            if (isInDecimalMode || amountExpression.isEmpty()) return
            val lastOpIndex = amountExpression.lastIndexOfAny(charArrayOf('+', '-', '×', '÷'))
            val lastOperand = if (lastOpIndex >= 0) {
                amountExpression.substring(lastOpIndex + 1)
            } else {
                amountExpression
            }
            if (lastOperand.contains(".")) return
            amountExpression += "."
            isInDecimalMode = true
            decimalDigits = 0
            return
        }

        if (isInDecimalMode) {
            if (decimalDigits >= 2) return
            decimalDigits++
        }

        val lastOpIndex = amountExpression.lastIndexOfAny(charArrayOf('+', '-', '×', '÷'))
        val lastOperand = if (lastOpIndex >= 0) {
            amountExpression.substring(lastOpIndex + 1)
        } else {
            amountExpression
        }
        val integerPart = lastOperand.substringBefore(".")
        if (integerPart.length >= 7) return

        amountExpression += digit
        recalculateIfNeeded()
    }

    /** 追加运算符 */
    fun appendOperator(op: String) {
        if (amountExpression.isEmpty()) return
        if (amountExpression.last() in setOf('+', '-', '×', '÷', '.')) {
            amountExpression = amountExpression.dropLast(1)
        }
        amountExpression += op
        isInDecimalMode = false
        decimalDigits = 0
        confirmedAmountCents = 0L
    }

    /** 退格 */
    fun backspace() {
        if (amountExpression.isEmpty()) return
        val removed = amountExpression.last()
        amountExpression = amountExpression.dropLast(1)
        when {
            removed == '.' -> {
                isInDecimalMode = false
                decimalDigits = 0
            }
            isInDecimalMode && decimalDigits > 0 -> decimalDigits--
        }
        recalculateIfNeeded()
    }

    /** 执行计算（点击 = 按钮） */
    fun calculate(): Boolean {
        if (!ExpressionEvaluator.hasPendingOperation(amountExpression)) return false
        val result = ExpressionEvaluator.evaluateToCents(amountExpression) ?: return false
        confirmedAmountCents = result
        val yuan = result / 100
        val fen = result % 100
        amountExpression = if (fen == 0L) yuan.toString() else "%.2f".format(yuan + fen / 100.0)
        isInDecimalMode = fen != 0L
        decimalDigits = if (fen == 0L) 0 else 2
        return true
    }

    /** 开始编辑备注 */
    fun startNoteEdit() {
        isNoteEditing = true
    }

    /** 结束编辑备注 */
    fun endNoteEdit() {
        isNoteEditing = false
    }

    /** 更新备注 */
    fun updateNote(newNote: String) {
        note = newNote
    }

    /** 更新时间戳 */
    fun updateTimestamp(newTimestamp: Long) {
        timestamp = newTimestamp
    }

    /** 更新日期（保持时分秒不变） */
    fun updateDate(dateEpochSecond: Long) {
        val current = Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault())
        val newDate = Instant.ofEpochSecond(dateEpochSecond).atZone(ZoneId.systemDefault()).toLocalDate()
        val combined = newDate.atTime(current.toLocalTime())
        timestamp = combined.atZone(ZoneId.systemDefault()).toEpochSecond()
    }

    /** 更新时间（保持日期不变） */
    fun updateTime(hour: Int, minute: Int) {
        val current = Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault())
        val combined = current.toLocalDate().atTime(hour, minute)
        timestamp = combined.atZone(ZoneId.systemDefault()).toEpochSecond()
    }

    /** 从已有记录加载（编辑模式） */
    fun loadFromRecord(record: CoreRecord, categories: List<CoreCategory>) {
        recordType = if (record.recordType == "income") RecordType.INCOME else RecordType.EXPENSE
        selectedCategoryId = record.categoryId
        note = record.note
        timestamp = record.createdAt
        confirmedAmountCents = record.amountCents
        amountExpression = formatCentsToExpression(record.amountCents)
        isInDecimalMode = amountExpression.contains(".")
        decimalDigits = if (isInDecimalMode) {
            amountExpression.substringAfter(".", "").length
        } else 0
        val category = categories.find { it.id == record.categoryId }
        if (category?.parentId != null) {
            selectedParentCategoryId = category.parentId
        }
    }

    /** 重置为默认状态 */
    fun reset() {
        recordType = RecordType.EXPENSE
        selectedCategoryId = -1L
        selectedParentCategoryId = -1L
        amountExpression = ""
        confirmedAmountCents = 0L
        note = ""
        timestamp = LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond()
        isNoteEditing = false
        currentGridPage = 0
        isInSubCategoryView = false
        isInDecimalMode = false
        decimalDigits = 0
    }

    /** 转为可保存的列表。 */
    fun toSaveable(): List<Any?> = listOf(
        recordType.name,
        selectedCategoryId,
        selectedParentCategoryId,
        amountExpression,
        confirmedAmountCents,
        note,
        timestamp
    )

    private fun recalculateIfNeeded() {
        if (!ExpressionEvaluator.hasPendingOperation(amountExpression)) {
            confirmedAmountCents = ExpressionEvaluator.evaluateToCents(amountExpression) ?: 0L
        } else {
            confirmedAmountCents = 0L
        }
    }

    private fun formatCentsToExpression(cents: Long): String {
        val yuan = cents / 100
        val fen = cents % 100
        return if (fen == 0L) yuan.toString() else "%.2f".format(yuan + fen / 100.0)
    }

    companion object {
        /** 使用 [listSaver] 保存为 [List]<Any?>，确保每项都可存入 Bundle。 */
        val Saver = listSaver<RecordEditorState, Any?>(
            save = { it.toSaveable() },
            restore = { RecordEditorState(it) }
        )
    }
}

private fun parseRecordType(name: String): RecordType = when (name) {
    RecordType.INCOME.name -> RecordType.INCOME
    else -> RecordType.EXPENSE
}

@Composable
fun rememberRecordEditorState(
    existingRecord: CoreRecord?,
    categories: List<CoreCategory>
): RecordEditorState {
    val state = rememberSaveable(saver = RecordEditorState.Saver) {
        RecordEditorState(null)
    }
    // 仅在编辑模式首次加载时注入已有数据
    androidx.compose.runtime.LaunchedEffect(existingRecord?.id) {
        existingRecord?.let { state.loadFromRecord(it, categories) }
    }
    return state
}
