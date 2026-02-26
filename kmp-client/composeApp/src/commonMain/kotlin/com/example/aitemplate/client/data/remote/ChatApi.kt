package com.example.aitemplate.client.data.remote

import com.example.aitemplate.client.data.model.ChatRequest
import com.example.aitemplate.client.data.model.ChatResponse
import com.russhwolf.settings.Settings
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class ChatApi(private val client: HttpClient) {

    private fun getAuthHeaders(): Map<String, String> {
        val settings = Settings()
        val token = settings.getStringOrNull("access_token")
        return if (token != null) {
            mapOf("Authorization" to "Bearer $token")
        } else {
            emptyMap()
        }
    }

    suspend fun chatOnce(baseUrl: String, request: ChatRequest): ChatResponse =
        client.post("$baseUrl/api/chat") {
            contentType(ContentType.Application.Json)
            getAuthHeaders().forEach { (key, value) ->
                header(key, value)
            }
            setBody(request)
        }.body()
}
