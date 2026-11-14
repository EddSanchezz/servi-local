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
import com.servilocal.domain.usecase.notifications.CreateNotificationUseCase
import com.servilocal.domain.usecase.notifications.ObserveNotificationsUseCase
import com.servilocal.domain.usecase.moderation.ApprovePostUseCase
import com.servilocal.domain.usecase.moderation.FilterPendingPostsByCategoryUseCase
import com.servilocal.domain.usecase.moderation.ObserveModerationPostsUseCase
import com.servilocal.domain.usecase.moderation.ObserveModeratorProfileUseCase
import com.servilocal.domain.usecase.moderation.ObservePendingPostsUseCase
import com.servilocal.domain.usecase.moderation.ObserveReportsUseCase
import com.servilocal.domain.usecase.moderation.RejectPostUseCase
import com.servilocal.domain.usecase.moderation.ResolveReportUseCase
import com.servilocal.domain.usecase.moderation.ResolveReportsForPostUseCase
import com.servilocal.domain.usecase.moderation.ToggleModeratorShiftUseCase
import com.servilocal.domain.usecase.posts.MarkPostAsResolvedUseCase
import com.servilocal.domain.usecase.posts.CreateServicePostUseCase
import com.servilocal.domain.usecase.posts.FilterPostsByCategoryUseCase
import com.servilocal.domain.usecase.posts.FilterPostsByLocationUseCase
import com.servilocal.domain.usecase.posts.FilterPostsByQueryUseCase
import com.servilocal.domain.usecase.posts.ObservePostsUseCase
import com.servilocal.domain.usecase.posts.TogglePostLikeUseCase
import com.servilocal.domain.repository.ImageStorageRepository
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
import org.junit.Assert.assertNull
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
            LogoutUseCase(sessions),
            MockDataRepository()
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
            RegisterUserUseCase(users),
            testResourceProvider
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
            RegisterUserUseCase(users),
            testResourceProvider
        )

        viewModel.login("valentina@gmail.com", "password123")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.loginResult is RequestResult.Success)
        assertEquals(UserRole.USER, viewModel.uiState.value.authenticatedRole)
        assertEquals("1", sessions.session.value?.userId)
    }

    @Test
    fun `invalid password exposes a login failure`() = runTest {
        val users = UserRepositoryImpl()
        val sessions = SessionRepositoryImpl()
        val viewModel = AuthViewModel(
            ValidateLoginCredentialsUseCase(),
            LoginUserUseCase(users, sessions),
            RegisterUserUseCase(users),
            testResourceProvider
        )

        viewModel.login("valentina@gmail.com", "incorrecta")
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
            RegisterUserUseCase(users),
            testResourceProvider
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
    fun `password recovery confirms the request without exposing a reset code`() = runTest {
        val users = UserRepositoryImpl()
        val codes = PasswordResetRepositoryImpl()
        val viewModel = PasswordRecoveryViewModel(
            RecoverPasswordUseCase(users, codes),
            ResetPasswordUseCase(users, codes),
            testResourceProvider
        )
        val email = "valentina@gmail.com"

        viewModel.onEmailChange(email)
        viewModel.requestCode()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.requestResult is RequestResult.Success)
        assertNull(viewModel.uiState.value.demoCode)
    }

    @Test
    fun `password recovery validates the email and the reset form`() = runTest {
        val users = UserRepositoryImpl()
        val codes = PasswordResetRepositoryImpl()
        val viewModel = PasswordRecoveryViewModel(
            RecoverPasswordUseCase(users, codes),
            ResetPasswordUseCase(users, codes),
            testResourceProvider
        )
        val email = "valentina@gmail.com"

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
    fun `create post validation returns an actionable error and saves valid posts`() = runTest {
        val repository = MockDataRepository()
        var uploadCalls = 0
        val imageStorageRepository = object : ImageStorageRepository {
            override suspend fun uploadImage(localUri: String, folder: String, onProgress: (Float) -> Unit): String {
                uploadCalls++
                onProgress(1f)
                return "https://res.cloudinary.com/test/image/upload/post.jpg"
            }
        }
        val viewModel = CreatePostViewModel(
            CreateServicePostUseCase(repository, repository, CreateNotificationUseCase(repository)),
            imageStorageRepository,
            testResourceProvider
        )

        viewModel.createPost("", "Hogar", "", 0, 0, false, "", null, null)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.submissionError)

        viewModel.onImageSelected("content://test/photo.jpg")
        viewModel.createPost("Pintura", "Hogar", "Pintura interior", 50000, 90000, false, "Armenia", 4.5339, -75.6811)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isCreated)
        assertEquals(1, uploadCalls)
        assertEquals(PostStatus.PENDING_VERIFICATION, repository.posts.value.first().status)
        assertEquals(4.5339, repository.posts.value.first().latitude!!, 0.0001)
        assertEquals(-75.6811, repository.posts.value.first().longitude!!, 0.0001)
        assertEquals(listOf("https://res.cloudinary.com/test/image/upload/post.jpg"), repository.posts.value.first().photos)
    }

    @Test
    fun `home controls keep screen state separate from session`() {
        val repository = MockDataRepository()
        val viewModel = HomeViewModel(
            repository,
            ObserveNotificationsUseCase(SessionRepositoryImpl(), repository),
            TogglePostLikeUseCase(repository, repository, repository, CreateNotificationUseCase(repository)),
            ObservePostsUseCase(repository),
            FilterPostsByCategoryUseCase(),
            FilterPostsByQueryUseCase(),
            FilterPostsByLocationUseCase()
        )

        viewModel.setSearchQuery("electricista")
        viewModel.setSelectedCategory("Electricidad")
        viewModel.setDisplayMode(ViewDisplayMode.MAP)

        assertEquals("electricista", viewModel.uiState.value.searchQuery)
        assertEquals("Electricidad", viewModel.uiState.value.selectedCategory)
        assertEquals(ViewDisplayMode.MAP, viewModel.uiState.value.displayMode)
    }

    @Test
    fun `schedules are available and a regular user cannot toggle the moderator shift`() = runTest {
        val repository = MockDataRepository()
        val notifications = CreateNotificationUseCase(repository)
        val moderationViewModel = ModerationViewModel(
            authRepository = repository,
            observeNotifications = ObserveNotificationsUseCase(SessionRepositoryImpl(), repository),
            observePendingPostsUseCase = ObservePendingPostsUseCase(repository),
            filterPendingPostsByCategoryUseCase = FilterPendingPostsByCategoryUseCase(),
            approvePostUseCase = ApprovePostUseCase(repository, repository, notifications),
            rejectPostUseCase = RejectPostUseCase(repository, repository, notifications),
            markPostAsResolvedUseCase = MarkPostAsResolvedUseCase(repository, repository),
            resolveReportUseCase = ResolveReportUseCase(repository, repository, repository, notifications),
            observeModerationPosts = ObserveModerationPostsUseCase(repository),
            observeReports = ObserveReportsUseCase(repository),
            observeReportHistory = com.servilocal.domain.usecase.moderation.ObserveReportHistoryUseCase(repository),
            observeModeratorProfile = ObserveModeratorProfileUseCase(repository),
            resolveReportsForPostUseCase = ResolveReportsForPostUseCase(repository, repository),
            toggleModeratorShiftUseCase = ToggleModeratorShiftUseCase(repository),
            resources = testResourceProvider
        )

        assertTrue(repository.currentUser.value.workingHours.contains("sábado"))
        assertTrue(repository.moderatorUser.value.workingHours.contains("viernes"))
        assertTrue(repository.moderatorUser.value.activeShift)

        moderationViewModel.toggleModeratorShift()
        advanceUntilIdle()
        assertTrue(repository.moderatorUser.value.activeShift)
    }

    @Test
    fun `application name is available to Android resources`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("ServiLocal", context.getString(R.string.app_name))
    }
}
