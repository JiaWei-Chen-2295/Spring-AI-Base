package com.example.aitemplate.client.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val conversationId: String,
    val modelId: String,
    val message: String,
    val tools: List<String> = emptyList(),
    val skills: List<String> = emptyList()
)

@Serializable
data class ChatResponse(
    val requestId: String = "",
    val conversationId: String = "",
    val modelId: String = "",
    val content: String = "",
    val toolCalls: List<ToolCallInfo> = emptyList()
)

@Serializable
data class ToolCallInfo(
    val toolName: String = "",
    val input: String = "",
    val output: String = "",
    val durationMs: Long = 0
)

@Serializable
data class SkillApplyInfo(
    val name: String = "",
    val version: String = ""
)
