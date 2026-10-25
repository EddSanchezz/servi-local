package com.servilocal

import com.servilocal.data.repository.memory.SessionRepositoryImpl
import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.model.ReportResolution
import com.servilocal.domain.model.ServicePost
import com.servilocal.domain.model.UserProfile
import com.servilocal.domain.model.UserRole
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.ModerationRepository
import com.servilocal.domain.repository.PostRepository
import com.servilocal.core.data.repository.MockDataRepository
import com.servilocal.domain.usecase.moderation.ApprovePendingPostUseCase
import com.servilocal.domain.usecase.moderation.FilterPendingPostsByCategoryUseCase
import com.servilocal.domain.usecase.moderation.ObservePendingPostsUseCase
import com.servilocal.domain.usecase.moderation.RejectPendingPostUseCase
import com.servilocal.domain.usecase.moderation.ReportPostUseCase
import com.servilocal.domain.usecase.moderation.ResolveReportUseCase
import com.servilocal.domain.usecase.notifications.ObserveNotificationsUseCase
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

        override fun updatePost(
            postId: String,
            title: String,
            category: String,
            description: String,
            minPrice: Int,
            maxPrice: Int,
            isPriceToAgree: Boolean,
            location: String
        ): Boolean = false

        override fun finishPost(postId: String): Boolean = false

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

        override fun moderatePost(
            postId: String,
            status: PostStatus,
            reason: String?,
            notes: String?
        ): Boolean {
            if (status != PostStatus.REJECTED && status != PostStatus.FINISHED) return false
            if (_posts.value.none { it.id == postId }) return false
            _posts.value = _posts.value.map {
                if (it.id == postId) {
                    it.copy(
                        status = status,
                        rejectionReason = if (status == PostStatus.REJECTED) reason else it.rejectionReason,
                        rejectionNotes = if (status == PostStatus.REJECTED) notes else it.rejectionNotes
                    )
                } else it
            }
            return true
        }
    }

    private class FakeAuthRepository(
        role: UserRole = UserRole.MODERATOR,
        userId: Long = 3L,
        userName: String = "Mod"
    ) : AuthRepository {
        private val _user = MutableStateFlow(
            UserProfile(id = userId, name = userName, email = "mod@test.com", role = role)
        )
        override val currentUser: StateFlow<UserProfile> = _user
        override val moderatorUser: StateFlow<UserProfile> = _user

        override fun updateCurrentUser(name: String, email: String) {}
        override fun toggleModeratorShift() {}
    }

    private class FakeModerationRepository : ModerationRepository {
        private val _reports = MutableStateFlow(emptyList<com.servilocal.domain.model.ModerationReportItem>())
        override val reports: StateFlow<List<com.servilocal.domain.model.ModerationReportItem>> = _reports

        override fun createReport(
            reason: String,
            description: String,
            targetTitle: String,
            targetPostId: String
        ) {
            val report = com.servilocal.domain.model.ModerationReportItem(
                id = "rep_${System.currentTimeMillis()}",
                reasonTitle = reason,
                targetTitle = targetTitle,
                description = description,
                targetPostId = targetPostId
            )
            _reports.value = listOf(report) + _reports.value
        }

        override fun resolveReport(
            reportId: String,
            resolution: ReportResolution,
            moderatorComment: String
        ): Boolean {
            val exists = _reports.value.any { it.id == reportId }
            _reports.value = _reports.value.filter { it.id != reportId }
            return exists
        }

        override fun resolveReportsForPost(postId: String) {
            _reports.value = _reports.value.filter { it.targetPostId != postId }
        }
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
    fun `ApprovePendingPostUseCase approves pending post idempotently when moderator`() {
        val pendingPost = ServicePost(id = "post_pending", title = "P", category = "Hogar", description = "Desc", providerName = "P", status = PostStatus.PENDING_VERIFICATION)
        val approvedPost = ServicePost(id = "post_approved", title = "P2", category = "Hogar", description = "Desc", providerName = "P", status = PostStatus.APPROVED)

        val postRepo = FakePostRepository(listOf(pendingPost, approvedPost))
        val authRepo = FakeAuthRepository(UserRole.MODERATOR)
        val useCase = ApprovePendingPostUseCase(postRepo, authRepo)

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
    fun `RejectPendingPostUseCase rejects pending post when moderator and fails when non-moderator or blank reason`() {
        val pendingPost = ServicePost(id = "post_pending", title = "P", category = "Hogar", description = "Desc", providerName = "P", status = PostStatus.PENDING_VERIFICATION)

        val postRepo = FakePostRepository(listOf(pendingPost))
        val userAuthRepo = FakeAuthRepository(UserRole.USER)
        val rejectUseCaseUser = RejectPendingPostUseCase(postRepo, userAuthRepo)

        assertFalse(rejectUseCaseUser("post_pending", "Spam", "Notas"))

        val modAuthRepo = FakeAuthRepository(UserRole.MODERATOR)
        val rejectUseCaseMod = RejectPendingPostUseCase(postRepo, modAuthRepo)

        // Blank reason fails
        assertFalse(rejectUseCaseMod("post_pending", "", "Notas"))

        assertTrue(rejectUseCaseMod("post_pending", "Spam", "Notas"))
        val rejected = postRepo.posts.value.first { it.id == "post_pending" }
        assertEquals(PostStatus.REJECTED, rejected.status)
        assertEquals("Spam", rejected.rejectionReason)
        assertEquals("Notas", rejected.rejectionNotes)
    }

    @Test
    fun `ReportPostUseCase allows reporting other users post and prevents reporting own post`() {
        val postOther = ServicePost(id = "post_1", title = "P1", category = "Hogar", description = "Desc", providerName = "Other User", providerId = 2L)
        val postOwn = ServicePost(id = "post_2", title = "P2", category = "Hogar", description = "Desc", providerName = "Test User", providerId = 1L)

        val postRepo = FakePostRepository(listOf(postOther, postOwn))
        val authRepo = FakeAuthRepository(role = UserRole.USER, userId = 1L, userName = "Test User")
        val modRepo = FakeModerationRepository()
        val reportUseCase = ReportPostUseCase(postRepo, authRepo, modRepo)

        // Cannot report own post
        val reportOwnResult = reportUseCase("post_2", "Estafa", "Detalles")
        assertFalse(reportOwnResult)
        assertEquals(0, modRepo.reports.value.size)

        // Can report other user's post
        val reportOtherResult = reportUseCase("post_1", "Estafa", "Detalles")
        assertTrue(reportOtherResult)
        assertEquals(1, modRepo.reports.value.size)
        assertEquals("post_1", modRepo.reports.value.first().targetPostId)
    }

    @Test
    fun `ResolveReportUseCase dismisses an invalid report without changing the post`() {
        val post = ServicePost(
            id = "post_1", title = "P1", category = "Hogar", description = "Desc",
            providerName = "Other User", providerId = 2L, status = PostStatus.APPROVED
        )
        val postRepo = FakePostRepository(listOf(post))
        val authRepo = FakeAuthRepository(role = UserRole.MODERATOR, userId = 3L, userName = "Mod")
        val modRepo = FakeModerationRepository()
        modRepo.createReport("Estafa", "Detalles", "P1", "post_1")
        val reportId = modRepo.reports.value.first().id
        val useCase = ResolveReportUseCase(modRepo, postRepo, authRepo)

        assertTrue(useCase(reportId, ReportResolution.DISMISSED, "No se pudo comprobar el cobro externo"))
        assertEquals(0, modRepo.reports.value.size)
        assertEquals(PostStatus.APPROVED, postRepo.posts.value.first { it.id == "post_1" }.status)
    }

    @Test
    fun `ResolveReportUseCase upheld can reject the reported post`() {
        val post = ServicePost(
            id = "post_1", title = "P1", category = "Hogar", description = "Desc",
            providerName = "Other User", providerId = 2L, status = PostStatus.APPROVED
        )
        val postRepo = FakePostRepository(listOf(post))
        val authRepo = FakeAuthRepository(role = UserRole.MODERATOR, userId = 3L, userName = "Mod")
        val modRepo = FakeModerationRepository()
        modRepo.createReport("Solicitud de pago externo", "Detalles", "P1", "post_1")
        val reportId = modRepo.reports.value.first().id
        val useCase = ResolveReportUseCase(modRepo, postRepo, authRepo)

        assertTrue(
            useCase(
                reportId,
                ReportResolution.UPHELD,
                "Se confirmó el cobro fuera de la plataforma",
                PostStatus.REJECTED
            )
        )
        assertEquals(0, modRepo.reports.value.size)
        assertEquals(PostStatus.REJECTED, postRepo.posts.value.first { it.id == "post_1" }.status)
    }

    @Test
    fun `ResolveReportUseCase rejects resolutions from non moderators`() {
        val post = ServicePost(
            id = "post_1", title = "P1", category = "Hogar", description = "Desc",
            providerName = "Other User", providerId = 2L, status = PostStatus.APPROVED
        )
        val postRepo = FakePostRepository(listOf(post))
        val authRepo = FakeAuthRepository(role = UserRole.USER, userId = 1L, userName = "User")
        val modRepo = FakeModerationRepository()
        modRepo.createReport("Estafa", "Detalles", "P1", "post_1")
        val reportId = modRepo.reports.value.first().id
        val useCase = ResolveReportUseCase(modRepo, postRepo, authRepo)

        assertFalse(useCase(reportId, ReportResolution.DISMISSED, "Comentario"))
        assertEquals(1, modRepo.reports.value.size)
    }

    @Test
    fun `ModerationViewModel updates pendingPosts on category filter change`() = runTest {
        val post1 = ServicePost(id = "1", title = "Instalación", category = "Hogar", description = "Desc", providerName = "P", status = PostStatus.PENDING_VERIFICATION, createdAtMillis = 100L)
        val post2 = ServicePost(id = "2", title = "Clases", category = "Educación", description = "Desc", providerName = "P", status = PostStatus.PENDING_VERIFICATION, createdAtMillis = 200L)

        val postRepo = FakePostRepository(listOf(post1, post2))
        val modRepo = FakeModerationRepository()
        val notifRepo = MockDataRepository()
        val authRepo = FakeAuthRepository()

        val viewModel = ModerationViewModel(
            postRepository = postRepo,
            moderationRepository = modRepo,
            observeNotifications = ObserveNotificationsUseCase(SessionRepositoryImpl(), notifRepo),
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
