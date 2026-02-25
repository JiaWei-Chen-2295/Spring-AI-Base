package com.example.aitemplate.client.data.repository

import com.example.aitemplate.client.data.model.*
import com.example.aitemplate.client.data.remote.ChatApi
import com.example.aitemplate.client.data.remote.ConversationApi
import com.example.aitemplate.client.data.sse.SseClient
import kotlinx.coroutines.flow.Flow

class ChatRepository(
    private val chatApi: ChatApi,
    private val conversationApi: ConversationApi,
    private val sseClient: SseClient
) {

    suspend fun chatOnce(baseUrl: String, request: ChatRequest): ChatResponse =
        chatApi.chatOnce(baseUrl, request)

    fun streamChat(
        baseUrl: String,
        conversationId: String,
        model: String,
        message: String,
        tools: List<String> = emptyList(),
        skills: List<String> = emptyList()
    ): Flow<SseEvent> =
        sseClient.streamChat(baseUrl, conversationId, model, message, tools, skills)

    suspend fun listConversations(baseUrl: String): List<ConversationInfo> =
        conversationApi.listConversations(baseUrl)

    suspend fun getMessages(baseUrl: String, conversationId: String): List<MessageInfo> =
        conversationApi.getMessages(baseUrl, conversationId)

    suspend fun deleteConversation(baseUrl: String, conversationId: String) =
        conversationApi.deleteConversation(baseUrl, conversationId)
}
