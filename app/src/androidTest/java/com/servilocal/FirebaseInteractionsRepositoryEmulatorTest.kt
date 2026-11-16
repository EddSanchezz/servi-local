package com.servilocal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import androidx.credentials.CredentialManager
import com.google.firebase.FirebaseApp
import com.servilocal.core.di.FirebaseModule
import com.servilocal.data.repository.memory.SessionRepositoryImpl
import com.servilocal.data.repository.firebase.FirebaseCommentRepository
import com.servilocal.data.repository.firebase.FirebaseLikeRepository
import com.servilocal.data.repository.firebase.FirebasePostRepository
import com.servilocal.data.repository.firebase.FirebaseBadgeRepository
import com.servilocal.data.repository.firebase.FirebaseUserRepository
import com.servilocal.domain.model.UserRole
import com.servilocal.domain.model.UserSession
import com.servilocal.domain.repository.SessionRepository
import com.servilocal.domain.usecase.profile.GetProfileStatsUseCase
import com.servilocal.domain.usecase.user.ObserveCurrentUserUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class FirebaseInteractionsRepositoryEmulatorTest {

    @Test
    fun likesAndCommentsPersistAndAreObservableForPostOwner() = runBlocking {
        assertTrue("Enable FIREBASE_EMULATOR_ENABLED for this instrumentation test", BuildConfig.FIREBASE_EMULATOR_ENABLED)

        val auth = FirebaseModule.provideFirebaseAuth()
        val firestore = FirebaseModule.provideFirebaseFirestore()
        auth.signOut()

        val suffix = UUID.randomUUID().toString()
        val password = "ServiLocalTest2026"
        val ownerEmail = "interaction-owner-$suffix@servilocal.test"
        val likerEmail = "interaction-liker-$suffix@servilocal.test"
        val ownerAuthUser = auth.createUserWithEmailAndPassword(ownerEmail, password).await().user
            ?: error("Could not create owner in Auth Emulator")
        val ownerId = ownerAuthUser.uid
        val ownerReference = firestore.collection(USERS_COLLECTION).document(ownerId)
        ownerReference.set(userDocument(ownerEmail, "Proveedor de prueba")).await()

        auth.signOut()
        val likerAuthUser = auth.createUserWithEmailAndPassword(likerEmail, password).await().user
            ?: error("Could not create liker in Auth Emulator")
        val likerId = likerAuthUser.uid
        val likerReference = firestore.collection(USERS_COLLECTION).document(likerId)
        likerReference.set(userDocument(likerEmail, "Cliente de prueba")).await()

        val postId = "interaction-post-$suffix"
        seedApprovedPost(postId, ownerId)
        val likeId = "${postId}_$likerId"
        var commentId: String? = null

        try {
            val likerSession = TestSessionRepository(likerId)
            val likeRepository = FirebaseLikeRepository(firestore, likerSession)
            val postReference = firestore.collection(POSTS_COLLECTION).document(postId)
            assertTrue(runCatching { postReference.update("upvotesCount", 99).await() }.isFailure)
            assertTrue(likeRepository.toggleLike(postId, likerId))
            assertEquals(1L, postReference.get().await().getLong("upvotesCount"))
            assertTrue(withTimeout(10_000) {
                likeRepository.likes.first { likes -> likes.any { it.postId == postId && it.userId == likerId } }
            }.any { it.postId == postId })

            assertEquals("Toggling an existing like removes it", false, likeRepository.toggleLike(postId, likerId))
            assertEquals(0L, postReference.get().await().getLong("upvotesCount"))
            withTimeout(10_000) { likeRepository.likes.first { likes -> likes.none { it.postId == postId } } }

            assertTrue(likeRepository.toggleLike(postId, likerId))
            assertEquals(1L, postReference.get().await().getLong("upvotesCount"))

            val commentRepository = FirebaseCommentRepository(firestore, likerSession)
            assertTrue(!commentRepository.addComment(postId, ownerId, "Proveedor", "Comentario ajeno"))
            assertTrue(commentRepository.addComment(postId, likerId, "Cliente de prueba", "Comentario sincronizado"))
            val postComments = withTimeout(10_000) {
                commentRepository.observeForPost(postId).first { comments ->
                    comments.any { it.content == "Comentario sincronizado" }
                }
            }
            val createdComment = postComments.first { it.content == "Comentario sincronizado" }
            commentId = createdComment.id
            assertEquals(ownerId, createdComment.postOwnerId)

            auth.signOut()
            auth.signInWithEmailAndPassword(ownerEmail, password).await()
            val ownerSession = TestSessionRepository(ownerId)
            val receivedComments = withTimeout(10_000) {
                FirebaseCommentRepository(firestore, ownerSession).comments.first { comments ->
                    comments.any { it.id == createdComment.id }
                }
            }
            assertEquals(1, receivedComments.count { it.postOwnerId == ownerId })

            val ownerPosts = withTimeout(10_000) {
                FirebasePostRepository(firestore, ownerSession).posts.first { posts ->
                    posts.any { it.id == postId && it.upvotesCount == 1 }
                }
            }
            assertEquals(1, ownerPosts.first { it.id == postId }.upvotesCount)

            val realSession = SessionRepositoryImpl(auth, firestore)
            val userRepository = FirebaseUserRepository(
                auth,
                firestore,
                CredentialManager.create(ApplicationProvider.getApplicationContext())
            )
            val profileStats = GetProfileStatsUseCase(
                ObserveCurrentUserUseCase(realSession, userRepository),
                FirebasePostRepository(firestore, realSession),
                FirebaseBadgeRepository(firestore, realSession),
                FirebaseCommentRepository(firestore, realSession)
            )
            val stats = withTimeout(15_000) {
                profileStats().first { it != null && it.likesReceived == 1 && it.commentsReceived == 1 }
            }
            assertEquals(1, stats?.likesReceived)
            assertEquals(1, stats?.commentsReceived)
        } finally {
            deleteEmulatorDocument("likes/$likeId")
            commentId?.let { deleteEmulatorDocument("comments/$it") }
            deleteEmulatorDocument("posts/$postId")
            deleteEmulatorDocument("users/$ownerId")
            deleteEmulatorDocument("users/$likerId")
            auth.signOut()
            runCatching { auth.signInWithEmailAndPassword(ownerEmail, password).await().user?.delete()?.await() }
            auth.signOut()
            runCatching { auth.signInWithEmailAndPassword(likerEmail, password).await().user?.delete()?.await() }
            auth.signOut()
        }
    }

    private fun userDocument(email: String, name: String) = mapOf(
        "name" to name,
        "email" to email,
        "role" to UserRole.USER.name,
        "status" to "ACTIVE",
        "points" to 0,
        "level" to "PRINCIPIANTE",
        "createdAtMillis" to System.currentTimeMillis()
    )

    private fun seedApprovedPost(postId: String, providerId: String) {
        val projectId = FirebaseApp.getInstance().options.projectId
        val fields = JSONObject()
            .put("providerId", JSONObject().put("stringValue", providerId))
            .put("providerName", JSONObject().put("stringValue", "Proveedor de prueba"))
            .put("title", JSONObject().put("stringValue", "Publicación para interacción"))
            .put("category", JSONObject().put("stringValue", "HOGAR"))
            .put("description", JSONObject().put("stringValue", "Publicación local de prueba"))
            .put("status", JSONObject().put("stringValue", "APPROVED"))
            .put("upvotesCount", JSONObject().put("integerValue", "0"))
            .put("createdAtMillis", JSONObject().put("integerValue", System.currentTimeMillis().toString()))
        val body = JSONObject().put("fields", fields).toString()
        val url = URL("http://${BuildConfig.FIREBASE_EMULATOR_HOST}:8080/v1/projects/$projectId/databases/(default)/documents/posts/$postId")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "PATCH"
            connection.setRequestProperty("Authorization", "Bearer owner")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            check(connection.responseCode in 200..299) {
                "Could not seed approved post in Firestore Emulator: HTTP ${connection.responseCode}"
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun deleteEmulatorDocument(path: String) {
        val projectId = FirebaseApp.getInstance().options.projectId
        val url = URL("http://${BuildConfig.FIREBASE_EMULATOR_HOST}:8080/v1/projects/$projectId/databases/(default)/documents/$path")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "DELETE"
            connection.setRequestProperty("Authorization", "Bearer owner")
            connection.responseCode
        } finally {
            connection.disconnect()
        }
    }

    private class TestSessionRepository(userId: String) : SessionRepository {
        private val mutableSession = MutableStateFlow<UserSession?>(UserSession(userId, UserRole.USER))
        override val session: StateFlow<UserSession?> = mutableSession

        override suspend fun saveSession(userId: String, role: UserRole) {
            mutableSession.value = UserSession(userId, role)
        }

        override suspend fun clearSession() {
            mutableSession.value = null
        }
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val POSTS_COLLECTION = "posts"
    }
}
