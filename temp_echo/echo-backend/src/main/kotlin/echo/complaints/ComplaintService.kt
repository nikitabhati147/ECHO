package echo.complaints

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import echo.ai.GeminiService
import echo.auth.AuthenticatedUser
import echo.auth.UserRole
import echo.shared.AuthorizationException
import echo.shared.NotFoundException
import echo.shared.ValidationException
import echo.shared.toObjectIdOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.Document
import org.bson.types.ObjectId

class ComplaintService(
    database: MongoDatabase,
    private val geminiService: GeminiService
) {
    private val complaints: MongoCollection<Document> = database.getCollection("complaints")

    suspend fun createComplaint(
        request: CreateComplaintRequest,
        user: AuthenticatedUser
    ): ComplaintResponse = withContext(Dispatchers.IO) {
        if (request.text.isBlank()) {
            throw ValidationException("Complaint text is required.")
        }

        val analysis = geminiService.analyzeComplaint(request.text)
        val now = System.currentTimeMillis()
        val history = listOf(
            ComplaintStatusHistory(
                status = ComplaintStatus.NEW,
                changedByUserId = user.id,
                changedByName = user.name,
                changedByRole = user.role,
                changedAt = now
            )
        )

        val document = Document()
            .append("citizenId", ObjectId(user.id))
            .append("citizenName", user.name)
            .append("text", request.text.trim())
            .append("locationText", request.locationText?.trim()?.takeIf { it.isNotEmpty() })
            .append("contactPhone", request.contactPhone?.trim()?.takeIf { it.isNotEmpty() })
            .append("category", analysis.category)
            .append("urgency", analysis.urgency)
            .append("suggestedPriority", analysis.suggestedPriority)
            .append("summary", analysis.summary)
            .append("reasoning", analysis.reasoning)
            .append("status", ComplaintStatus.NEW.name)
            .append("resolutionNote", null)
            .append("createdAt", now)
            .append("updatedAt", now)
            .append("history", history.map { it.toDocument() })

        complaints.insertOne(document)
        document.toComplaintResponse()
    }

    suspend fun listComplaints(user: AuthenticatedUser): List<ComplaintResponse> = withContext(Dispatchers.IO) {
        val filter = if (user.role == UserRole.LEADER) {
            Document()
        } else {
            Filters.eq("citizenId", ObjectId(user.id))
        }

        complaints.find(filter)
            .sort(Sorts.descending("createdAt"))
            .map { it.toComplaintResponse() }
            .toList()
    }

    suspend fun getComplaint(id: String, user: AuthenticatedUser): ComplaintResponse = withContext(Dispatchers.IO) {
        val objectId = id.toObjectIdOrNull() ?: throw NotFoundException("Complaint not found.")
        val complaint = complaints.find(Filters.eq("_id", objectId)).first()
            ?: throw NotFoundException("Complaint not found.")

        authorizeComplaintAccess(complaint, user)
        complaint.toComplaintResponse()
    }

    suspend fun updateStatus(
        id: String,
        request: UpdateComplaintStatusRequest,
        user: AuthenticatedUser
    ): ComplaintResponse = withContext(Dispatchers.IO) {
        if (user.role != UserRole.LEADER) {
            throw AuthorizationException("Only leaders can update complaint status.")
        }

        val complaint = getComplaintDocument(id)
        val history = complaint.readHistory().toMutableList()
        val now = System.currentTimeMillis()
        history += ComplaintStatusHistory(
            status = request.status,
            changedByUserId = user.id,
            changedByName = user.name,
            changedByRole = user.role,
            changedAt = now
        )

        complaint["status"] = request.status.name
        complaint["updatedAt"] = now
        complaint["history"] = history.map { it.toDocument() }
        complaints.replaceOne(Filters.eq("_id", complaint.getObjectId("_id")), complaint)
        complaint.toComplaintResponse()
    }

    suspend fun resolveComplaint(
        id: String,
        request: ResolveComplaintRequest,
        user: AuthenticatedUser
    ): ComplaintResponse = withContext(Dispatchers.IO) {
        if (user.role != UserRole.LEADER) {
            throw AuthorizationException("Only leaders can resolve complaints.")
        }
        if (request.resolutionNote.isBlank()) {
            throw ValidationException("Resolution note is required.")
        }

        val complaint = getComplaintDocument(id)
        val history = complaint.readHistory().toMutableList()
        val now = System.currentTimeMillis()
        history += ComplaintStatusHistory(
            status = ComplaintStatus.RESOLVED,
            changedByUserId = user.id,
            changedByName = user.name,
            changedByRole = user.role,
            changedAt = now
        )

        complaint["status"] = ComplaintStatus.RESOLVED.name
        complaint["resolutionNote"] = request.resolutionNote.trim()
        complaint["updatedAt"] = now
        complaint["history"] = history.map { it.toDocument() }
        complaints.replaceOne(Filters.eq("_id", complaint.getObjectId("_id")), complaint)
        complaint.toComplaintResponse()
    }

    private fun getComplaintDocument(id: String): Document {
        val objectId = id.toObjectIdOrNull() ?: throw NotFoundException("Complaint not found.")
        return complaints.find(Filters.eq("_id", objectId)).first()
            ?: throw NotFoundException("Complaint not found.")
    }

    private fun authorizeComplaintAccess(document: Document, user: AuthenticatedUser) {
        if (user.role == UserRole.LEADER) {
            return
        }

        val citizenId = document.getObjectId("citizenId").toHexString()
        if (citizenId != user.id) {
            throw AuthorizationException("You do not have access to this complaint.")
        }
    }
}

data class ComplaintStatusHistory(
    val status: ComplaintStatus,
    val changedByUserId: String,
    val changedByName: String,
    val changedByRole: UserRole,
    val changedAt: Long
)

private fun ComplaintStatusHistory.toDocument(): Document = Document()
    .append("status", status.name)
    .append("changedByUserId", changedByUserId)
    .append("changedByName", changedByName)
    .append("changedByRole", changedByRole.name)
    .append("changedAt", changedAt)

private fun Document.readHistory(): List<ComplaintStatusHistory> =
    (get("history") as? List<*>)?.mapNotNull { entry ->
        val document = entry as? Document ?: return@mapNotNull null
        ComplaintStatusHistory(
            status = ComplaintStatus.valueOf(document.getString("status")),
            changedByUserId = document.getString("changedByUserId"),
            changedByName = document.getString("changedByName"),
            changedByRole = UserRole.valueOf(document.getString("changedByRole")),
            changedAt = document.getLong("changedAt")
        )
    }.orEmpty()

private fun Document.toComplaintResponse(): ComplaintResponse = ComplaintResponse(
    id = getObjectId("_id").toHexString(),
    citizenId = getObjectId("citizenId").toHexString(),
    citizenName = getString("citizenName"),
    text = getString("text"),
    locationText = getString("locationText"),
    contactPhone = getString("contactPhone"),
    category = getString("category"),
    urgency = getString("urgency"),
    suggestedPriority = getString("suggestedPriority"),
    summary = getString("summary"),
    reasoning = getString("reasoning"),
    status = ComplaintStatus.valueOf(getString("status")),
    resolutionNote = getString("resolutionNote"),
    createdAt = getLong("createdAt"),
    updatedAt = getLong("updatedAt"),
    history = readHistory().map {
        ComplaintStatusHistoryResponse(
            status = it.status,
            changedByUserId = it.changedByUserId,
            changedByName = it.changedByName,
            changedByRole = it.changedByRole.name,
            changedAt = it.changedAt
        )
    }
)
