package com.servilocal

import com.servilocal.core.data.repository.MockDataRepository
import com.servilocal.data.repository.memory.SessionRepositoryImpl
import com.servilocal.data.repository.memory.UserRepositoryImpl
import com.servilocal.domain.model.UserRole
import com.servilocal.domain.usecase.auth.DeleteAccountResult
import com.servilocal.domain.usecase.auth.DeleteAccountUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeleteAccountUseCaseTest {

    private lateinit var users: UserRepositoryImpl
    private lateinit var sessions: SessionRepositoryImpl
    private lateinit var content: MockDataRepository
    private lateinit var deleteAccount: DeleteAccountUseCase

    @Before
    fun setUp() {
        users = UserRepositoryImpl()
        sessions = SessionRepositoryImpl()
        content = MockDataRepository(users, sessions)
        deleteAccount = DeleteAccountUseCase(sessions, users, content, content)
    }

    @Test
    fun `requires an active session before deleting`() = runTest {
        val result = deleteAccount()

        assertEquals(DeleteAccountResult.NoActiveSession, result)
        assertNotNull(users.findById("1"))
    }

    @Test
    fun `deletes the profile posts and conversations in cascade`() = runTest {
        sessions.saveSession("1", UserRole.USER)

        assertTrue(content.posts.value.any { it.providerId == "1" })
        assertTrue(content.conversations.value.any { it.clientId == "1" })

        val result = deleteAccount()

        assertEquals(DeleteAccountResult.Success, result)
        assertNull(users.findById("1"))
        assertNull(sessions.session.value)
        assertTrue(content.posts.value.none { it.providerId == "1" })
        assertTrue(
            content.conversations.value.none {
                it.clientId == "1" || it.providerId == "1"
            }
        )
    }
}
