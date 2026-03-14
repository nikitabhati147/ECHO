package echo.config

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import echo.shared.EnvFile
import io.ktor.server.application.*
import io.ktor.server.config.*

fun Application.configureDatabases() {
    val databaseName = resolveMongoDatabaseName()
    val source = if (EnvFile.get("MONGODB_URI") != null) ".env/system MONGODB_URI" else "application.yaml"
    log.info("MongoDB configured for database '{}' via {}", databaseName, source)
}

fun Application.connectToMongoDB(): MongoDatabase {
    val databaseName = resolveMongoDatabaseName()
    val uri = resolveMongoUri()

    val mongoClient = MongoClients.create(uri)
    val database = mongoClient.getDatabase(databaseName)

    monitor.subscribe(ApplicationStopped) {
        mongoClient.close()
    }

    return database
}

private fun Application.resolveMongoDatabaseName(): String =
    EnvFile.get("MONGODB_DATABASE")
        ?: environment.config.tryGetString("db.mongo.database.name")
        ?: "echo"

private fun Application.resolveMongoUri(): String {
    EnvFile.get("MONGODB_URI")?.let { return it }

    val user = environment.config.tryGetString("db.mongo.user")
    val password = environment.config.tryGetString("db.mongo.password")
    val host = environment.config.tryGetString("db.mongo.host") ?: "127.0.0.1"
    val port = environment.config.tryGetString("db.mongo.port") ?: "27017"
    val maxPoolSize = environment.config.tryGetString("db.mongo.maxPoolSize")?.toInt() ?: 20

    val credentials = user
        ?.takeIf { password != null }
        ?.let { "$it:$password@" }
        .orEmpty()

    return "mongodb://$credentials$host:$port/?maxPoolSize=$maxPoolSize&w=majority"
}
