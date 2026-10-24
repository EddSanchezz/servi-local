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
        sessions.saveSession("user_val_1", UserRole.USER)

        val user = getCurrentUser()

        assertEquals("user_val_1", user?.id)
        assertEquals("Valentina Morales", user?.name)
        assertEquals(UserRole.USER, user?.role)
    }

    @Test
    fun `current user follows the session when it changes`() = runTest {
        sessions.saveSession("user_car_1", UserRole.USER)

        assertEquals("user_car_1", getCurrentUser()?.id)

        sessions.saveSession("mod_ale_1", UserRole.MODERATOR)

        assertEquals("mod_ale_1", getCurrentUser()?.id)
        assertEquals(UserRole.MODERATOR, getCurrentUser()?.role)
    }

    @Test
    fun `current user is null without an active session`() = runTest {
        assertNull(getCurrentUser())
    }

    @Test
    fun `current user is null when the session points to an unknown user`() = runTest {
        sessions.saveSession("user_missing", UserRole.USER)

        assertNull(getCurrentUser())
    }
}
