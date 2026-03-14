package echo.complaints

import kotlinx.serialization.Serializable

enum class ComplaintStatus {
    NEW,
    IN_REVIEW,
    IN_PROGRESS,
    RESOLVED
}

@Serializable
data class CreateComplaintRequest(
    val text: String,
    val locationText: String? = null,
    val contactPhone: String? = null
)

@Serializable
data class UpdateComplaintStatusRequest(
    val status: ComplaintStatus
)

@Serializable
data class ResolveComplaintRequest(
    val resolutionNote: String
)

@Serializable
data class ComplaintResponse(
    val id: String,
    val citizenId: String,
    val citizenName: String,
    val text: String,
    val locationText: String? = null,
    val contactPhone: String? = null,
    val category: String,
    val urgency: String,
    val suggestedPriority: String,
    val summary: String,
    val reasoning: String,
    val status: ComplaintStatus,
    val resolutionNote: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val history: List<ComplaintStatusHistoryResponse>
)

@Serializable
data class ComplaintStatusHistoryResponse(
    val status: ComplaintStatus,
    val changedByUserId: String,
    val changedByName: String,
    val changedByRole: String,
    val changedAt: Long
)
