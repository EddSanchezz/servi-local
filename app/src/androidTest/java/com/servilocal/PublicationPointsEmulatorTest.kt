package com.servilocal

import androidx.credentials.CredentialManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import com.servilocal.core.data.repository.MockDataRepository
import com.servilocal.core.di.FirebaseModule
import com.servilocal.data.repository.firebase.FirebaseBadgeRepository
import com.servilocal.data.repository.firebase.FirebasePostRepository
import com.servilocal.data.repository.firebase.FirebaseUserRepository
import com.servilocal.data.repository.memory.SessionRepositoryImpl
import com.servilocal.domain.model.UserLevel
import com.servilocal.domain.model.UserRole
import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.usecase.notifications.CreateNotificationUseCase
import com.servilocal.domain.usecase.posts.CreateServicePostUseCase
import com.servilocal.domain.usecase.profile.AddPointsUseCase
import com.servilocal.domain.usecase.profile.AwardPublicationPointsUseCase
import com.servilocal.domain.usecase.profile.CheckAndAwardBadgesUseCase
import com.servilocal.domain.usecase.profile.ObserveUserBadgesUseCase
import com.servilocal.domain.usecase.user.ObserveCurrentUserUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PublicationPointsEmulatorTest {

    @Test
    fun onlyApprovedPublicationsAwardFiftyPointsAndEligibleBadgesOnce() = runBlocking {
        assertTrue("Enable FIREBASE_EMULATOR_ENABLED for this instrumentation test", BuildConfig.FIREBASE_EMULATOR_ENABLED)

        val auth = FirebaseModule.provideFirebaseAuth()
        val firestore = FirebaseModule.provideFirebaseFirestore()
        auth.signOut()

        val email = "publication-points-${UUID.randomUUID()}@servilocal.test"
        val credential = auth.createUserWithEmailAndPassword(email, TEST_PASSWORD).await()
        val firebaseUser = credential.user ?: error("Auth Emulator did not create a user")
        val userId = firebaseUser.uid
        val userReference = firestore.collection(USERS_COLLECTION).document(userId)

        try {
            userReference.set(mapOf(
                "name" to "Publicador de prueba",
                "email" to email,
                "role" to UserRole.USER.name,
                "points" to 0,
                "level" to UserLevel.PRINCIPIANTE.name,
                "status" to "ACTIVE",
                "createdAtMillis" to System.currentTimeMillis(),
                "photoUrl" to null
            )).await()
            seedExistingPointBalance(userId, 220, UserLevel.PROFESIONAL.name)

            val sessionRepository = SessionRepositoryImpl(auth, firestore)
            val userRepository = FirebaseUserRepository(
                auth,
                firestore,
                CredentialManager.create(ApplicationProvider.getApplicationContext())
            )
            val postRepository = FirebasePostRepository(firestore, sessionRepository)
            val badgeRepository = FirebaseBadgeRepository(firestore, sessionRepository)
            val notifications = MockDataRepository()
            val awardPoints = AwardPublicationPointsUseCase(
                AddPointsUseCase(userRepository),
                CheckAndAwardBadgesUseCase(
                    sessionRepository,
                    userRepository,
                    badgeRepository,
                    postRepository,
                    CreateNotificationUseCase(notifications)
                ),
                postRepository
            )

            withTimeout(10_000) { sessionRepository.session.first { it?.userId == userId } }
            withTimeout(10_000) { userRepository.currentUser.first { it.id == userId } }
            val publicProfile = firestore.collection(PUBLIC_USERS_COLLECTION).document(userId).get().await()
            assertEquals("Publicador de prueba", publicProfile.getString("name"))
            assertFalse(publicProfile.contains("email"))

            val createPost = CreateServicePostUseCase(
                postRepository = postRepository,
                authRepository = userRepository,
                createNotification = CreateNotificationUseCase(notifications)
            )
            val post = createPost(
                title = "Servicio Firebase de prueba",
                category = "Hogar",
                description = "Publicación para probar el premio automático",
                minPrice = 10_000,
                maxPrice = 20_000,
                isPriceToAgree = false,
                location = "Armenia, Quindío",
                latitude = 4.5339,
                longitude = -75.6811,
                photos = listOf("https://example.com/test-publication.jpg")
            )

            assertEquals(userId, post.providerId)
            assertEquals(PostStatus.PENDING_VERIFICATION, post.status)

            val persistedUser = userReference.get().await()
            assertEquals(220L, persistedUser.getLong("points"))
            assertFalse(userReference.collection(POINT_EVENTS_COLLECTION)
                .document("publication_${post.id}").get().await().exists())

            assertEquals(null, awardPoints(userId, post.id))
            assertEquals(220L, userReference.get().await().getLong("points"))
            setEmulatorPostStatus(post.id, PostStatus.APPROVED)
            withTimeout(10_000) {
                postRepository.posts.first { posts ->
                    posts.any { it.id == post.id && it.status == PostStatus.APPROVED }
                }
            }
            assertTrue(awardPoints(userId, post.id) != null)
            assertEquals(270L, userReference.get().await().getLong("points"))
            assertEquals(UserLevel.EXPERTO.name, userReference.get().await().getString("level"))
            assertTrue(userReference.collection(POINT_EVENTS_COLLECTION)
                .document("publication_${post.id}").get().await().exists())

            val loadedBadges = withTimeout(10_000) {
                ObserveUserBadgesUseCase(sessionRepository, badgeRepository)().first { badges ->
                    badges.count { it.unlocked } >= 3
                }
            }
            assertTrue(loadedBadges.first { it.id == "badge_1" }.unlocked)
            assertTrue(loadedBadges.first { it.id == "badge_2" }.unlocked)
            assertTrue(loadedBadges.first { it.id == "badge_3" }.unlocked)
            assertTrue(!loadedBadges.first { it.id == "badge_4" }.unlocked)

            awardPoints(userId, post.id)
            assertEquals(270L, userReference.get().await().getLong("points"))
        } finally {
            // Remove documents created by the test while bypassing client rules, then remove its Auth identity.
            val postDocs = firestore.collection(POSTS_COLLECTION).whereEqualTo("providerId", userId).get().await()
            postDocs.documents.forEach { deleteEmulatorDocument("posts/${it.id}") }
            val badgeDocs = userReference.collection(BADGES_COLLECTION).get().await()
            badgeDocs.documents.forEach { deleteEmulatorDocument("users/$userId/badges/${it.id}") }
            val pointEvents = userReference.collection(POINT_EVENTS_COLLECTION).get().await()
            pointEvents.documents.forEach { deleteEmulatorDocument("users/$userId/pointEvents/${it.id}") }
            deleteEmulatorDocument("users/$userId")
            deleteEmulatorDocument("$PUBLIC_USERS_COLLECTION/$userId")
            auth.signOut()
            runCatching { auth.signInWithEmailAndPassword(email, TEST_PASSWORD).await().user?.delete()?.await() }
            auth.signOut()
        }
    }

    private fun seedExistingPointBalance(userId: String, points: Int, level: String) {
        val projectId = FirebaseApp.getInstance().options.projectId
        val fields = JSONObject()
            .put("points", JSONObject().put("integerValue", points.toString()))
            .put("level", JSONObject().put("stringValue", level))
        val body = JSONObject().put("fields", fields).toString()
        val url = URL("http://${BuildConfig.FIREBASE_EMULATOR_HOST}:8080/v1/projects/$projectId/databases/(default)/documents/users/$userId?updateMask.fieldPaths=points&updateMask.fieldPaths=level")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "PATCH"
            connection.setRequestProperty("Authorization", "Bearer owner")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            check(connection.responseCode in 200..299) {
                "Could not seed existing point balance: HTTP ${connection.responseCode}"
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun setEmulatorPostStatus(postId: String, status: PostStatus) {
        val projectId = FirebaseApp.getInstance().options.projectId
        val fields = JSONObject()
            .put("status", JSONObject().put("stringValue", status.name))
        val body = JSONObject().put("fields", fields).toString()
        val url = URL(
            "http://${BuildConfig.FIREBASE_EMULATOR_HOST}:8080/v1/projects/$projectId/databases/(default)/documents/$POSTS_COLLECTION/$postId?updateMask.fieldPaths=status"
        )
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "PATCH"
            connection.setRequestProperty("Authorization", "Bearer owner")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            check(connection.responseCode in 200..299) {
                "Could not approve emulator post: HTTP ${connection.responseCode}"
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

    private companion object {
        const val USERS_COLLECTION = "users"
        const val POSTS_COLLECTION = "posts"
        const val BADGES_COLLECTION = "badges"
        const val POINT_EVENTS_COLLECTION = "pointEvents"
        const val PUBLIC_USERS_COLLECTION = "users_public"
        const val TEST_PASSWORD = "ServiLocalTest2026"
    }
}
