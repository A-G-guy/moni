package com.agguy.moni.core

import kotlinx.serialization.encodeToString
import uniffi.moni_core.MoniCore

data class CoreMutation(
    val state: CoreAppState,
    val effects: List<CoreEffect>
)

class RustCoreController {
    private val core = MoniCore()

    fun initialize(): CoreMutation {
        val update = core.initialize()
        return decodeMutation(update)
    }

    fun initializeWithDb(dbPath: String): CoreMutation {
        val update = core.initializeWithDb(dbPath)
        return decodeMutation(update)
    }

    fun dispatch(intent: CoreIntent): CoreMutation {
        val update = core.dispatch(BridgeJson.encodeToString(intent))
        return decodeMutation(update)
    }

    private fun decodeMutation(update: uniffi.moni_core.CoreUpdate): CoreMutation =
        CoreMutation(
            state = BridgeJson.decodeFromString(CoreAppState.serializer(), update.stateJson),
            effects = update.effects.map { CoreEffect(it.kind, it.payloadJson) }
        )
}
