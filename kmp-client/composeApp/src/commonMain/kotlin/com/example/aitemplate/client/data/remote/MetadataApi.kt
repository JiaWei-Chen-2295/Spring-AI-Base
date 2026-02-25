package com.example.aitemplate.client.data.remote

import com.example.aitemplate.client.data.model.ModelInfo
import com.example.aitemplate.client.data.model.SkillInfo
import com.example.aitemplate.client.data.model.ToolInfo
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class MetadataApi(private val client: HttpClient) {

    suspend fun getModels(baseUrl: String): List<ModelInfo> =
        client.get("$baseUrl/api/models").body()

    suspend fun getTools(baseUrl: String): List<ToolInfo> =
        client.get("$baseUrl/api/tools").body()

    suspend fun getSkills(baseUrl: String): List<SkillInfo> =
        client.get("$baseUrl/api/skills").body()
}
