package com.mooner.starlightdebugbridge.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SocketData(
    @SerialName("T")
    val type: String,
    @SerialName("C")
    val content: String
)
