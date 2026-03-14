package echo.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.auth.jwt.*
import io.ktor.server.config.*
import java.util.Date

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String,
    val expiresInMs: Long
) {
    companion object {
        fun from(config: ApplicationConfig): JwtConfig = JwtConfig(
            secret = config.property("jwt.secret").getString(),
            issuer = config.property("jwt.issuer").getString(),
            audience = config.property("jwt.audience").getString(),
            realm = config.property("jwt.realm").getString(),
            expiresInMs = config.propertyOrNull("jwt.expiresInMs")?.getString()?.toLong() ?: 86_400_000L
        )
    }
}

data class AuthenticatedUser(
    val id: String,
    val email: String,
    val name: String,
    val role: UserRole
)

fun JwtConfig.buildToken(user: AuthenticatedUser): String {
    val now = System.currentTimeMillis()
    return JWT.create()
        .withIssuer(issuer)
        .withAudience(audience)
        .withClaim("userId", user.id)
        .withClaim("email", user.email)
        .withClaim("name", user.name)
        .withClaim("role", user.role.name)
        .withIssuedAt(Date(now))
        .withExpiresAt(Date(now + expiresInMs))
        .sign(Algorithm.HMAC256(secret))
}

fun JWTPrincipal.toAuthenticatedUser(): AuthenticatedUser {
    val payload = payload
    return AuthenticatedUser(
        id = payload.getClaim("userId").asString(),
        email = payload.getClaim("email").asString(),
        name = payload.getClaim("name").asString(),
        role = UserRole.valueOf(payload.getClaim("role").asString())
    )
}
