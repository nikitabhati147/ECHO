package echo.config

import com.mongodb.client.MongoDatabase
import echo.ai.GeminiService
import echo.auth.AuthService
import echo.auth.JwtConfig
import echo.complaints.ComplaintService
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureFrameworks() {
    install(Koin) {
        slf4jLogger()
        modules(
            module {
                single {
                    HttpClient(CIO) {
                        install(ContentNegotiation) {
                            json(
                                Json {
                                    ignoreUnknownKeys = true
                                    explicitNulls = false
                                }
                            )
                        }
                    }
                }
                single { JwtConfig.from(environment.config) }
                single<MongoDatabase> { connectToMongoDB() }
                single { GeminiService(get(), environment.config, log) }
                single { AuthService(get(), get(), environment.config) }
                single { ComplaintService(get(), get()) }
            }
        )
    }
}
