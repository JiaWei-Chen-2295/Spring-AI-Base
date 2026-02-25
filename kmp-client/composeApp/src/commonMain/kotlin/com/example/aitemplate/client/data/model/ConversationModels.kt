package com.example.aitemplate.client.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ConversationInfo(
    val conversationId: String = ""
)

@Serializable
data class MessageInfo(
    val role: String = "",
    val content: String = ""
)
