package com.agguy.moni.app.repository

import com.agguy.moni.app.model.AiBookkeepingParseResult
import com.agguy.moni.app.model.AiBookkeepingParseResultDto
import com.agguy.moni.app.model.AiProviderPreset
import com.agguy.moni.app.model.AiProviderPresetDto
import com.agguy.moni.app.model.AiProviderPresetSaveRequest
import com.agguy.moni.app.model.toDto
import com.agguy.moni.core.BridgeJson
import com.agguy.moni.core.BridgeJsonEncode
import kotlinx.serialization.builtins.ListSerializer
import uniffi.moni_core.MoniCore

/** AI Provider 与记账解析仓库。 */
class AiRepository(
    private val core: MoniCore,
) {
    suspend fun listPresets(): List<AiProviderPreset> {
        val json = core.aiPresetList()
        return BridgeJson.decodeFromString(ListSerializer(AiProviderPresetDto.serializer()), json)
            .map { it.toModel() }
    }

    suspend fun savePreset(request: AiProviderPresetSaveRequest): Long {
        val json = BridgeJsonEncode.encodeToString(
            com.agguy.moni.app.model.AiProviderPresetSaveRequestDto.serializer(),
            request.toDto(),
        )
        return core.aiPresetSave(json)
    }

    suspend fun deletePreset(id: Long) {
        core.aiPresetDelete(id)
    }

    suspend fun setDefaultPreset(id: Long) {
        core.aiPresetSetDefault(id)
    }

    suspend fun testConnection(id: Long): String = core.aiPresetTestConnection(id)

    suspend fun parseBookkeeping(input: String): AiBookkeepingParseResult {
        val json = core.aiBookkeepingParse(input)
        return BridgeJson.decodeFromString(AiBookkeepingParseResultDto.serializer(), json).toModel()
    }
}
