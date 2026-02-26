package com.example.aitemplate.client.ui.screen.auth

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.example.aitemplate.client.data.model.LoginResponse
import com.example.aitemplate.client.data.remote.AuthApi
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginScreenModel(
    private val authApi: AuthApi
) : ScreenModel {

    private val settings = Settings()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password

    private val _serverUrl = MutableStateFlow(settings.getString("server_url", ""))
    val serverUrl: StateFlow<String> = _serverUrl

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _loginSuccess = MutableStateFlow(false)
    val loginSuccess: StateFlow<Boolean> = _loginSuccess

    fun updateUsername(value: String) {
        _username.value = value
        _errorMessage.value = null
    }

    fun updatePassword(value: String) {
        _password.value = value
        _errorMessage.value = null
    }

    fun updateServerUrl(value: String) {
        _serverUrl.value = value
        settings["server_url"] = value
        _errorMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun login() {
        if (_username.value.isBlank() || _password.value.isBlank()) {
            _errorMessage.value = "请输入用户名和密码"
            return
        }

        if (_serverUrl.value.isBlank()) {
            _errorMessage.value = "请先配置服务器地址"
            return
        }

        screenModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val response = authApi.login(
                    baseUrl = _serverUrl.value.trimEnd('/'),
                    username = _username.value,
                    password = _password.value
                )

                // Save auth data
                saveAuthData(response)

                _loginSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = "登录失败: ${e.message ?: "未知错误"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun saveAuthData(response: LoginResponse) {
        settings["access_token"] = response.accessToken
        settings["refresh_token"] = response.refreshToken
        settings["token_type"] = response.tokenType
        settings["expires_in"] = response.expiresIn.toString()
        settings["user_id"] = response.user.id.toString()
        settings["user_name"] = response.user.name
        settings["user_username"] = response.user.username
        settings["username"] = response.user.name  // For display in UI
        
        // Save roles as comma-separated string
        val roles = response.user.roles.joinToString(",") { it.roleCode }
        settings["user_roles"] = roles
    }

    fun hasSavedAuth(): Boolean {
        return settings.getStringOrNull("access_token") != null &&
               settings.getStringOrNull("server_url") != null
    }

    fun clearSavedAuth() {
        settings.remove("access_token")
        settings.remove("refresh_token")
        settings.remove("token_type")
        settings.remove("expires_in")
        settings.remove("user_id")
        settings.remove("user_name")
        settings.remove("user_username")
        settings.remove("user_roles")
    }
}
