package com.example.aitemplate.client.data.remote

import com.example.aitemplate.client.data.model.ConversationInfo
import com.example.aitemplate.client.data.model.MessageInfo
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class ConversationApi(private val client: HttpClient) {

    suspend fun listConversations(baseUrl: String): List<ConversationInfo> =
        client.get("$baseUrl/api/conversations").body()

    suspend fun getMessages(baseUrl: String, conversationId: String): List<MessageInfo> =
        client.get("$baseUrl/api/conversations/$conversationId/messages").body()

    suspend fun deleteConversation(baseUrl: String, conversationId: String) {
        client.delete("$baseUrl/api/conversations/$conversationId")
    }
}
