package com.servilocal.data.repository.firebase

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.servilocal.domain.model.Like
import com.servilocal.domain.repository.LikeRepository
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await

@Singleton
class FirebaseLikeRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val sessionRepository: SessionRepository
) : LikeRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val likesCollection get() = firestore.collection(LIKES_COLLECTION)
    private val postsCollection get() = firestore.collection(POSTS_COLLECTION)

    override val likes = sessionRepository.session
        .flatMapLatest { session ->
            if (session == null) flowOf(emptyList()) else observeLikes()
        }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override suspend fun toggleLike(postId: String, userId: String): Boolean {
        val likeId = "${postId}_${userId}"
        val likeRef = likesCollection.document(likeId)
        val postRef = postsCollection.document(postId)
        return firestore.runTransaction { transaction ->
            val likeSnapshot = transaction.get(likeRef)
            val liked = !likeSnapshot.exists()
            if (liked) {
                transaction.set(
                    likeRef,
                    mapOf(
                        "id" to likeId,
                        "userId" to userId,
                        "postId" to postId
                    )
                )
                transaction.update(postRef, "upvotesCount", FieldValue.increment(1))
            } else {
                transaction.delete(likeRef)
                transaction.update(postRef, "upvotesCount", FieldValue.increment(-1))
            }
            liked
        }.await()
    }

    override fun hasLiked(postId: String, userId: String): Boolean =
        likes.value.any { it.postId == postId && it.userId == userId }

    private fun observeLikes(): Flow<List<Like>> = callbackFlow {
        val registration: ListenerRegistration = likesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
            } else {
                trySend(snapshot?.documents?.mapNotNull { it.toLike() } ?: emptyList())
            }
        }
        awaitClose { registration.remove() }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toLike(): Like? {
        if (!exists()) return null
        return Like(
            id = id,
            userId = getString("userId") ?: return null,
            postId = getString("postId") ?: return null
        )
    }

    private companion object {
        const val LIKES_COLLECTION = "likes"
        const val POSTS_COLLECTION = "posts"
    }
}
