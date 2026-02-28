package com.example.aitemplate.client.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ModelInfo(
    val provider: String = "",
    val modelId: String = "",
    val capabilities: CapabilitySet? = null,
    val health: String = "UP"
)

@Serializable
data class CapabilitySet(
    val chat: Boolean = true,
    val stream: Boolean = true,
    val toolCall: Boolean = false,
    val vision: Boolean = false
)

@Serializable
data class ToolInfo(
    val toolName: String = "",
    val riskLevel: String = "READ"
)

@Serializable
data class SkillInfo(
    val skillName: String = "",
    val version: String = "",
    val source: String = "builtin",
    val editable: Boolean = false
)

@Serializable
data class AppConfig(
    val authEnabled: Boolean = true
)
