package com.agguy.moni.core

import kotlinx.serialization.json.Json

internal val BridgeJson = Json {
    ignoreUnknownKeys = true
    prettyPrint = false
    encodeDefaults = true
}
