package com.servilocal.data.repository.firebase

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.servilocal.domain.model.Comment
import com.servilocal.domain.repository.CommentRepository
import com.servilocal.domain.repository.SessionRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await

@Singleton
class FirebaseCommentRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val sessionRepository: SessionRepository
) : CommentRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val commentsCollection get() = firestore.collection(COMMENTS_COLLECTION)

    override val comments = sessionRepository.session
        .flatMapLatest { session ->
            if (session == null) flowOf(emptyList()) else observeComments()
        }
        .catch { emit(emptyList()) }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override suspend fun addComment(
        postId: String,
        userId: String,
        authorName: String,
        content: String
    ): Boolean {
        return try {
            val reference = commentsCollection.document()
            reference.set(
                mapOf(
                    "id" to reference.id,
                    "postId" to postId,
                    "userId" to userId,
                    "authorName" to authorName,
                    "content" to content,
                    "createdAtMillis" to System.currentTimeMillis(),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            true
        } catch (error: Exception) {
            false
        }
    }

    private fun observeComments(): Flow<List<Comment>> = callbackFlow {
        val registration: ListenerRegistration = commentsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
            } else {
                trySend(snapshot?.documents?.mapNotNull { it.toComment() } ?: emptyList())
            }
        }
        awaitClose { registration.remove() }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toComment(): Comment? {
        if (!exists()) return null
        return Comment(
            id = id,
            postId = getString("postId") ?: return null,
            userId = getString("userId") ?: return null,
            authorName = getString("authorName").orEmpty(),
            content = getString("content").orEmpty(),
            createdAtMillis = getLong("createdAtMillis") ?: 0L
        )
    }

    private companion object {
        const val COMMENTS_COLLECTION = "comments"
    }
}
