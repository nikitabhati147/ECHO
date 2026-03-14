package echo.auth

import com.mongodb.ErrorCategory
import com.mongodb.MongoWriteException
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import echo.shared.AuthenticationException
import echo.shared.NotFoundException
import echo.shared.ValidationException
import echo.shared.toObjectIdOrNull
import io.ktor.server.config.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.Document
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

class AuthService(
    database: MongoDatabase,
    private val jwtConfig: JwtConfig,
    private val config: ApplicationConfig
) {
    private val users: MongoCollection<Document> = database.getCollection("users")

    init {
        users.createIndex(Indexes.ascending("email"), IndexOptions().unique(true))
        seedLeaderIfConfigured()
    }

    suspend fun signup(request: SignupRequest): AuthResponse = withContext(Dispatchers.IO) {
        val normalizedEmail = request.email.trim().lowercase()
        validateSignup(request.name, normalizedEmail, request.password)

        val now = System.currentTimeMillis()
        val document = Document()
            .append("name", request.name.trim())
            .append("email", normalizedEmail)
            .append("passwordHash", hashPassword(request.password))
            .append("role", UserRole.CITIZEN.name)
            .append("createdAt", now)

        try {
            users.insertOne(document)
        } catch (exception: MongoWriteException) {
            if (exception.error.category == ErrorCategory.DUPLICATE_KEY) {
                throw ValidationException("An account with that email already exists.")
            }
            throw exception
        }

        val user = document.toUserRecord()
        issueAuthResponse(user)
    }

    suspend fun login(request: LoginRequest): AuthResponse = withContext(Dispatchers.IO) {
        val normalizedEmail = request.email.trim().lowercase()
        val user = users.find(Filters.eq("email", normalizedEmail)).first()?.toUserRecord()
            ?: throw AuthenticationException("Invalid email or password.")

        if (!verifyPassword(request.password, user.passwordHash)) {
            throw AuthenticationException("Invalid email or password.")
        }

        issueAuthResponse(user)
    }

    suspend fun getUserById(id: String): UserResponse = withContext(Dispatchers.IO) {
        val objectId = id.toObjectIdOrNull() ?: throw NotFoundException("User not found.")
        val user = users.find(Filters.eq("_id", objectId)).first()?.toUserRecord()
            ?: throw NotFoundException("User not found.")
        user.toResponse()
    }

    private fun issueAuthResponse(user: UserRecord): AuthResponse {
        val authenticatedUser = AuthenticatedUser(
            id = user.id,
            email = user.email,
            name = user.name,
            role = user.role
        )
        return AuthResponse(
            token = jwtConfig.buildToken(authenticatedUser),
            user = user.toResponse()
        )
    }

    private fun seedLeaderIfConfigured() {
        val leaderEmail = config.propertyOrNull("seed.leader.email")?.getString()?.trim()?.lowercase().orEmpty()
        val leaderPassword = config.propertyOrNull("seed.leader.password")?.getString().orEmpty()
        val leaderName = config.propertyOrNull("seed.leader.name")?.getString()?.trim().orEmpty()

        if (leaderEmail.isBlank() || leaderPassword.isBlank() || leaderName.isBlank()) {
            return
        }

        val existing = users.find(Filters.eq("email", leaderEmail)).first()
        if (existing != null) {
            return
        }

        val now = System.currentTimeMillis()
        val leader = Document()
            .append("name", leaderName)
            .append("email", leaderEmail)
            .append("passwordHash", hashPassword(leaderPassword))
            .append("role", UserRole.LEADER.name)
            .append("createdAt", now)
        users.insertOne(leader)
    }

    private fun validateSignup(name: String, email: String, password: String) {
        if (name.trim().length < 2) {
            throw ValidationException("Name must be at least 2 characters long.")
        }
        if (email.isBlank() || "@" !in email) {
            throw ValidationException("Enter a valid email address.")
        }
        if (password.length < 8) {
            throw ValidationException("Password must be at least 8 characters long.")
        }
    }
}

data class UserRecord(
    val id: String,
    val name: String,
    val email: String,
    val passwordHash: String,
    val role: UserRole,
    val createdAt: Long
)

private fun Document.toUserRecord(): UserRecord = UserRecord(
    id = getObjectId("_id").toHexString(),
    name = getString("name"),
    email = getString("email"),
    passwordHash = getString("passwordHash"),
    role = UserRole.valueOf(getString("role")),
    createdAt = getLong("createdAt")
)

private fun UserRecord.toResponse(): UserResponse = UserResponse(
    id = id,
    name = name,
    email = email,
    role = role.name,
    createdAt = createdAt
)

private fun hashPassword(password: String): String {
    val salt = ByteArray(16)
    SecureRandom().nextBytes(salt)
    val hash = sha256(salt + password.toByteArray())
    return "${Base64.getEncoder().encodeToString(salt)}:${Base64.getEncoder().encodeToString(hash)}"
}

private fun verifyPassword(password: String, storedHash: String): Boolean {
    val parts = storedHash.split(":")
    if (parts.size != 2) {
        return false
    }

    val salt = Base64.getDecoder().decode(parts[0])
    val expectedHash = Base64.getDecoder().decode(parts[1])
    val actualHash = sha256(salt + password.toByteArray())
    return MessageDigest.isEqual(expectedHash, actualHash)
}

private fun sha256(input: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(input)
