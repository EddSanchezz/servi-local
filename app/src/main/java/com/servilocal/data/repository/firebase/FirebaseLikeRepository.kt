package com.servilocal.data.repository.firebase

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.servilocal.domain.model.Like
import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.repository.LikeRepository
import com.servilocal.domain.repository.SessionRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Firestore-backed likes; document IDs make each user's like on a post unique. */
@Singleton
class FirebaseLikeRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val sessionRepository: SessionRepository
) : LikeRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val likesCollection get() = firestore.collection(LIKES_COLLECTION)
    private val _likes = MutableStateFlow<List<Like>>(emptyList())
    override val likes: StateFlow<List<Like>> = _likes.asStateFlow()
    private var likesRegistration: ListenerRegistration? = null

    @Volatile
    private var activeUserId: String? = null

    init {
        scope.launch {
            sessionRepository.session.collectLatest { session ->
                likesRegistration?.remove()
                likesRegistration = null
                val userId = session?.userId?.takeIf(String::isNotBlank)
                activeUserId = userId
                _likes.value = emptyList()
                if (userId == null) return@collectLatest
                likesRegistration = likesCollection.whereEqualTo("userId", userId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null || snapshot == null) return@addSnapshotListener
                        if (activeUserId != userId) return@addSnapshotListener
                        _likes.value = snapshot.documents.mapNotNull { it.toLike() }
                            .sortedByDescending(Like::createdAtMillis)
                    }
            }
        }
    }

    override suspend fun toggleLike(postId: String, userId: String): Boolean {
        if (userId.isBlank() || sessionRepository.session.value?.userId != userId) return false
        val postReference = firestore.collection(POSTS_COLLECTION).document(postId)
        val likeReference = likesCollection.document("${postId}_$userId")
        val now = System.currentTimeMillis()

        return firestore.runTransaction { transaction ->
            val post = transaction.get(postReference)
            if (!post.exists() || post.getString("status") != PostStatus.APPROVED.name) {
                return@runTransaction false
            }
            val postOwnerId = post.getString("providerId") ?: return@runTransaction false
            if (postOwnerId == userId) return@runTransaction false

            val existingLike = transaction.get(likeReference)
            if (existingLike.exists()) {
                transaction.delete(likeReference)
                transaction.update(postReference, "upvotesCount", FieldValue.increment(-1))
                false
            } else {
                transaction.set(likeReference, mapOf(
                    "id" to likeReference.id,
                    "postId" to postId,
                    "userId" to userId,
                    "createdAtMillis" to now
                ))
                transaction.update(postReference, "upvotesCount", FieldValue.increment(1))
                true
            }
        }.await()
    }

    override fun hasLiked(postId: String, userId: String): Boolean =
        likes.value.any { it.postId == postId && it.userId == userId }

    private fun com.google.firebase.firestore.DocumentSnapshot.toLike(): Like? {
        val postId = getString("postId") ?: return null
        val userId = getString("userId") ?: return null
        return Like(
            id = id,
            userId = userId,
            postId = postId,
            createdAtMillis = getLong("createdAtMillis") ?: 0L
        )
    }

    private companion object {
        const val LIKES_COLLECTION = "likes"
        const val POSTS_COLLECTION = "posts"
    }
}
