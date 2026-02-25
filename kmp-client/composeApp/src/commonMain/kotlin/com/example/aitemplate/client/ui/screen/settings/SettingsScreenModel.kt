package com.example.aitemplate.client.ui.screen.settings

import cafe.adriel.voyager.core.model.ScreenModel
import com.example.aitemplate.client.data.remote.MetadataApi
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SettingsScreenModel(private val settings: Settings) : ScreenModel, KoinComponent {

    companion object {
        const val KEY_SERVER_URL = "server_url"
        const val DEFAULT_SERVER_URL = "http://localhost:8080"
    }

    private val metadataApi: MetadataApi by inject()

    private val _serverUrl = MutableStateFlow(settings.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL))
    val serverUrl = _serverUrl.asStateFlow()

    private val _testResult = MutableStateFlow<TestResult>(TestResult.Idle)
    val testResult = _testResult.asStateFlow()

    fun updateServerUrl(url: String) {
        _serverUrl.value = url
    }

    fun saveAndTest() {
        val url = _serverUrl.value.trimEnd('/')
        _serverUrl.value = url
        settings.putString(KEY_SERVER_URL, url)
        _testResult.value = TestResult.Testing
    }

    suspend fun testConnection(): Boolean {
        return try {
            _testResult.value = TestResult.Testing
            val models = metadataApi.getModels(_serverUrl.value)
            _testResult.value = TestResult.Success(models.size)
            true
        } catch (e: Exception) {
            _testResult.value = TestResult.Error(e.message ?: "Connection failed")
            false
        }
    }

    fun getSavedUrl(): String =
        settings.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL)

    fun hasSavedUrl(): Boolean =
        settings.hasKey(KEY_SERVER_URL)
}

sealed class TestResult {
    data object Idle : TestResult()
    data object Testing : TestResult()
    data class Success(val modelCount: Int) : TestResult()
    data class Error(val message: String) : TestResult()
}
