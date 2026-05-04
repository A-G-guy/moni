package com.agguy.moni.core

import kotlinx.serialization.Serializable

@Serializable
data class CoreEffect(val kind: String, val payloadJson: String)

@Serializable
data class CoreUpdate(val stateJson: String, val effects: List<CoreEffect>)
