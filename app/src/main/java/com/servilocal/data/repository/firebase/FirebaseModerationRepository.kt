package com.servilocal.data.repository.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FirebaseFirestoreException
import com.servilocal.domain.exception.DuplicateReportException
import com.servilocal.domain.model.ModerationReportItem
import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.model.ReportResolution
import com.servilocal.domain.model.ReportStatus
import com.servilocal.domain.model.UserRole
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.ModerationRepository
import com.servilocal.domain.repository.PostRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Moderation reports persisted in Firestore. */
@Singleton
class FirebaseModerationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val postRepository: PostRepository
) : ModerationRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val collection get() = firestore.collection(REPORTS_COLLECTION)
    private val _reports = MutableStateFlow<List<ModerationReportItem>>(emptyList())
    override val reports: StateFlow<List<ModerationReportItem>> = _reports.asStateFlow()
    private val _reportHistory = MutableStateFlow<List<ModerationReportItem>>(emptyList())
    override val reportHistory: StateFlow<List<ModerationReportItem>> = _reportHistory.asStateFlow()
    private val registrations = mutableListOf<ListenerRegistration>()

    init {
        scope.launch {
            combine(authRepository.currentUser, postRepository.posts) { user, posts -> user to posts }
                .collect { (user, posts) ->
                registrations.forEach(ListenerRegistration::remove)
                registrations.clear()
                _reports.value = emptyList()
                _reportHistory.value = emptyList()
                if (user.id.isBlank()) return@collect
                if (user.role == UserRole.MODERATOR) {
                    registrations += collection.addSnapshotListener { snapshot, error ->
                        if (error != null || snapshot == null) return@addSnapshotListener
                        publishReports(snapshot.documents.mapNotNull(::toModerationReport))
                    }
                } else {
                    val ownedPostIds = posts.filter { it.providerId == user.id }.map { it.id }.distinct()
                    if (ownedPostIds.isEmpty()) return@collect
                    val reportsByPost = mutableMapOf<String, List<ModerationReportItem>>()
                    ownedPostIds.forEach { postId ->
                        registrations += collection.whereEqualTo("targetPostId", postId)
                            .addSnapshotListener { snapshot, error ->
                                if (error != null || snapshot == null) return@addSnapshotListener
                                reportsByPost[postId] = snapshot.documents.mapNotNull(::toModerationReport)
                                publishReports(reportsByPost.values.flatten())
                            }
                    }
                }
            }
        }
    }

    private fun toModerationReport(document: com.google.firebase.firestore.DocumentSnapshot): ModerationReportItem? {
        val postId = document.getString("targetPostId") ?: return null
        return ModerationReportItem(
            id = document.id,
            reasonTitle = document.getString("reason").orEmpty(),
            targetTitle = document.getString("targetTitle").orEmpty(),
            description = document.getString("description").orEmpty(),
            targetPostId = postId,
            reporterId = document.getString("reporterId").orEmpty(),
            reporterName = document.getString("reporterName").orEmpty(),
            status = runCatching {
                ReportStatus.valueOf(document.getString(FIELD_STATUS).orEmpty())
            }.getOrDefault(ReportStatus.PENDING),
            createdAtMillis = document.getLong("createdAtMillis") ?: 0L
        )
    }

    private fun publishReports(reports: List<ModerationReportItem>) {
        val ordered = reports.distinctBy { it.id }.sortedByDescending { it.createdAtMillis }
        _reportHistory.value = ordered
        _reports.value = if (authRepository.currentUser.value.role == UserRole.MODERATOR) {
            ordered.filter { it.status == ReportStatus.PENDING }
        } else emptyList()
    }

    override suspend fun createReport(
        reason: String,
        description: String,
        targetTitle: String,
        targetPostId: String
    ) {
        val reporter = authRepository.currentUser.value
        require(reporter.id.isNotBlank()) { "Inicia sesión para reportar una publicación." }
        require(reason.isNotBlank() && targetPostId.isNotBlank()) { "El motivo y la publicación son obligatorios." }
        val documentId = "${targetPostId}_${reporter.id}"
        val reference = collection.document(documentId)
        firestore.runTransaction { transaction ->
            if (transaction.get(reference).exists()) throw DuplicateReportException()
            transaction.set(reference, mapOf(
                "id" to documentId,
                "targetType" to "POST",
                "targetPostId" to targetPostId,
                "targetTitle" to targetTitle,
                "reporterId" to reporter.id,
                "reporterName" to reporter.name,
                "reason" to reason.trim(),
                "description" to description.trim(),
                FIELD_STATUS to ReportStatus.PENDING.name,
                "resolution" to null,
                "moderatorComment" to null,
                "createdAtMillis" to System.currentTimeMillis(),
                "resolvedAtMillis" to null
            ))
            null
        }.await()
    }

    override suspend fun resolveReport(
        reportId: String,
        resolution: ReportResolution,
        moderatorComment: String,
        newPostStatus: PostStatus?,
        blockUser: Boolean
    ): Boolean = runCatching {
        val moderator = authRepository.currentUser.value
        require(moderator.role == UserRole.MODERATOR && moderator.id.isNotBlank())
        val reportSnapshot = collection.whereEqualTo("targetPostId", reportPostId(reportId))
            .whereEqualTo(FIELD_STATUS, ReportStatus.PENDING.name).get().await()
        val caseReferences = reportSnapshot.documents.map { it.reference }
        val reportReference = collection.document(reportId)
        firestore.runTransaction { transaction ->
            val selectedReport = transaction.get(reportReference)
            check(selectedReport.exists() && selectedReport.getString(FIELD_STATUS) == ReportStatus.PENDING.name)
            val postId = selectedReport.getString("targetPostId") ?: error("Publicación no encontrada")
            val postReference = firestore.collection(POSTS_COLLECTION).document(postId)
            val post = transaction.get(postReference)
            check(post.exists())
            val caseReports = caseReferences.map { transaction.get(it) }
            val now = System.currentTimeMillis()
            caseReports.forEach { document ->
                if (!document.exists() || document.getString(FIELD_STATUS) != ReportStatus.PENDING.name) return@forEach
                transaction.update(document.reference, mapOf(
                    FIELD_STATUS to ReportStatus.REVIEWED.name,
                    "resolution" to resolution.name,
                    "moderatorComment" to moderatorComment,
                    "moderatorId" to moderator.id,
                    "resolvedAtMillis" to now
                ))
            }
            if (resolution == ReportResolution.UPHELD && newPostStatus != null) {
                check(newPostStatus == PostStatus.REJECTED)
                transaction.update(postReference, mapOf(
                    FIELD_STATUS to PostStatus.REJECTED.name,
                    "moderatorId" to moderator.id,
                    "rejectionReason" to "Reporte: ${selectedReport.getString("reason").orEmpty()}",
                    "rejectionNotes" to moderatorComment,
                    "reportCount" to caseReports.count { it.exists() },
                    "moderatedAtMillis" to now
                ))
            }
            if (resolution == ReportResolution.UPHELD && blockUser) {
                val providerId = post.getString("providerId") ?: error("Proveedor no encontrado")
                transaction.update(firestore.collection(USERS_COLLECTION).document(providerId), "status", "BLOCKED")
            }
            null
        }.await()
        true
    }.getOrDefault(false)

    private suspend fun reportPostId(reportId: String): String = collection.document(reportId).get().await()
        .getString("targetPostId").orEmpty()

    override suspend fun resolveReportsForPost(postId: String): Boolean = runCatching {
        val moderator = authRepository.currentUser.value
        require(moderator.role == UserRole.MODERATOR)
        val snapshot = collection.whereEqualTo("targetPostId", postId)
            .whereEqualTo(FIELD_STATUS, ReportStatus.PENDING.name).get().await()
        val now = System.currentTimeMillis()
        val batch = firestore.batch()
        snapshot.documents.forEach { document -> batch.update(document.reference, mapOf(
            FIELD_STATUS to ReportStatus.REVIEWED.name,
            "resolution" to ReportResolution.DISMISSED.name,
            "moderatorId" to moderator.id,
            "resolvedAtMillis" to now
        )) }
        batch.commit().await()
        true
    }.getOrDefault(false)

    private companion object {
        const val REPORTS_COLLECTION = "reports"
        const val POSTS_COLLECTION = "posts"
        const val USERS_COLLECTION = "users"
        const val FIELD_STATUS = "status"
    }
}
