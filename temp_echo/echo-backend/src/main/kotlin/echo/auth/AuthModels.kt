package echo.auth

import kotlinx.serialization.Serializable

enum class UserRole {
    CITIZEN,
    LEADER
}

@Serializable
data class SignupRequest(
    val name: String,
    val email: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: UserResponse
)

@Serializable
data class UserResponse(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val createdAt: Long
)
