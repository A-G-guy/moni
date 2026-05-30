package com.agguy.moni.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agguy.moni.app.model.AiApiFormat
import com.agguy.moni.app.model.AiProviderPreset
import com.agguy.moni.app.model.AiProviderPresetSaveRequest
import com.agguy.moni.app.model.AiThinkingLevel
import com.agguy.moni.app.repository.AiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** AI 设置页状态。 */
data class AiSettingsUiState(
    val presets: List<AiProviderPreset> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null,
    val editingPreset: AiPresetFormState? = null,
)

/** AI 预设编辑表单状态。 */
data class AiPresetFormState(
    val id: Long? = null,
    val name: String = "",
    val apiFormat: AiApiFormat = AiApiFormat.OpenAiChatCompletions,
    val baseUrl: String = "https://api.openai.com/v1",
    val apiKey: String = "",
    val model: String = "gpt-4o-mini",
    val thinkingLevel: AiThinkingLevel = AiThinkingLevel.Off,
    val supportsVision: Boolean = false,
    val isDefault: Boolean = false,
) {
    fun toSaveRequest(): AiProviderPresetSaveRequest = AiProviderPresetSaveRequest(
        id = id,
        name = name,
        apiFormat = apiFormat,
        baseUrl = baseUrl,
        apiKey = apiKey.takeIf { it.isNotBlank() },
        model = model,
        thinkingLevel = thinkingLevel,
        supportsVision = supportsVision,
        isDefault = isDefault,
    )
}

/** AI 设置页 ViewModel。 */
class AiSettingsViewModel(
    private val repository: AiRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AiSettingsUiState(isLoading = true))
    val uiState: StateFlow<AiSettingsUiState> = _uiState.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            runCatching {
                _uiState.value = _uiState.value.copy(isLoading = true, message = null)
                repository.listPresets()
            }.onSuccess { presets ->
                _uiState.value = _uiState.value.copy(presets = presets, isLoading = false)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(isLoading = false, message = error.safeMessage())
            }
        }
    }

    fun startCreate() {
        _uiState.value = _uiState.value.copy(editingPreset = AiPresetFormState())
    }

    fun startEdit(preset: AiProviderPreset) {
        _uiState.value = _uiState.value.copy(
            editingPreset = AiPresetFormState(
                id = preset.id,
                name = preset.name,
                apiFormat = preset.apiFormat,
                baseUrl = preset.baseUrl,
                model = preset.model,
                thinkingLevel = preset.thinkingLevel,
                supportsVision = preset.supportsVision,
                isDefault = preset.isDefault,
            )
        )
    }

    fun updateForm(form: AiPresetFormState) {
        _uiState.value = _uiState.value.copy(editingPreset = form)
    }

    fun dismissEditor() {
        _uiState.value = _uiState.value.copy(editingPreset = null)
    }

    fun saveEditing() {
        val form = _uiState.value.editingPreset ?: return
        viewModelScope.launch {
            runCatching {
                repository.savePreset(form.toSaveRequest())
            }.onSuccess {
                _uiState.value = _uiState.value.copy(editingPreset = null, message = "AI 预设已保存")
                reload()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(message = error.safeMessage())
            }
        }
    }

    fun deletePreset(id: Long) {
        viewModelScope.launch {
            runCatching { repository.deletePreset(id) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(message = "AI 预设已删除")
                    reload()
                }
                .onFailure { error -> _uiState.value = _uiState.value.copy(message = error.safeMessage()) }
        }
    }

    fun setDefault(id: Long) {
        viewModelScope.launch {
            runCatching { repository.setDefaultPreset(id) }
                .onSuccess { reload() }
                .onFailure { error -> _uiState.value = _uiState.value.copy(message = error.safeMessage()) }
        }
    }

    fun testConnection(id: Long) {
        viewModelScope.launch {
            runCatching { repository.testConnection(id) }
                .onSuccess { _uiState.value = _uiState.value.copy(message = "连接测试成功") }
                .onFailure { error -> _uiState.value = _uiState.value.copy(message = error.safeMessage()) }
        }
    }

    private fun Throwable.safeMessage(): String = message ?: "操作失败"
}
