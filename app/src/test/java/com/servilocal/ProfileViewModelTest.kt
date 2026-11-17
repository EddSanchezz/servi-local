package com.servilocal

import com.servilocal.core.data.repository.MockDataRepository
import com.servilocal.data.repository.memory.BadgeRepositoryImpl
import com.servilocal.data.repository.memory.SessionRepositoryImpl
import com.servilocal.data.repository.memory.UserRepositoryImpl
import com.servilocal.domain.model.UserRole
import com.servilocal.domain.repository.ImageStorageRepository
import com.servilocal.domain.repository.PostRepository
import com.servilocal.domain.usecase.auth.DeleteAccountUseCase
import com.servilocal.domain.usecase.moderation.ObserveReportHistoryUseCase
import com.servilocal.domain.usecase.notifications.CreateNotificationUseCase
import com.servilocal.domain.usecase.notifications.ObserveNotificationsUseCase
import com.servilocal.domain.usecase.posts.DeleteServicePostUseCase
import com.servilocal.domain.usecase.posts.ObserveMyPostsUseCase
import com.servilocal.domain.usecase.profile.CheckAndAwardBadgesUseCase
import com.servilocal.domain.usecase.profile.GetProfileStatsUseCase
import com.servilocal.domain.usecase.profile.ObserveUserBadgesUseCase
import com.servilocal.domain.usecase.user.ObserveCurrentUserUseCase
import com.servilocal.features.profile.viewmodel.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ProfileViewModelTest {
    private val mainDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `profile remains available when badge synchronization fails`() = runTest(mainDispatcher) {
        val authAndData = MockDataRepository()
        val users = UserRepositoryImpl()
        val sessions = SessionRepositoryImpl()
        sessions.saveSession("1", UserRole.USER)
        val badgeRepository = BadgeRepositoryImpl()
        val postRepository = object : PostRepository by authAndData {
            override suspend fun countPostsByProvider(providerId: String): Int =
                error("Simulated Firestore badge synchronization failure")
        }
        val notifications = CreateNotificationUseCase(authAndData)
        val imageStorage = object : ImageStorageRepository {
            override suspend fun uploadImage(
                localUri: String,
                folder: String,
                onProgress: (Float) -> Unit
            ): String = error("Not used by this test")
        }
        val checkBadges = CheckAndAwardBadgesUseCase(
            sessionRepository = sessions,
            userRepository = users,
            badgeRepository = badgeRepository,
            postRepository = postRepository,
            createNotification = notifications
        )
        val viewModel = ProfileViewModel(
            authRepository = authAndData,
            imageStorageRepository = imageStorage,
            postRepository = postRepository,
            observeNotifications = ObserveNotificationsUseCase(sessions, authAndData),
            observeUserBadges = ObserveUserBadgesUseCase(sessions, badgeRepository),
            observeMyPosts = ObserveMyPostsUseCase(postRepository, authAndData),
            observeReportHistory = ObserveReportHistoryUseCase(authAndData),
            deleteServicePost = DeleteServicePostUseCase(postRepository),
            deleteAccountUseCase = DeleteAccountUseCase(sessions, users, postRepository, authAndData, authAndData),
            getProfileStats = GetProfileStatsUseCase(
                ObserveCurrentUserUseCase(sessions, users),
                postRepository,
                badgeRepository,
                authAndData
            ),
            checkAndAwardBadges = checkBadges
        )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.stats)
    }
}
