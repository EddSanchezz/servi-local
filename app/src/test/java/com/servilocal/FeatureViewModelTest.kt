package com.servilocal

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.servilocal.core.util.RequestResult
import com.servilocal.data.repository.memory.PasswordResetRepositoryImpl
import com.servilocal.data.repository.memory.SessionRepositoryImpl
import com.servilocal.data.repository.memory.UserRepositoryImpl
import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.model.UserRole
import com.servilocal.core.data.repository.MockDataRepository
import com.servilocal.features.app.viewmodel.AppViewModel
import com.servilocal.features.auth.viewmodel.AuthViewModel
import com.servilocal.features.auth.viewmodel.PasswordRecoveryViewModel
import com.servilocal.features.moderation.viewmodel.ModerationViewModel
import com.servilocal.features.posts.viewmodel.CreatePostViewModel
import com.servilocal.domain.usecase.auth.LoginUserUseCase
import com.servilocal.domain.usecase.auth.LogoutUseCase
import com.servilocal.domain.usecase.auth.ObserveSessionUseCase
import com.servilocal.domain.usecase.auth.RecoverPasswordUseCase
import com.servilocal.domain.usecase.auth.RegisterUserUseCase
import com.servilocal.domain.usecase.auth.ResetPasswordUseCase
import com.servilocal.domain.usecase.auth.ValidateLoginCredentialsUseCase
import com.servilocal.domain.usecase.posts.CreateServicePostUseCase
import com.servilocal.features.home.viewmodel.HomeViewModel
import com.servilocal.features.home.viewmodel.ViewDisplayMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FeatureViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `app session changes role without owning navigation`() = runTest {
        val sessions = SessionRepositoryImpl()
        val viewModel = AppViewModel(
            ObserveSessionUseCase(sessions),
            LogoutUseCase(sessions)
        )

        viewModel.login(UserRole.MODERATOR)
        assertEquals(UserRole.MODERATOR, viewModel.uiState.value.activeRole)

        viewModel.logout()
        advanceUntilIdle()
        assertEquals(UserRole.USER, viewModel.uiState.value.activeRole)
        assertEquals(null, sessions.session.value)
    }

    @Test
    fun `login validation returns feedback for malformed fields`() = runTest {
        val users = UserRepositoryImpl()
        val viewModel = AuthViewModel(
            ValidateLoginCredentialsUseCase(),
            LoginUserUseCase(users, SessionRepositoryImpl()),
            RegisterUserUseCase(users)
        )

        viewModel.login("not-an-email", "123")

        assertEquals("Ingresa un correo válido.", viewModel.uiState.value.emailError)
        assertEquals("Usa al menos 6 caracteres.", viewModel.uiState.value.passwordError)
    }

    @Test
    fun `valid login authenticates a seed user and creates the session`() = runTest {
        val users = UserRepositoryImpl()
        val sessions = SessionRepositoryImpl()
        val viewModel = AuthViewModel(
            ValidateLoginCredentialsUseCase(),
            LoginUserUseCase(users, sessions),
            RegisterUserUseCase(users)
        )

        viewModel.login("valentina.morales@servilocal.co", "servilocal2026")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.loginResult is RequestResult.Success)
        assertEquals(UserRole.USER, viewModel.uiState.value.authenticatedRole)
        assertEquals("user_val_1", sessions.session.value?.userId)
    }

    @Test
    fun `invalid password exposes a login failure`() = runTest {
        val users = UserRepositoryImpl()
        val sessions = SessionRepositoryImpl()
        val viewModel = AuthViewModel(
            ValidateLoginCredentialsUseCase(),
            LoginUserUseCase(users, sessions),
            RegisterUserUseCase(users)
        )

        viewModel.login("valentina.morales@servilocal.co", "incorrecta")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.loginResult is RequestResult.Failure)
        assertEquals(null, sessions.session.value)
    }

    @Test
    fun `registration rejects missing name and accepts valid details`() = runTest {
        val users = UserRepositoryImpl()
        val viewModel = AuthViewModel(
            ValidateLoginCredentialsUseCase(),
            LoginUserUseCase(users, SessionRepositoryImpl()),
            RegisterUserUseCase(users)
        )

        viewModel.register(" ", "ana@example.com", "clave2026")
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.nameError)

        viewModel.register("Ana Torres", "ana@example.com", "clave2026")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.registerResult is RequestResult.Success)
        assertNotNull(users.findByEmail("ana@example.com"))
    }

    @Test
    fun `password recovery generates a code and resets the account`() = runTest {
        val users = UserRepositoryImpl()
        val codes = PasswordResetRepositoryImpl()
        val viewModel = PasswordRecoveryViewModel(
            RecoverPasswordUseCase(users, codes),
            ResetPasswordUseCase(users, codes)
        )
        val email = "valentina.morales@servilocal.co"

        viewModel.onEmailChange(email)
        viewModel.requestCode()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.requestResult is RequestResult.Success)
        val code = viewModel.uiState.value.demoCode
        assertNotNull(code)
        assertEquals(5, code?.length)

        viewModel.prepareReset(email)
        code!!.forEachIndexed { index, digit ->
            viewModel.onCodeDigitChange(index, digit.toString())
        }
        viewModel.onNewPasswordChange("nuevaClave2026")
        viewModel.onConfirmPasswordChange("nuevaClave2026")
        viewModel.submitReset()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.resetResult is RequestResult.Success)
        assertNotNull(users.login(email, "nuevaClave2026"))
    }

    @Test
    fun `password recovery validates the email and the reset form`() = runTest {
        val users = UserRepositoryImpl()
        val codes = PasswordResetRepositoryImpl()
        val viewModel = PasswordRecoveryViewModel(
            RecoverPasswordUseCase(users, codes),
            ResetPasswordUseCase(users, codes)
        )
        val email = "valentina.morales@servilocal.co"

        viewModel.onEmailChange("correo-invalido")
        viewModel.requestCode()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.emailError)

        viewModel.prepareReset(email)
        viewModel.submitReset()
        assertNotNull(viewModel.uiState.value.codeError)

        repeat(5) { index -> viewModel.onCodeDigitChange(index, "7") }
        viewModel.onNewPasswordChange("corta")
        viewModel.onConfirmPasswordChange("otra")
        viewModel.submitReset()

        assertNotNull(viewModel.uiState.value.passwordError)
        assertNotNull(viewModel.uiState.value.confirmPasswordError)
    }

    @Test
    fun `create post validation returns an actionable error and saves valid posts`() {
        val repository = MockDataRepository()
        val viewModel = CreatePostViewModel(CreateServicePostUseCase(repository))

        assertFalse(viewModel.createPost("", "Hogar", "", 0, 0, false, ""))
        assertNotNull(viewModel.uiState.value.submissionError)

        assertTrue(viewModel.createPost("Pintura", "Hogar", "Pintura interior", 50000, 90000, false, "Armenia"))
        assertEquals(PostStatus.PENDING_VERIFICATION, repository.posts.value.first().status)
    }

    @Test
    fun `home controls keep screen state separate from session`() {
        val repository = MockDataRepository()
        val viewModel = HomeViewModel(repository, repository, repository)

        viewModel.setSearchQuery("electricista")
        viewModel.setSelectedCategory("Electricidad")
        viewModel.setDisplayMode(ViewDisplayMode.MAP)

        assertEquals("electricista", viewModel.uiState.value.searchQuery)
        assertEquals("Electricidad", viewModel.uiState.value.selectedCategory)
        assertEquals(ViewDisplayMode.MAP, viewModel.uiState.value.displayMode)
    }

    @Test
    fun `user and moderator schedules are available and moderator shift can be toggled`() {
        val repository = MockDataRepository()
        val moderationViewModel = ModerationViewModel(repository, repository, repository, repository)

        assertTrue(repository.currentUser.value.workingHours.contains("sábado"))
        assertTrue(repository.moderatorUser.value.workingHours.contains("viernes"))
        assertTrue(repository.moderatorUser.value.activeShift)

        moderationViewModel.toggleModeratorShift()
        assertFalse(repository.moderatorUser.value.activeShift)
    }

    @Test
    fun `application name is available to Android resources`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("ServiLocal", context.getString(R.string.app_name))
    }
}
