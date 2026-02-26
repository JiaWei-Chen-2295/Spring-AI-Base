package com.example.aitemplate.client.data.remote

import com.example.aitemplate.client.data.model.ChangePasswordRequest
import com.example.aitemplate.client.data.model.LoginRequest
import com.example.aitemplate.client.data.model.LoginResponse
import com.example.aitemplate.client.data.model.UpdateUserRequest
import com.example.aitemplate.client.data.model.UserInfo
import com.russhwolf.settings.Settings
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class AuthApi(private val client: HttpClient) {

    private fun getAuthHeaders(): Map<String, String> {
        val settings = Settings()
        val token = settings.getStringOrNull("access_token")
        return if (token != null) {
            mapOf("Authorization" to "Bearer $token")
        } else {
            emptyMap()
        }
    }

    suspend fun login(baseUrl: String, username: String, password: String): LoginResponse =
        client.post("$baseUrl/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username, password))
        }.body()

    suspend fun logout(baseUrl: String) {
        client.post("$baseUrl/api/auth/logout") {
            getAuthHeaders().forEach { (key, value) ->
                header(key, value)
            }
        }
    }

    suspend fun refresh(baseUrl: String, refreshToken: String): LoginResponse =
        client.post("$baseUrl/api/auth/refresh") {
            contentType(ContentType.Text.Plain)
            setBody(refreshToken)
        }.body()

    suspend fun getCurrentUser(baseUrl: String): UserInfo =
        client.get("$baseUrl/api/auth/me") {
            getAuthHeaders().forEach { (key, value) ->
                header(key, value)
            }
        }.body()

    suspend fun changePassword(
        baseUrl: String,
        oldPassword: String,
        newPassword: String
    ) {
        client.put("$baseUrl/api/auth/password") {
            contentType(ContentType.Application.Json)
            getAuthHeaders().forEach { (key, value) ->
                header(key, value)
            }
            setBody(ChangePasswordRequest(oldPassword, newPassword))
        }
    }

    suspend fun updateUser(
        baseUrl: String,
        request: UpdateUserRequest
    ): UserInfo =
        client.put("$baseUrl/api/auth/profile") {
            contentType(ContentType.Application.Json)
            getAuthHeaders().forEach { (key, value) ->
                header(key, value)
            }
            setBody(request)
        }.body()
}
