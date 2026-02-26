package com.example.aitemplate.client.data.remote

import com.example.aitemplate.client.data.model.ConversationInfo
import com.example.aitemplate.client.data.model.MessageInfo
import com.russhwolf.settings.Settings
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class ConversationApi(private val client: HttpClient) {

    private fun getAuthHeaders(): Map<String, String> {
        val settings = Settings()
        val token = settings.getStringOrNull("access_token")
        return if (token != null) {
            mapOf("Authorization" to "Bearer $token")
        } else {
            emptyMap()
        }
    }

    suspend fun listConversations(baseUrl: String): List<ConversationInfo> =
        client.get("$baseUrl/api/conversations") {
            getAuthHeaders().forEach { (key, value) ->
                header(key, value)
            }
        }.body()

    suspend fun getMessages(baseUrl: String, conversationId: String): List<MessageInfo> =
        client.get("$baseUrl/api/conversations/$conversationId/messages") {
            getAuthHeaders().forEach { (key, value) ->
                header(key, value)
            }
        }.body()

    suspend fun deleteConversation(baseUrl: String, conversationId: String) {
        client.delete("$baseUrl/api/conversations/$conversationId") {
            getAuthHeaders().forEach { (key, value) ->
                header(key, value)
            }
        }
    }
}
