package com.example.aitemplate.client.data.repository

import com.example.aitemplate.client.data.model.ModelInfo
import com.example.aitemplate.client.data.model.SkillInfo
import com.example.aitemplate.client.data.model.ToolInfo
import com.example.aitemplate.client.data.remote.MetadataApi

class MetadataRepository(private val api: MetadataApi) {

    private var cachedModels: List<ModelInfo>? = null
    private var cachedTools: List<ToolInfo>? = null
    private var cachedSkills: List<SkillInfo>? = null

    suspend fun getModels(baseUrl: String, refresh: Boolean = false): List<ModelInfo> {
        if (refresh || cachedModels == null) {
            cachedModels = api.getModels(baseUrl)
        }
        return cachedModels!!
    }

    suspend fun getTools(baseUrl: String, refresh: Boolean = false): List<ToolInfo> {
        if (refresh || cachedTools == null) {
            cachedTools = api.getTools(baseUrl)
        }
        return cachedTools!!
    }

    suspend fun getSkills(baseUrl: String, refresh: Boolean = false): List<SkillInfo> {
        if (refresh || cachedSkills == null) {
            cachedSkills = api.getSkills(baseUrl)
        }
        return cachedSkills!!
    }

    fun clearCache() {
        cachedModels = null
        cachedTools = null
        cachedSkills = null
    }
}
