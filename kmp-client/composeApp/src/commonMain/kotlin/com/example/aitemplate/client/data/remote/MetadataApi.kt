package com.example.aitemplate.client.data.remote

import com.example.aitemplate.client.data.model.ModelInfo
import com.example.aitemplate.client.data.model.SkillInfo
import com.example.aitemplate.client.data.model.ToolInfo
import com.russhwolf.settings.Settings
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class MetadataApi(private val client: HttpClient) {

    private fun getAuthHeaders(): Map<String, String> {
        val settings = Settings()
        val token = settings.getStringOrNull("access_token")
        return if (token != null) {
            mapOf("Authorization" to "Bearer $token")
        } else {
            emptyMap()
        }
    }

    suspend fun getModels(baseUrl: String): List<ModelInfo> =
        client.get("$baseUrl/api/models") {
            getAuthHeaders().forEach { (key, value) ->
                header(key, value)
            }
        }.body()

    suspend fun getTools(baseUrl: String): List<ToolInfo> =
        client.get("$baseUrl/api/tools") {
            getAuthHeaders().forEach { (key, value) ->
                header(key, value)
            }
        }.body()

    suspend fun getSkills(baseUrl: String): List<SkillInfo> =
        client.get("$baseUrl/api/skills") {
            getAuthHeaders().forEach { (key, value) ->
                header(key, value)
            }
        }.body()
}
