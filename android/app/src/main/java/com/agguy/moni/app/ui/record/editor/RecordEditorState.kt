package com.agguy.moni.app.ui.record.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.agguy.moni.core.CoreCategory
import com.agguy.moni.core.CoreRecord
import com.agguy.moni.core.RecordType
import com.agguy.moni.core.serialName
import java.time.LocalDate
import java.time.ZoneId

/**
 * 记账编辑器状态持有者。
 *
 * 管理记录编辑过程中的所有 UI 状态，支持屏幕旋转恢复。
 */
class RecordEditorState internal constructor(saved: BundleState?) {

    var recordType by mutableStateOf(saved?.recordType ?: RecordType.EXPENSE)
        private set

    var selectedCategoryId by mutableLongStateOf(saved?.selectedCategoryId ?: -1L)
        private set

    /** 当前在二级视图中的父分类 ID */
    var selectedParentCategoryId by mutableLongStateOf(saved?.selectedParentCategoryId ?: -1L)
        private set

    /** 原始输入表达式，如 "15.5+20" */
    var amountExpression by mutableStateOf(saved?.amountExpression ?: "")
        private set

    /** 已计算/确认的金额（分），用于保存 */
    var confirmedAmountCents by mutableLongStateOf(saved?.confirmedAmountCents ?: 0L)
        private set

    var note by mutableStateOf(saved?.note ?: "")
        private set

    var timestamp by mutableLongStateOf(
        saved?.timestamp ?: LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
    )
        private set

    var isNoteEditing by mutableStateOf(false)
        private set

    var currentGridPage by mutableStateOf(0)

    var isInSubCategoryView by mutableStateOf(false)
        private set

    /** 当前是否正在输入小数部分 */
    private var isInDecimalMode by mutableStateOf(false)

    /** 当前小数位数（0-2） */
    private var decimalDigits by mutableStateOf(0)

    /** 当前显示文本：表达式或计算结果 */
    val displayText: String
        get() = when {
            amountExpression.isNotEmpty() && confirmedAmountCents > 0 && !ExpressionEvaluator.hasPendingOperation(amountExpression) -> {
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
            // 检查最后一个操作数是否已有小数点
            val lastOpIndex = amountExpression.lastIndexOfAny(charArrayOf('+', '-'))
            val lastOperand = if (lastOpIndex >= 0) amountExpression.substring(lastOpIndex + 1) else amountExpression
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

        // 限制整数部分长度（最大 999999.99，约7位整数）
        val lastOpIndex = amountExpression.lastIndexOfAny(charArrayOf('+', '-'))
        val lastOperand = if (lastOpIndex >= 0) amountExpression.substring(lastOpIndex + 1) else amountExpression
        val integerPart = lastOperand.substringBefore(".")
        if (integerPart.length >= 7) return

        amountExpression += digit
        recalculateIfNeeded()
    }

    /** 追加运算符 */
    fun appendOperator(op: String) {
        if (amountExpression.isEmpty()) return
        // 如果表达式以运算符结尾，替换它
        if (amountExpression.last() in setOf('+', '-', '.')) {
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
        // 计算后将表达式替换为结果值
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

    /** 更新日期 */
    fun updateTimestamp(newTimestamp: Long) {
        timestamp = newTimestamp
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
        // 恢复二级视图状态（如果分类是子分类）
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
        timestamp = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
        isNoteEditing = false
        currentGridPage = 0
        isInSubCategoryView = false
        isInDecimalMode = false
        decimalDigits = 0
    }

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

    // ---------- Saver ----------

    data class BundleState(
        val recordType: RecordType,
        val selectedCategoryId: Long,
        val selectedParentCategoryId: Long,
        val amountExpression: String,
        val confirmedAmountCents: Long,
        val note: String,
        val timestamp: Long
    )

    internal fun toBundleState(): BundleState = BundleState(
        recordType = recordType,
        selectedCategoryId = selectedCategoryId,
        selectedParentCategoryId = selectedParentCategoryId,
        amountExpression = amountExpression,
        confirmedAmountCents = confirmedAmountCents,
        note = note,
        timestamp = timestamp
    )

    companion object {
        val Saver: Saver<RecordEditorState, BundleState> = Saver(
            save = { it.toBundleState() },
            restore = { RecordEditorState(it) }
        )
    }
}

@Composable
fun rememberRecordEditorState(
    existingRecord: CoreRecord?,
    categories: List<CoreCategory>
): RecordEditorState {
    val state = rememberSaveable(saver = RecordEditorState.Saver) {
        RecordEditorState(null)
    }
    // 编辑模式时加载已有数据
    if (existingRecord != null) {
        state.loadFromRecord(existingRecord, categories)
    }
    return state
}
