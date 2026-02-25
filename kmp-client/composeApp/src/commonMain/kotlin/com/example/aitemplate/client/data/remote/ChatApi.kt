package com.example.aitemplate.client.data.remote

import com.example.aitemplate.client.data.model.ChatRequest
import com.example.aitemplate.client.data.model.ChatResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class ChatApi(private val client: HttpClient) {

    suspend fun chatOnce(baseUrl: String, request: ChatRequest): ChatResponse =
        client.post("$baseUrl/api/chat") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
}
