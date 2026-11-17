package com.servilocal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import androidx.credentials.CredentialManager
import com.google.firebase.FirebaseApp
import com.servilocal.core.di.FirebaseModule
import com.servilocal.data.repository.firebase.FirebaseBadgeRepository
import com.servilocal.data.repository.firebase.FirebaseUserRepository
import com.servilocal.domain.model.UserLevel
import com.servilocal.domain.model.UserRole
import com.servilocal.domain.model.UserSession
import com.servilocal.domain.repository.SessionRepository
import com.servilocal.domain.usecase.profile.AddPointsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class FirebaseBadgeRepositoryEmulatorTest {

    @Test
    fun earnedBadgeIsStoredOnceAndObservedFromFirestore() = runBlocking {
        assertTrue("Enable FIREBASE_EMULATOR_ENABLED for this instrumentation test", BuildConfig.FIREBASE_EMULATOR_ENABLED)

        val auth = FirebaseModule.provideFirebaseAuth()
        val firestore = FirebaseModule.provideFirebaseFirestore()
        auth.signOut()

        val email = "badge-${UUID.randomUUID()}@servilocal.test"
        val credential = auth.createUserWithEmailAndPassword(email, TEST_PASSWORD).await()
        val firebaseUser = credential.user ?: error("Auth Emulator did not create a user")
        val userId = firebaseUser.uid
        val userReference = firestore.collection(USERS_COLLECTION).document(userId)
        val badgesReference = userReference.collection(BADGES_COLLECTION)
        val postId = "point-source-${UUID.randomUUID()}"
        val postReference = firestore.collection(POSTS_COLLECTION).document(postId)
        val pointEventId = "publication_$postId"

        try {
            userReference.set(
                mapOf(
                    "name" to "Insignias de prueba",
                    "email" to email,
                    "role" to UserRole.USER.name,
                    "status" to "ACTIVE",
                    "points" to 0,
                    "level" to "PRINCIPIANTE",
                    "createdAtMillis" to System.currentTimeMillis()
                )
            ).await()
            postReference.set(
                mapOf(
                    "providerId" to userId,
                    "providerName" to "Insignias de prueba",
                    "status" to "PENDING_VERIFICATION",
                    "title" to "Publicación de prueba"
                )
            ).await()
            seedExistingPointBalance(userId, 220, "PROFESIONAL")

            val repository = FirebaseBadgeRepository(firestore, TestSessionRepository(userId))

            assertTrue(runCatching { repository.award(userId, "badge_1") }.isFailure)
            assertTrue(repository.award(userId, "badge_1", postId))
            assertFalse(repository.award(userId, "badge_1", postId))
            assertTrue(repository.award(userId, "badge_2"))
            assertFalse(repository.award(userId, "badge_2"))

            val observedBadges = withTimeout(10_000) {
                repository.userBadges.first { badges ->
                    badges.any { it.badgeId == "badge_1" } && badges.any { it.badgeId == "badge_2" }
                }
            }
            assertEquals(1, observedBadges.count { it.badgeId == "badge_1" })
            assertEquals(1, observedBadges.count { it.badgeId == "badge_2" })
            assertTrue(badgesReference.document("badge_1").get().await().exists())
            assertTrue(badgesReference.document("badge_2").get().await().exists())
            assertTrue(runCatching { repository.award(userId, "badge_4") }.isFailure)
            assertFalse(badgesReference.document("badge_4").get().await().exists())

            val userRepository = FirebaseUserRepository(
                auth = auth,
                firestore = firestore,
                credentialManager = CredentialManager.create(ApplicationProvider.getApplicationContext())
            )
            val updatedUser = AddPointsUseCase(userRepository)(userId, 50, pointEventId)
            assertEquals(270, updatedUser?.points)
            assertEquals(UserLevel.EXPERTO, updatedUser?.level)

            val persistedUser = userReference.get().await()
            assertEquals(270L, persistedUser.getLong("points"))
            assertEquals(UserLevel.PROFESIONAL.name, persistedUser.getString("level"))
            assertEquals(UserLevel.EXPERTO, userRepository.findById(userId)?.level)
            assertTrue(
                runCatching {
                    userReference.update(mapOf("points" to 9_999, "level" to "MAESTRO")).await()
                }.isFailure
            )
        } finally {
            runCatching { deleteEmulatorDocument("users/$userId/badges/badge_1") }
            runCatching { deleteEmulatorDocument("users/$userId/badges/badge_2") }
            runCatching { deleteEmulatorDocument("users/$userId/pointEvents/$pointEventId") }
            runCatching { deleteEmulatorDocument("posts/$postId") }
            runCatching { deleteEmulatorDocument("users/$userId") }
            runCatching { firebaseUser.delete().await() }
            auth.signOut()
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

    private fun seedExistingPointBalance(userId: String, points: Int, level: String) {
        val projectId = com.google.firebase.FirebaseApp.getInstance().options.projectId
        val fields = org.json.JSONObject()
            .put("points", org.json.JSONObject().put("integerValue", points.toString()))
            .put("level", org.json.JSONObject().put("stringValue", level))
        val body = org.json.JSONObject().put("fields", fields).toString()
        val url = java.net.URL(
            "http://${BuildConfig.FIREBASE_EMULATOR_HOST}:8080/v1/projects/$projectId/databases/(default)/documents/users/$userId?updateMask.fieldPaths=points&updateMask.fieldPaths=level"
        )
        val connection = url.openConnection() as java.net.HttpURLConnection
        try {
            connection.requestMethod = "PATCH"
            connection.setRequestProperty("Authorization", "Bearer owner")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            check(connection.responseCode in 200..299) {
                "Could not seed existing point balance in Firestore Emulator: HTTP ${connection.responseCode}"
            }
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
        const val BADGES_COLLECTION = "badges"
        const val POINT_EVENTS_COLLECTION = "pointEvents"
        const val POSTS_COLLECTION = "posts"
        const val TEST_PASSWORD = "ServiLocalTest2026"
    }
}
