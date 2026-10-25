package com.servilocal

import com.servilocal.core.data.repository.MockDataRepository
import com.servilocal.core.util.relativeTime
import com.servilocal.data.repository.memory.SessionRepositoryImpl
import com.servilocal.domain.model.NotificationType
import com.servilocal.domain.model.UserRole
import com.servilocal.domain.usecase.notifications.CreateNotificationUseCase
import com.servilocal.domain.usecase.notifications.MarkNotificationReadUseCase
import com.servilocal.domain.usecase.notifications.ObserveNotificationsUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationUseCaseTest {

    private lateinit var notifications: MockDataRepository
    private lateinit var sessions: SessionRepositoryImpl
    private lateinit var observeNotifications: ObserveNotificationsUseCase
    private lateinit var markNotificationRead: MarkNotificationReadUseCase
    private lateinit var createNotification: CreateNotificationUseCase

    @Before
    fun setUp() {
        notifications = MockDataRepository()
        sessions = SessionRepositoryImpl()
        observeNotifications = ObserveNotificationsUseCase(sessions, notifications)
        markNotificationRead = MarkNotificationReadUseCase(sessions, notifications)
        createNotification = CreateNotificationUseCase(notifications)
    }

    @Test
    fun `observe notifications filters by session and sorts the newest first`() = runTest {
        sessions.saveSession(1L, UserRole.USER)

        val result = observeNotifications().first()

        assertEquals(5, result.size)
        assertTrue(result.all { it.userId == 1L })
        assertEquals(result.sortedByDescending { it.createdAtMillis }, result)
    }

    @Test
    fun `observe notifications is empty without an active session`() = runTest {
        assertTrue(observeNotifications().first().isEmpty())
    }

    @Test
    fun `create notification delivers the event to the recipient`() = runTest {
        sessions.saveSession(1L, UserRole.USER)

        val created = createNotification(
            userId = 1L,
            title = "Nuevo comentario",
            body = "Alguien respondió a tu publicación.",
            type = NotificationType.COMMENT,
            relatedPostId = "pub_1"
        )

        assertNotNull(created)
        val result = observeNotifications().first()
        assertTrue(result.any { it.id == created?.id && it.type == NotificationType.COMMENT })
    }

    @Test
    fun `mark notification read only touches the session user`() = runTest {
        sessions.saveSession(1L, UserRole.USER)
        val own = createNotification(1L, "Aviso", "Mensaje para Valentina")
        val foreign = createNotification(2L, "Aviso", "Mensaje para Carlos")
        assertNotNull(own)
        assertNotNull(foreign)

        assertTrue(markNotificationRead(own!!.id))
        assertFalse(markNotificationRead(foreign!!.id))

        val ownStored = notifications.notifications.value.first { it.id == own.id }
        val foreignStored = notifications.notifications.value.first { it.id == foreign.id }
        assertTrue(ownStored.isRead)
        assertFalse(foreignStored.isRead)
    }

    @Test
    fun `mark all read only affects the notifications of the session user`() = runTest {
        sessions.saveSession(1L, UserRole.USER)
        createNotification(2L, "Aviso", "Mensaje para Carlos")

        assertTrue(markNotificationRead())

        val all = notifications.notifications.value
        assertTrue(all.filter { it.userId == 1L }.all { it.isRead })
        assertTrue(all.filter { it.userId == 2L }.none { it.isRead })
    }

    @Test
    fun `relative time describes recent notifications`() {
        val now = 1_000_000_000L
        assertEquals("Ahora", relativeTime(now - 10_000, now))
        assertEquals("Hace 10 min", relativeTime(now - 10 * 60_000, now))
        assertEquals("Hace 2 h", relativeTime(now - 2 * 60 * 60_000, now))
        assertEquals("Ayer", relativeTime(now - 30 * 60 * 60_000, now))
        assertEquals("Hace 3 días", relativeTime(now - 3 * 24 * 60 * 60_000, now))
    }
}
