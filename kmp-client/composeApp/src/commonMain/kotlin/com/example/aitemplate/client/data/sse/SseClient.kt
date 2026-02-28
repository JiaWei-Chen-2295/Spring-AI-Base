package com.example.aitemplate.client.data.sse

import com.example.aitemplate.client.data.model.SkillApplyInfo
import com.example.aitemplate.client.data.model.SseEvent
import com.example.aitemplate.client.data.model.ToolCallInfo
import com.russhwolf.settings.Settings
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.*

class SseClient(private val httpClient: HttpClient) {

    private val json = Json { ignoreUnknownKeys = true }

    fun streamChat(
        baseUrl: String,
        conversationId: String,
        model: String,
        message: String,
        tools: List<String> = emptyList(),
        skills: List<String> = emptyList()
    ): Flow<SseEvent> = callbackFlow {
        val url = buildStreamUrl(baseUrl, conversationId, model, message, tools, skills)

        val statement = httpClient.prepareGet(url) {
            accept(ContentType.Text.EventStream)
            headers {
                append(HttpHeaders.CacheControl, "no-cache")
                append(HttpHeaders.Connection, "keep-alive")
            }
        }

        statement.execute { response: HttpResponse ->
            val channel: ByteReadChannel = response.bodyAsChannel()
            var eventType = ""
            var data = ""

            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break

                when {
                    line.startsWith("event:") -> {
                        eventType = line.removePrefix("event:").trim()
                    }
                    line.startsWith("data:") -> {
                        data = line.removePrefix("data:").trim()
                    }
                    line.isEmpty() && data.isNotEmpty() -> {
                        val event = parseEvent(eventType, data)
                        if (event != null) {
                            trySend(event)
                        }
                        if (event is SseEvent.Done) break
                        eventType = ""
                        data = ""
                    }
                }
            }
        }

        close()
        awaitClose()
    }

    private fun buildStreamUrl(
        baseUrl: String,
        conversationId: String,
        model: String,
        message: String,
        tools: List<String>,
        skills: List<String>
    ): String {
        val params = ParametersBuilder().apply {
            append("conversationId", conversationId)
            append("model", model)
            append("message", message)
            tools.forEach { append("tools", it) }
            skills.forEach { append("skills", it) }
            // SSE cannot set headers; pass JWT as query param for auth-enabled=true
            val token = Settings().getStringOrNull("access_token")
            if (token != null) append("token", token)
        }
        return "$baseUrl/api/chat/stream?${params.build().formUrlEncode()}"
    }

    private fun parseEvent(type: String, data: String): SseEvent? {
        return try {
            when (type) {
                "token" -> {
                    val jsonObj = json.parseToJsonElement(data).jsonObject
                    val tokenText = jsonObj["token"]?.jsonPrimitive?.content ?: data
                    SseEvent.Token(tokenText)
                }
                "tool_call" -> {
                    val info = json.decodeFromString<ToolCallInfo>(data)
                    SseEvent.ToolCall(info)
                }
                "tool_call_progress" -> {
                    val info = json.decodeFromString<ToolCallInfo>(data)
                    SseEvent.ToolCallProgress(info)
                }
                "skill_apply" -> {
                    val skills = json.decodeFromString<List<SkillApplyInfo>>(data)
                    SseEvent.SkillApply(skills)
                }
                "error" -> SseEvent.Error(data)
                "done" -> SseEvent.Done
                else -> null
            }
        } catch (e: Exception) {
            // If JSON parsing fails for token, treat raw data as text
            if (type == "token") SseEvent.Token(data) else null
        }
    }
}
