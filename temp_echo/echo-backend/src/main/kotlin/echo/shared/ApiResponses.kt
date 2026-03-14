package echo.shared

import kotlinx.serialization.Serializable

@Serializable
data class ApiStatusResponse(
    val name: String,
    val status: String
)

@Serializable
data class ErrorResponse(val message: String)

@Serializable
data class MessageResponse(
    val message: String
)
