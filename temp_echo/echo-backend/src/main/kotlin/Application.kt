package echo

import echo.api.configureRouting
import echo.config.configureDatabases
import echo.config.configureFrameworks
import echo.config.configureHTTP
import echo.config.configureMonitoring
import echo.config.configureSecurity
import echo.config.configureSerialization
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureHTTP()
    configureMonitoring()
    configureSerialization()
    configureSecurity()
    configureFrameworks()
    configureDatabases()
    configureRouting()
}
