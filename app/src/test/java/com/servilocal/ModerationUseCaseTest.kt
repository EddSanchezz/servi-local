package com.servilocal

import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.model.ServicePost
import com.servilocal.domain.model.UserProfile
import com.servilocal.domain.model.UserRole
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.ModerationRepository
import com.servilocal.domain.repository.NotificationRepository
import com.servilocal.domain.repository.PostRepository
import com.servilocal.domain.usecase.moderation.ApprovePostUseCase
import com.servilocal.domain.usecase.moderation.FilterPendingPostsByCategoryUseCase
import com.servilocal.domain.usecase.moderation.ObservePendingPostsUseCase
import com.servilocal.domain.usecase.moderation.RejectPostUseCase
import com.servilocal.features.moderation.viewmodel.ModerationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ModerationUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakePostRepository(initialPosts: List<ServicePost> = emptyList()) : PostRepository {
        private val _posts = MutableStateFlow(initialPosts)
        override val posts: StateFlow<List<ServicePost>> = _posts

        override fun toggleUpvote(postId: String) {}
        override fun addComment(postId: String, text: String, authorName: String, isAuthorReply: Boolean) {}
        override fun createPost(
            title: String,
            category: String,
            description: String,
            minPrice: Int,
            maxPrice: Int,
            isPriceToAgree: Boolean,
            location: String
        ): ServicePost {
            val post = ServicePost(
                id = "post_${System.currentTimeMillis()}",
                title = title,
                category = category,
                description = description,
                providerName = "Test Provider",
                minPrice = minPrice,
                maxPrice = maxPrice,
                isPriceToAgree = isPriceToAgree,
                location = location,
                status = PostStatus.PENDING_VERIFICATION
            )
            _posts.value = _posts.value + post
            return post
        }

        override fun approvePost(postId: String) {
            _posts.value = _posts.value.map {
                if (it.id == postId) it.copy(status = PostStatus.APPROVED) else it
            }
        }

        override fun rejectPost(postId: String, reason: String, notes: String?) {
            _posts.value = _posts.value.map {
                if (it.id == postId) it.copy(status = PostStatus.REJECTED, rejectionReason = reason, rejectionNotes = notes) else it
            }
        }
    }

    private class FakeAuthRepository(
        role: UserRole = UserRole.MODERATOR
    ) : AuthRepository {
        private val _moderatorUser = MutableStateFlow(
            UserProfile(id = "mod_1", name = "Mod", email = "mod@test.com", role = role)
        )
        override val currentUser: StateFlow<UserProfile> = _moderatorUser
        override val moderatorUser: StateFlow<UserProfile> = _moderatorUser

        override fun registerUser(name: String, email: String) {}
        override fun updateCurrentUser(name: String, email: String) {}
        override fun toggleModeratorShift() {}
    }

    private class FakeModerationRepository : ModerationRepository {
        override val reports = MutableStateFlow(emptyList<com.servilocal.domain.model.ModerationReportItem>())
        override fun createReport(reason: String, description: String, targetType: String, targetTitle: String, targetPostId: String?, targetChatId: String?) {}
        override fun dismissReport(reportId: String) {}
        override fun resolveReportsForPost(postId: String) {}
    }

    private class FakeNotificationRepository : NotificationRepository {
        override val notifications = MutableStateFlow(emptyList<com.servilocal.domain.model.NotificationItem>())
        override fun markAllNotificationsRead() {}
        override fun toggleNotificationRead(notificationId: String) {}
    }

    @Test
    fun `ObservePendingPostsUseCase filters only pending posts and orders FIFO by createdAtMillis`() = runTest {
        val post1 = ServicePost(id = "post_2", title = "P1", category = "Hogar", description = "Desc", providerName = "P", status = PostStatus.PENDING_VERIFICATION, createdAtMillis = 2000L)
        val post2 = ServicePost(id = "post_1", title = "P2", category = "Hogar", description = "Desc", providerName = "P", status = PostStatus.PENDING_VERIFICATION, createdAtMillis = 1000L)
        val post3 = ServicePost(id = "post_3", title = "P3", category = "Hogar", description = "Desc", providerName = "P", status = PostStatus.APPROVED, createdAtMillis = 500L)

        val repository = FakePostRepository(listOf(post1, post2, post3))
        val useCase = ObservePendingPostsUseCase(repository)

        val pending = useCase().first()

        assertEquals(2, pending.size)
        assertEquals("post_1", pending[0].id)
        assertEquals("post_2", pending[1].id)
    }

    @Test
    fun `FilterPendingPostsByCategoryUseCase filters by category name, title, or returns all when Todos`() {
        val useCase = FilterPendingPostsByCategoryUseCase()

        val postHogar = ServicePost(id = "1", title = "Reparación de Techos", category = "Hogar", description = "Desc", providerName = "P")
        val postPlomeria = ServicePost(id = "2", title = "Plomería de Urgencia", category = "Hogar", description = "Desc", providerName = "P")
        val postEducacion = ServicePost(id = "3", title = "Clases de Física", category = "Educación", description = "Desc", providerName = "P")

        val posts = listOf(postHogar, postPlomeria, postEducacion)

        val filteredHogar = useCase(posts, "Hogar")
        assertTrue(filteredHogar.contains(postHogar))
        assertTrue(filteredHogar.contains(postPlomeria))
        assertFalse(filteredHogar.contains(postEducacion))

        val filteredEducacion = useCase(posts, "Educación")
        assertEquals(1, filteredEducacion.size)
        assertEquals("3", filteredEducacion.first().id)

        val filteredTodos = useCase(posts, "Todos")
        assertEquals(3, filteredTodos.size)

        val filteredNull = useCase(posts, null)
        assertEquals(3, filteredNull.size)
    }

    @Test
    fun `ApprovePostUseCase approves pending post idempotently when moderator`() {
        val pendingPost = ServicePost(id = "post_pending", title = "P", category = "Hogar", description = "Desc", providerName = "P", status = PostStatus.PENDING_VERIFICATION)
        val approvedPost = ServicePost(id = "post_approved", title = "P2", category = "Hogar", description = "Desc", providerName = "P", status = PostStatus.APPROVED)

        val postRepo = FakePostRepository(listOf(pendingPost, approvedPost))
        val authRepo = FakeAuthRepository(UserRole.MODERATOR)
        val useCase = ApprovePostUseCase(postRepo, authRepo)

        val successFirst = useCase("post_pending")
        assertTrue(successFirst)
        assertEquals(PostStatus.APPROVED, postRepo.posts.value.first { it.id == "post_pending" }.status)

        // Idempotency check: approving non-pending post returns false
        val successSecond = useCase("post_pending")
        assertFalse(successSecond)

        val successAlreadyApproved = useCase("post_approved")
        assertFalse(successAlreadyApproved)
    }

    @Test
    fun `RejectPostUseCase rejects pending post when moderator and fails when non-moderator`() {
        val pendingPost = ServicePost(id = "post_pending", title = "P", category = "Hogar", description = "Desc", providerName = "P", status = PostStatus.PENDING_VERIFICATION)

        val postRepo = FakePostRepository(listOf(pendingPost))
        val userAuthRepo = FakeAuthRepository(UserRole.USER)
        val rejectUseCaseUser = RejectPostUseCase(postRepo, userAuthRepo)

        assertFalse(rejectUseCaseUser("post_pending", "Spam", "Notas"))

        val modAuthRepo = FakeAuthRepository(UserRole.MODERATOR)
        val rejectUseCaseMod = RejectPostUseCase(postRepo, modAuthRepo)

        assertTrue(rejectUseCaseMod("post_pending", "Spam", "Notas"))
        val rejected = postRepo.posts.value.first { it.id == "post_pending" }
        assertEquals(PostStatus.REJECTED, rejected.status)
        assertEquals("Spam", rejected.rejectionReason)
        assertEquals("Notas", rejected.rejectionNotes)
    }

    @Test
    fun `ModerationViewModel updates pendingPosts on category filter change`() = runTest {
        val post1 = ServicePost(id = "1", title = "Instalación", category = "Hogar", description = "Desc", providerName = "P", status = PostStatus.PENDING_VERIFICATION, createdAtMillis = 100L)
        val post2 = ServicePost(id = "2", title = "Clases", category = "Educación", description = "Desc", providerName = "P", status = PostStatus.PENDING_VERIFICATION, createdAtMillis = 200L)

        val postRepo = FakePostRepository(listOf(post1, post2))
        val modRepo = FakeModerationRepository()
        val notifRepo = FakeNotificationRepository()
        val authRepo = FakeAuthRepository()

        val viewModel = ModerationViewModel(
            postRepository = postRepo,
            moderationRepository = modRepo,
            notificationRepository = notifRepo,
            authRepository = authRepo
        )

        testScheduler.advanceUntilIdle()

        viewModel.setCategoryFilter("Educación")
        testScheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertEquals("Educación", uiState.selectedCategory)

        viewModel.approvePost("1")
        testScheduler.advanceUntilIdle()

        viewModel.setCategoryFilter("Todos")
        testScheduler.advanceUntilIdle()
    }
}
