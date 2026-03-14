package echo.api

import echo.ai.ComplaintAnalysisRequest
import echo.ai.GeminiConfigurationException
import echo.ai.GeminiService
import echo.ai.GeminiUpstreamException
import echo.auth.AuthService
import echo.auth.AuthenticatedUser
import echo.auth.LoginRequest
import echo.auth.SignupRequest
import echo.auth.UserRole
import echo.auth.toAuthenticatedUser
import echo.complaints.ComplaintService
import echo.complaints.CreateComplaintRequest
import echo.complaints.ResolveComplaintRequest
import echo.complaints.UpdateComplaintStatusRequest
import echo.shared.ApiStatusResponse
import echo.shared.AuthenticationException
import echo.shared.AuthorizationException
import echo.shared.ErrorResponse
import echo.shared.NotFoundException
import echo.shared.ValidationException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    val geminiService by inject<GeminiService>()
    val authService by inject<AuthService>()
    val complaintService by inject<ComplaintService>()

    install(StatusPages) {
        exception<ValidationException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: "Invalid request."))
        }
        exception<AuthenticationException> { call, cause ->
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse(cause.message ?: "Authentication failed."))
        }
        exception<AuthorizationException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden, ErrorResponse(cause.message ?: "You are not allowed to do that."))
        }
        exception<NotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, ErrorResponse(cause.message ?: "Resource not found."))
        }
        exception<GeminiConfigurationException> { call, cause ->
            call.respond(
                HttpStatusCode.ServiceUnavailable,
                ErrorResponse(cause.message ?: "Gemini API key is not configured.")
            )
        }
        exception<GeminiUpstreamException> { call, cause ->
            call.respond(
                HttpStatusCode.BadGateway,
                ErrorResponse(cause.message ?: "Gemini request failed.")
            )
        }
        exception<Throwable> { call, cause ->
            this@configureRouting.log.error("Unhandled request failure", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("Unexpected server error.")
            )
        }
    }

    routing {
        get("/") {
            call.respond(ApiStatusResponse(name = "ECHO API", status = "ok"))
        }

        get("/health") {
            call.respond(ApiStatusResponse(name = "ECHO API", status = "healthy"))
        }

        route("/auth") {
            post("/signup") {
                val request = call.receive<SignupRequest>()
                call.respond(HttpStatusCode.Created, authService.signup(request))
            }

            post("/login") {
                val request = call.receive<LoginRequest>()
                call.respond(authService.login(request))
            }
        }

        route("/ai") {
            post("/complaints/analyze") {
                val request = call.receive<ComplaintAnalysisRequest>()
                if (request.text.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Complaint text is required."))
                    return@post
                }
                call.respond(geminiService.analyzeComplaint(request.text))
            }
        }

        authenticate("auth-jwt") {
            get("/auth/me") {
                val currentUser = call.requireCurrentUser()
                call.respond(authService.getUserById(currentUser.id))
            }

            route("/complaints") {
                post {
                    val currentUser = call.requireCurrentUser()
                    if (currentUser.role != UserRole.CITIZEN) {
                        throw AuthorizationException("Only citizens can submit complaints.")
                    }

                    val request = call.receive<CreateComplaintRequest>()
                    call.respond(HttpStatusCode.Created, complaintService.createComplaint(request, currentUser))
                }

                get {
                    val currentUser = call.requireCurrentUser()
                    call.respond(complaintService.listComplaints(currentUser))
                }

                get("/{id}") {
                    val currentUser = call.requireCurrentUser()
                    val id = call.parameters["id"] ?: throw NotFoundException("Complaint not found.")
                    call.respond(complaintService.getComplaint(id, currentUser))
                }

                patch("/{id}/status") {
                    val currentUser = call.requireCurrentUser()
                    val id = call.parameters["id"] ?: throw NotFoundException("Complaint not found.")
                    val request = call.receive<UpdateComplaintStatusRequest>()
                    call.respond(complaintService.updateStatus(id, request, currentUser))
                }

                patch("/{id}/resolve") {
                    val currentUser = call.requireCurrentUser()
                    val id = call.parameters["id"] ?: throw NotFoundException("Complaint not found.")
                    val request = call.receive<ResolveComplaintRequest>()
                    call.respond(complaintService.resolveComplaint(id, request, currentUser))
                }
            }
        }
    }
}

private fun ApplicationCall.requireCurrentUser(): AuthenticatedUser {
    val principal = principal<JWTPrincipal>() ?: throw AuthenticationException("Missing JWT principal.")
    return principal.toAuthenticatedUser()
}
