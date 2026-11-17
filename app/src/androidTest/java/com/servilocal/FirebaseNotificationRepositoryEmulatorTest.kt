package com.servilocal

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.servilocal.core.di.FirebaseModule
import com.servilocal.data.repository.firebase.FirebaseNotificationRepository
import com.servilocal.data.repository.memory.SessionRepositoryImpl
import com.servilocal.domain.model.AppNotification
import com.servilocal.domain.model.NotificationType
import com.servilocal.domain.model.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class FirebaseNotificationRepositoryEmulatorTest {

    @Test
    fun notificationPersistsAcrossRepositoryInstancesAndReadStateIsScoped() = runBlocking {
        assertTrue("Enable FIREBASE_EMULATOR_ENABLED for this instrumentation test", BuildConfig.FIREBASE_EMULATOR_ENABLED)

        val auth = FirebaseModule.provideFirebaseAuth()
        val firestore = FirebaseModule.provideFirebaseFirestore()
        auth.signOut()
        val email = "notifications-${UUID.randomUUID()}@servilocal.test"
        val password = "ServiLocalTest2026"
        val firebaseUser = auth.createUserWithEmailAndPassword(email, password).await().user
            ?: error("Auth Emulator did not create notification test user")
        val userId = firebaseUser.uid
        val userReference = firestore.collection(USERS_COLLECTION).document(userId)
        val session = SessionRepositoryImpl()
        val notificationIds = listOf("notif-${UUID.randomUUID()}", "notif-${UUID.randomUUID()}")

        try {
            userReference.set(mapOf(
                "name" to "Usuario de notificaciones",
                "email" to email,
                "role" to UserRole.USER.name,
                "points" to 0,
                "level" to "PRINCIPIANTE",
                "status" to "ACTIVE",
                "createdAtMillis" to System.currentTimeMillis(),
                "photoUrl" to null
            )).await()
            session.saveSession(userId, UserRole.USER)

            val repository = FirebaseNotificationRepository(firestore, session)
            val firstNotification = AppNotification(
                id = notificationIds[0],
                userId = userId,
                title = "Insignia desbloqueada",
                body = "Desbloqueaste tu primera insignia.",
                type = NotificationType.ACHIEVEMENT,
                createdAtMillis = System.currentTimeMillis()
            )
            repository.create(firstNotification)

            val observed = withTimeout(10_000) {
                repository.notifications.first { items -> items.any { it.id == firstNotification.id } }
            }.first { it.id == firstNotification.id }
            assertEquals(firstNotification.title, observed.title)
            assertFalse(observed.isRead)
            assertEquals(userId, firestore.collection(NOTIFICATIONS_COLLECTION)
                .document(firstNotification.id).get().await().getString("createdByUserId"))

            val reopenedRepository = FirebaseNotificationRepository(firestore, session)
            val reloaded = withTimeout(10_000) {
                reopenedRepository.notifications.first { items -> items.any { it.id == firstNotification.id } }
            }.first { it.id == firstNotification.id }
            assertEquals(firstNotification.body, reloaded.body)

            assertFalse(repository.markRead(firstNotification.id, "someone-else", true))
            assertTrue(repository.markRead(firstNotification.id, userId, true))
            withTimeout(10_000) {
                repository.notifications.first { items -> items.any { it.id == firstNotification.id && it.isRead } }
            }

            repository.create(firstNotification.copy(
                id = notificationIds[1],
                title = "Nueva actualización",
                type = NotificationType.SYSTEM,
                isRead = false,
                createdAtMillis = System.currentTimeMillis() + 1
            ))
            withTimeout(10_000) {
                repository.notifications.first { items -> items.any { it.id == notificationIds[1] } }
            }
            repository.markAllRead(userId)
            withTimeout(10_000) {
                repository.notifications.first { items ->
                    items.size >= 2 && items.filter { it.id in notificationIds }.all(AppNotification::isRead)
                }
            }

            assertNotNull(firestore.collection(NOTIFICATIONS_COLLECTION).document(notificationIds[0]).get().await().data)
        } finally {
            notificationIds.forEach { id ->
                runCatching { firestore.collection(NOTIFICATIONS_COLLECTION).document(id).delete().await() }
            }
            runCatching { userReference.delete().await() }
            auth.signOut()
            runCatching { auth.signInWithEmailAndPassword(email, password).await().user?.delete()?.await() }
            auth.signOut()
        }
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val NOTIFICATIONS_COLLECTION = "notifications"
    }
}
