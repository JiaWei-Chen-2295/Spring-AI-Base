package com.example.aitemplate.client.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val user: UserInfo
)

@Serializable
data class UserInfo(
    val id: Long,
    val username: String,
    val name: String,
    val gender: Int?,
    val phone: String?,
    val email: String?,
    val avatar: String?,
    val status: Int,
    val roles: Set<RoleInfo>
)

@Serializable
data class RoleInfo(
    val id: Long,
    val roleCode: String,
    val roleName: String,
    val description: String?
)

@Serializable
data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)

@Serializable
data class UpdateUserRequest(
    val name: String? = null,
    val gender: Int? = null,
    val phone: String? = null,
    val email: String? = null,
    val avatar: String? = null
)
