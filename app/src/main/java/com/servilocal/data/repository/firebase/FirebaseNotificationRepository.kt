package com.servilocal.data.repository.firebase

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.servilocal.domain.model.AppNotification
import com.servilocal.domain.model.NotificationType
import com.servilocal.domain.repository.NotificationRepository
import com.servilocal.domain.repository.SessionRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.tasks.await

/** Persists notifications and observes only the signed-in user's inbox. */
@Singleton
class FirebaseNotificationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val sessionRepository: SessionRepository
) : NotificationRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val collection get() = firestore.collection(NOTIFICATIONS_COLLECTION)
    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    override val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    @Volatile
    private var activeUserId: String? = null
    private var registration: ListenerRegistration? = null

    init {
        scope.launch {
            sessionRepository.session.collectLatest { session ->
                registration?.remove()
                registration = null
                val userId = session?.userId?.takeIf(String::isNotBlank)
                activeUserId = userId
                _notifications.value = emptyList()
                if (userId == null) return@collectLatest

                registration = collection.whereEqualTo(FIELD_USER_ID, userId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.w(TAG, "Could not observe notification inbox", error)
                            return@addSnapshotListener
                        }
                        if (snapshot == null || activeUserId != userId) return@addSnapshotListener
                        _notifications.value = snapshot.documents
                            .mapNotNull { it.toAppNotification() }
                            .sortedByDescending(AppNotification::createdAtMillis)
                    }
            }
        }
    }

    /**
     * Notifications are best-effort side effects: enqueue them without blocking the
     * action that caused them. Firestore's local persistence keeps writes queued offline.
     */
    override fun create(notification: AppNotification) {
        val senderId = sessionRepository.session.value?.userId?.takeIf(String::isNotBlank)
        if (senderId == null || notification.id.isBlank() || notification.userId.isBlank() ||
            notification.title.isBlank() || notification.body.isBlank() || notification.isRead
        ) return

        scope.launch {
            try {
                // A newly-started chat is written by another coroutine. Wait until its
                // local Firestore snapshot exists so the notification rules can verify
                // that both users are actual chat participants.
                val chatId = notification.relatedChatId
                if (notification.type == NotificationType.REQUEST && chatId != null &&
                    !awaitChatExists(chatId)
                ) {
                    Log.w(TAG, "Skipping chat notification because chat $chatId was not created")
                    return@launch
                }
                if (sessionRepository.session.value?.userId != senderId) return@launch
                collection.document(notification.id)
                    .set(notification.toFirestoreMap(senderId))
                    .await()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.w(TAG, "Could not save notification ${notification.id}", error)
            }
        }
    }

    override suspend fun markRead(notificationId: String, userId: String, isRead: Boolean): Boolean {
        if (!isCurrentUser(userId) || notificationId.isBlank()) return false
        val reference = collection.document(notificationId)
        return firestore.runTransaction { transaction ->
            val document = transaction.get(reference)
            if (!document.exists() || document.getString(FIELD_USER_ID) != userId) {
                return@runTransaction false
            }
            transaction.update(reference, FIELD_IS_READ, isRead)
            true
        }.await()
    }

    override suspend fun markAllRead(userId: String) {
        if (!isCurrentUser(userId)) return
        val unread = collection
            .whereEqualTo(FIELD_USER_ID, userId)
            .get()
            .await()
            .documents
            .filter { it.getBoolean(FIELD_IS_READ) != true }

        unread.chunked(MAX_BATCH_WRITES).forEach { documents ->
            val batch = firestore.batch()
            documents.forEach { batch.update(it.reference, FIELD_IS_READ, true) }
            batch.commit().await()
        }
    }

    override suspend fun deleteNotificationsForUser(userId: String): Boolean {
        if (!isCurrentUser(userId)) return false
        val documents = collection.whereEqualTo(FIELD_USER_ID, userId).get().await().documents
        documents.chunked(MAX_BATCH_WRITES).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
        return true
    }

    private suspend fun awaitChatExists(chatId: String): Boolean =
        withTimeoutOrNull(CHAT_WAIT_TIMEOUT_MILLIS) {
            callbackFlow {
                val registration = firestore.collection(CHATS_COLLECTION).document(chatId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                        } else if (snapshot?.exists() == true) {
                            trySend(Unit)
                            close()
                        }
                    }
                awaitClose { registration.remove() }
            }.first()
            true
        } ?: false

    private fun isCurrentUser(userId: String): Boolean =
        userId.isNotBlank() && sessionRepository.session.value?.userId == userId

    private fun AppNotification.toFirestoreMap(senderId: String): Map<String, Any?> = mapOf(
        "id" to id,
        FIELD_USER_ID to userId,
        FIELD_CREATED_BY_USER_ID to senderId,
        "title" to title.trim(),
        "body" to body.trim(),
        "type" to type.name,
        FIELD_IS_READ to isRead,
        "relatedPostId" to relatedPostId,
        "relatedChatId" to relatedChatId,
        "relatedCommentId" to relatedCommentId,
        "relatedReportId" to relatedReportId,
        "createdAtMillis" to createdAtMillis
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toAppNotification(): AppNotification? {
        val userId = getString(FIELD_USER_ID) ?: return null
        val type = getString("type")
            ?.let { runCatching { NotificationType.valueOf(it) }.getOrNull() }
            ?: return null
        return AppNotification(
            id = id,
            userId = userId,
            title = getString("title").orEmpty(),
            body = getString("body").orEmpty(),
            type = type,
            isRead = getBoolean(FIELD_IS_READ) ?: false,
            relatedPostId = getString("relatedPostId"),
            relatedChatId = getString("relatedChatId"),
            createdAtMillis = getLong("createdAtMillis") ?: 0L,
            relatedCommentId = getString("relatedCommentId"),
            relatedReportId = getString("relatedReportId")
        )
    }

    private companion object {
        const val TAG = "FirebaseNotificationRepo"
        const val NOTIFICATIONS_COLLECTION = "notifications"
        const val CHATS_COLLECTION = "chats"
        const val FIELD_USER_ID = "userId"
        const val FIELD_CREATED_BY_USER_ID = "createdByUserId"
        const val FIELD_IS_READ = "isRead"
        const val MAX_BATCH_WRITES = 500
        const val CHAT_WAIT_TIMEOUT_MILLIS = 15_000L
    }
}
