package echo.ai

import echo.shared.EnvFile
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.config.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.Logger

@Serializable
data class ComplaintAnalysisRequest(val text: String)

@Serializable
data class ComplaintAnalysisResponse(
    val category: String,
    val urgency: String,
    val suggestedPriority: String,
    val summary: String,
    val reasoning: String
)

class GeminiConfigurationException(message: String) : IllegalStateException(message)

class GeminiUpstreamException(message: String) : IllegalStateException(message)

class GeminiService(
    private val httpClient: HttpClient,
    private val config: ApplicationConfig,
    private val log: Logger
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val model: String = config.propertyOrNull("gemini.model")?.getString() ?: "gemini-2.0-flash"
    private val apiKeyEnvVar: String =
        config.propertyOrNull("gemini.apiKeyEnv")?.getString() ?: "GEMINI_API_KEY"

    suspend fun analyzeComplaint(text: String): ComplaintAnalysisResponse {
        val apiKey = EnvFile.get(apiKeyEnvVar)
            ?: throw GeminiConfigurationException(
                "Gemini API key missing. Add $apiKeyEnvVar to echo-backend/.env."
            )

        val response = httpClient.post("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent") {
            parameter("key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(
                GeminiGenerateContentRequest(
                    systemInstruction = GeminiContent(
                        parts = listOf(
                            GeminiPart(
                                """
                                You classify local-governance complaints into structured JSON.
                                Return JSON only with keys:
                                category, urgency, suggestedPriority, summary, reasoning.
                                Use one category from [Water, Road, Sanitation, Electricity, Health, Governance, Other].
                                Use one urgency from [Low, Medium, High, Critical].
                                Use one suggestedPriority from [Low, Medium, High, Critical].
                                Keep summary and reasoning concise.
                                """.trimIndent()
                            )
                        )
                    ),
                    contents = listOf(
                        GeminiContent(
                            role = "user",
                            parts = listOf(GeminiPart(text))
                        )
                    )
                )
            )
        }

        if (!response.status.isSuccess()) {
            val body = response.body<String>()
            log.error("Gemini request failed with status {}: {}", response.status, body)
            throw GeminiUpstreamException("Gemini returned ${response.status.value}.")
        }

        val geminiResponse = response.body<GeminiGenerateContentResponse>()
        val payload = geminiResponse.candidates
            .firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.text
            ?.cleanJsonPayload()
            ?: throw GeminiUpstreamException("Gemini returned an empty response.")

        return try {
            json.decodeFromString<ComplaintAnalysisResponse>(payload)
        } catch (exception: Exception) {
            log.error("Failed to parse Gemini response: {}", payload, exception)
            throw GeminiUpstreamException("Gemini returned an invalid response payload.")
        }
    }
}

private fun String.cleanJsonPayload(): String = trim()
    .removePrefix("```json")
    .removePrefix("```")
    .removeSuffix("```")
    .trim()

@Serializable
private data class GeminiGenerateContentRequest(
    val systemInstruction: GeminiContent,
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig = GeminiGenerationConfig()
)

@Serializable
private data class GeminiGenerationConfig(
    @SerialName("responseMimeType")
    val responseMimeType: String = "application/json",
    val temperature: Double = 0.2
)

@Serializable
private data class GeminiGenerateContentResponse(
    val candidates: List<GeminiCandidate> = emptyList()
)

@Serializable
private data class GeminiCandidate(
    val content: GeminiContent? = null
)

@Serializable
private data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String? = null
)

@Serializable
private data class GeminiPart(
    val text: String
)
