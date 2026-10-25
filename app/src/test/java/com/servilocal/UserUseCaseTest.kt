package com.servilocal

import com.servilocal.data.repository.memory.SessionRepositoryImpl
import com.servilocal.data.repository.memory.UserRepositoryImpl
import com.servilocal.domain.model.UserRole
import com.servilocal.domain.usecase.user.GetCurrentUserUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class UserUseCaseTest {

    private lateinit var users: UserRepositoryImpl
    private lateinit var sessions: SessionRepositoryImpl
    private lateinit var getCurrentUser: GetCurrentUserUseCase

    @Before
    fun setUp() {
        users = UserRepositoryImpl()
        sessions = SessionRepositoryImpl()
        getCurrentUser = GetCurrentUserUseCase(sessions, users)
    }

    @Test
    fun `current user returns the owner of the active session`() = runTest {
        sessions.saveSession(1L, UserRole.USER)

        val user = getCurrentUser()

        assertEquals(1L, user?.id)
        assertEquals("Valentina Morales", user?.name)
        assertEquals(UserRole.USER, user?.role)
    }

    @Test
    fun `current user follows the session when it changes`() = runTest {
        sessions.saveSession(2L, UserRole.USER)

        assertEquals(2L, getCurrentUser()?.id)

        sessions.saveSession(3L, UserRole.MODERATOR)

        assertEquals(3L, getCurrentUser()?.id)
        assertEquals(UserRole.MODERATOR, getCurrentUser()?.role)
    }

    @Test
    fun `current user is null without an active session`() = runTest {
        assertNull(getCurrentUser())
    }

    @Test
    fun `current user is null when the session points to an unknown user`() = runTest {
        sessions.saveSession(999L, UserRole.USER)

        assertNull(getCurrentUser())
    }
}
