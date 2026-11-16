package com.servilocal

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import com.servilocal.core.di.FirebaseModule
import com.servilocal.data.repository.firebase.FirebasePostRepository
import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.model.UserRole
import com.servilocal.domain.model.UserSession
import com.servilocal.domain.repository.SessionRepository
import com.servilocal.domain.usecase.moderation.GetModerationStatsUseCase
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
class ModerationStatsEmulatorTest {

    @Test
    fun moderationDecisionsUpdateMetricsByDecisionTimeNotPostCreationTime() = runBlocking {
        assertTrue("Enable FIREBASE_EMULATOR_ENABLED for this instrumentation test", BuildConfig.FIREBASE_EMULATOR_ENABLED)

        val auth = FirebaseModule.provideFirebaseAuth()
        val firestore = FirebaseModule.provideFirebaseFirestore()
        auth.signOut()

        val email = "moderation-metrics-${UUID.randomUUID()}@servilocal.test"
        val password = "ServiLocalTest2026"
        val firebaseUser = auth.createUserWithEmailAndPassword(email, password).await().user
            ?: error("Could not create moderator in Auth Emulator")
        val moderatorId = firebaseUser.uid
        val userReference = firestore.collection(USERS_COLLECTION).document(moderatorId)
        val now = System.currentTimeMillis()
        val oldCreatedAt = now - 20L * DAY_MILLIS
        val approvedPostId = "old-approved-${UUID.randomUUID()}"
        val rejectedPostId = "old-rejected-${UUID.randomUUID()}"
        val pendingPostId = "old-pending-${UUID.randomUUID()}"

        try {
            userReference.set(mapOf(
                "name" to "Moderador de métricas",
                "email" to email,
                "role" to UserRole.USER.name,
                "status" to "ACTIVE",
                "points" to 0,
                "level" to "PRINCIPIANTE",
                "createdAtMillis" to now
            )).await()
            updateUserRoleAsEmulatorAdmin(moderatorId, UserRole.MODERATOR.name)
            seedPendingPost(approvedPostId, oldCreatedAt)
            seedPendingPost(rejectedPostId, oldCreatedAt)
            seedPendingPost(pendingPostId, oldCreatedAt)

            val session = TestSessionRepository(moderatorId)
            val posts = FirebasePostRepository(firestore, session)
            withTimeout(10_000) { posts.posts.first { it.count { post -> post.status == PostStatus.PENDING_VERIFICATION } >= 3 } }

            posts.approvePost(approvedPostId)
            posts.rejectPost(rejectedPostId, "Prueba", "Rechazo del emulador")

            val updatedPosts = withTimeout(10_000) {
                posts.posts.first { observed ->
                    observed.any { it.id == approvedPostId && it.status == PostStatus.APPROVED && it.moderatedAtMillis > 0 } &&
                        observed.any { it.id == rejectedPostId && it.status == PostStatus.REJECTED && it.moderatedAtMillis > 0 }
                }
            }
            val testPostIds = setOf(approvedPostId, rejectedPostId, pendingPostId)
            val stats = GetModerationStatsUseCase()(
                posts = updatedPosts.filter { it.id in testPostIds },
                reportHistory = emptyList(),
                timeRange = "Esta semana",
                nowMillis = System.currentTimeMillis() + 60_000
            )

            assertEquals(0, stats.newPosts)
            assertEquals(1, stats.approvedPosts)
            assertEquals(1, stats.rejectedPosts)
            assertEquals(1, stats.pendingPosts)
        } finally {
            deleteEmulatorDocument("posts/$approvedPostId")
            deleteEmulatorDocument("posts/$rejectedPostId")
            deleteEmulatorDocument("posts/$pendingPostId")
            deleteEmulatorDocument("users/$moderatorId")
            auth.signOut()
            runCatching { auth.signInWithEmailAndPassword(email, password).await().user?.delete()?.await() }
            auth.signOut()
        }
    }

    private fun seedPendingPost(postId: String, createdAtMillis: Long) {
        val projectId = FirebaseApp.getInstance().options.projectId
        val fields = JSONObject()
            .put("providerId", JSONObject().put("stringValue", "demo-provider"))
            .put("providerName", JSONObject().put("stringValue", "Proveedor de prueba"))
            .put("title", JSONObject().put("stringValue", postId))
            .put("category", JSONObject().put("stringValue", "HOGAR"))
            .put("description", JSONObject().put("stringValue", "Publicación antigua para probar métricas"))
            .put("status", JSONObject().put("stringValue", PostStatus.PENDING_VERIFICATION.name))
            .put("upvotesCount", JSONObject().put("integerValue", "0"))
            .put("createdAtMillis", JSONObject().put("integerValue", createdAtMillis.toString()))
        seedEmulatorDocument("posts/$postId", fields)
    }

    private fun updateUserRoleAsEmulatorAdmin(userId: String, role: String) {
        val fields = JSONObject().put("role", JSONObject().put("stringValue", role))
        seedEmulatorDocument("users/$userId?updateMask.fieldPaths=role", fields)
    }

    private fun seedEmulatorDocument(path: String, fields: JSONObject) {
        val projectId = FirebaseApp.getInstance().options.projectId
        val body = JSONObject().put("fields", fields).toString()
        val url = URL("http://${BuildConfig.FIREBASE_EMULATOR_HOST}:8080/v1/projects/$projectId/databases/(default)/documents/$path")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "PATCH"
            connection.setRequestProperty("Authorization", "Bearer owner")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            check(connection.responseCode in 200..299) {
                "Could not seed moderation test document: HTTP ${connection.responseCode}"
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
        override val session: StateFlow<UserSession?> = MutableStateFlow(UserSession(userId, UserRole.MODERATOR))
        override suspend fun saveSession(userId: String, role: UserRole) {}
        override suspend fun clearSession() {}
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val DAY_MILLIS = 24L * 60 * 60 * 1000
    }
}
