package com.servilocal.data.repository.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.servilocal.domain.model.Comment
import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.repository.CommentRepository
import com.servilocal.domain.repository.SessionRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Firestore-backed comments and the current user's received-comment stream. */
@Singleton
class FirebaseCommentRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val sessionRepository: SessionRepository
) : CommentRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val collection get() = firestore.collection(COMMENTS_COLLECTION)
    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    override val comments: StateFlow<List<Comment>> = _comments.asStateFlow()
    private var receivedRegistration: ListenerRegistration? = null
    @Volatile
    private var activeUserId: String? = null

    init {
        scope.launch {
            sessionRepository.session.collectLatest { session ->
                receivedRegistration?.remove()
                receivedRegistration = null
                val userId = session?.userId?.takeIf(String::isNotBlank)
                activeUserId = userId
                _comments.value = emptyList()
                if (userId == null) return@collectLatest
                receivedRegistration = collection.whereEqualTo("postOwnerId", userId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null || snapshot == null) return@addSnapshotListener
                        if (activeUserId != userId) return@addSnapshotListener
                        _comments.value = snapshot.documents
                            .mapNotNull { it.toComment() }
                            .sortedByDescending(Comment::createdAtMillis)
                    }
            }
        }
    }

    override fun observeForPost(postId: String): Flow<List<Comment>> = callbackFlow {
        if (postId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = collection.whereEqualTo("postId", postId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) trySend(emptyList())
                else trySend(snapshot.documents.mapNotNull { it.toComment() }
                    .sortedByDescending(Comment::createdAtMillis))
            }
        awaitClose { registration.remove() }
    }

    override suspend fun addComment(
        postId: String,
        userId: String,
        authorName: String,
        content: String
    ): String? {
        val sessionUserId = sessionRepository.session.value?.userId
        if (postId.isBlank() || userId.isBlank() || userId != sessionUserId || content.isBlank()) return null

        val postReference = firestore.collection(POSTS_COLLECTION).document(postId)
        val commentReference = collection.document()
        val createdAtMillis = System.currentTimeMillis()
        return firestore.runTransaction { transaction ->
            val post = transaction.get(postReference)
            if (!post.exists() || post.getString("status") != PostStatus.APPROVED.name) {
                return@runTransaction null
            }
            val postOwnerId = post.getString("providerId") ?: return@runTransaction null
            transaction.set(commentReference, mapOf(
                "id" to commentReference.id,
                "postId" to postId,
                "postOwnerId" to postOwnerId,
                "userId" to userId,
                "authorName" to authorName,
                "content" to content.trim(),
                "createdAtMillis" to createdAtMillis
            ))
            commentReference.id
        }.await()
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toComment(): Comment? {
        val postId = getString("postId") ?: return null
        val userId = getString("userId") ?: return null
        return Comment(
            id = id,
            postId = postId,
            userId = userId,
            content = getString("content").orEmpty(),
            createdAtMillis = getLong("createdAtMillis") ?: 0L,
            postOwnerId = getString("postOwnerId"),
            authorName = getString("authorName").orEmpty()
        )
    }

    private companion object {
        const val COMMENTS_COLLECTION = "comments"
        const val POSTS_COLLECTION = "posts"
    }
}
