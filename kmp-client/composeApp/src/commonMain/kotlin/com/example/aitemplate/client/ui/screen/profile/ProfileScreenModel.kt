package com.example.aitemplate.client.ui.screen.profile

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.example.aitemplate.client.data.model.UpdateUserRequest
import com.example.aitemplate.client.data.model.UserInfo
import com.example.aitemplate.client.data.remote.AuthApi
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileScreenModel(
    private val authApi: AuthApi
) : ScreenModel {

    private val settings = Settings()

    private val _user = MutableStateFlow<UserInfo?>(null)
    val user: StateFlow<UserInfo?> = _user

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _logoutSuccess = MutableStateFlow(false)
    val logoutSuccess: StateFlow<Boolean> = _logoutSuccess

    fun loadUser() {
        screenModelScope.launch {
            _isLoading.value = true
            try {
                val baseUrl = settings.getStringOrNull("server_url") ?: return@launch
                val userInfo = authApi.getCurrentUser(baseUrl)
                _user.value = userInfo
            } catch (e: Exception) {
                _message.value = "Failed to load user: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateProfile(name: String, email: String?, phone: String?, gender: Int?) {
        screenModelScope.launch {
            _isLoading.value = true
            try {
                val baseUrl = settings.getStringOrNull("server_url") ?: return@launch
                val request = UpdateUserRequest(
                    name = name,
                    email = email,
                    phone = phone,
                    gender = gender
                )
                val updatedUser = authApi.updateUser(baseUrl, request)
                _user.value = updatedUser
                _message.value = "Profile updated successfully"
            } catch (e: Exception) {
                _message.value = "Failed to update profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun changePassword(oldPassword: String, newPassword: String) {
        screenModelScope.launch {
            _isLoading.value = true
            try {
                val baseUrl = settings.getStringOrNull("server_url") ?: return@launch
                authApi.changePassword(baseUrl, oldPassword, newPassword)
                _message.value = "Password changed successfully"
            } catch (e: Exception) {
                _message.value = "Failed to change password: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        screenModelScope.launch {
            _isLoading.value = true
            try {
                val baseUrl = settings.getStringOrNull("server_url")
                if (baseUrl != null) {
                    authApi.logout(baseUrl)
                }
            } catch (e: Exception) {
                // Ignore error, still clear local data
            } finally {
                // Clear all auth data
                settings.remove("access_token")
                settings.remove("refresh_token")
                settings.remove("user_id")
                settings.remove("username")
                _logoutSuccess.value = true
                _isLoading.value = false
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
