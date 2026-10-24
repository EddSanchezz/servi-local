package com.servilocal

import com.servilocal.data.repository.memory.SessionRepositoryImpl
import com.servilocal.data.repository.memory.UserRepositoryImpl
import com.servilocal.domain.model.UserRole
import com.servilocal.domain.usecase.auth.LoginUserUseCase
import com.servilocal.domain.usecase.auth.LogoutUseCase
import com.servilocal.domain.usecase.auth.ObserveSessionUseCase
import com.servilocal.domain.usecase.auth.RecoverPasswordResult
import com.servilocal.domain.usecase.auth.RecoverPasswordUseCase
import com.servilocal.domain.usecase.auth.RegisterResult
import com.servilocal.domain.usecase.auth.RegisterUserUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthUseCaseTest {

    private lateinit var users: UserRepositoryImpl
    private lateinit var sessions: SessionRepositoryImpl
    private lateinit var login: LoginUserUseCase
    private lateinit var register: RegisterUserUseCase

    @Before
    fun setUp() {
        users = UserRepositoryImpl()
        sessions = SessionRepositoryImpl()
        login = LoginUserUseCase(users, sessions)
        register = RegisterUserUseCase(users)
    }

    @Test
    fun `login with seed credentials creates the session`() = runTest {
        val user = login("valentina.morales@servilocal.co", "servilocal2026")

        assertNotNull(user)
        assertEquals("user_val_1", user?.id)
        assertEquals(UserRole.USER, sessions.session.value?.role)
        assertEquals("user_val_1", sessions.session.value?.userId)
    }

    @Test
    fun `login normalizes the email before authenticating`() = runTest {
        val user = login("  VALENTINA.MORALES@SERVILOCAL.CO  ", "servilocal2026")

        assertEquals("user_val_1", user?.id)
    }

    @Test
    fun `login with wrong password returns null and keeps the session empty`() = runTest {
        val user = login("valentina.morales@servilocal.co", "incorrecta")

        assertNull(user)
        assertNull(sessions.session.value)
    }

    @Test
    fun `login with unknown email returns null`() = runTest {
        val user = login("nadie@example.com", "servilocal2026")

        assertNull(user)
        assertNull(sessions.session.value)
    }

    @Test
    fun `login with blank fields returns null`() = runTest {
        assertNull(login(" ", "servilocal2026"))
        assertNull(login("valentina.morales@servilocal.co", " "))
    }

    @Test
    fun `moderator login preserves the moderator role`() = runTest {
        val user = login("alejandro.vargas@staff.servilocal.pe", "servilocal2026")

        assertEquals(UserRole.MODERATOR, user?.role)
        assertEquals(UserRole.MODERATOR, sessions.session.value?.role)
    }

    @Test
    fun `registration saves a normalized user with a hashed password`() = runTest {
        val result = register("  Ana Torres  ", "  ANA@EXAMPLE.COM  ", "clave2026")

        assertTrue(result is RegisterResult.Success)
        val saved = users.findByEmail("ana@example.com")
        assertNotNull(saved)
        assertEquals("Ana Torres", saved?.name)
        assertEquals("ana@example.com", saved?.email)
        assertEquals(UserRole.USER, saved?.role)
        assertNotEquals("clave2026", saved?.passwordHash)
        assertNull(sessions.session.value)
    }

    @Test
    fun `registration rejects a duplicated email ignoring case`() = runTest {
        register("Ana Torres", "ana@example.com", "clave2026")

        val duplicate = register("Otra Ana", "ANA@EXAMPLE.COM", "otraclave")

        assertEquals(RegisterResult.EmailAlreadyExists, duplicate)
    }

    @Test
    fun `registration validates name email and password`() = runTest {
        assertEquals(RegisterResult.InvalidName, register(" ", "ana@example.com", "clave2026"))
        assertEquals(RegisterResult.InvalidEmail, register("Ana", "correo-invalido", "clave2026"))
        assertEquals(RegisterResult.WeakPassword, register("Ana", "ana@example.com", "1234567"))
    }

    @Test
    fun `logout clears the active session`() = runTest {
        login("valentina.morales@servilocal.co", "servilocal2026")
        assertNotNull(sessions.session.value)

        LogoutUseCase(sessions)()

        assertNull(sessions.session.value)
    }

    @Test
    fun `observe session exposes the repository flow`() = runTest {
        val observed = ObserveSessionUseCase(sessions)
        assertNull(observed().value)

        login("alejandro.vargas@staff.servilocal.pe", "servilocal2026")

        assertEquals("mod_ale_1", observed().value?.userId)
        assertEquals(UserRole.MODERATOR, observed().value?.role)
    }

    @Test
    fun `recover password validates the email and the existing account`() = runTest {
        val recover = RecoverPasswordUseCase(users)

        assertEquals(RecoverPasswordResult.InvalidEmail, recover("correo-invalido"))
        assertEquals(RecoverPasswordResult.UserNotFound, recover("nadie@example.com"))
        assertEquals(
            RecoverPasswordResult.Success,
            recover("  VALENTINA.MORALES@SERVILOCAL.CO  ")
        )
    }

    @Test
    fun `a registered user can log in afterwards`() = runTest {
        val registered = register("Ana Torres", "ana@example.com", "clave2026")
        assertTrue(registered is RegisterResult.Success)

        val user = login("ana@example.com", "clave2026")

        assertEquals((registered as RegisterResult.Success).user.id, user?.id)
        assertEquals("ana@example.com", sessions.session.value?.let { users.findById(it.userId)?.email })
    }
}
