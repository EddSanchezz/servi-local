package com.servilocal.features.moderation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.model.ReportResolution
import com.servilocal.domain.model.ServicePost
import com.servilocal.domain.model.UserRole
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.ModerationRepository
import com.servilocal.domain.repository.PostRepository
import com.servilocal.domain.usecase.moderation.ApprovePendingPostUseCase
import com.servilocal.domain.usecase.moderation.FilterPendingPostsByCategoryUseCase
import com.servilocal.domain.usecase.moderation.ObservePendingPostsUseCase
import com.servilocal.domain.usecase.moderation.RejectPendingPostUseCase
import com.servilocal.domain.usecase.moderation.ResolveReportUseCase
import com.servilocal.domain.usecase.notifications.CreateNotificationUseCase
import com.servilocal.domain.usecase.notifications.ObserveNotificationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class ModerationUiState(
    val filter: String = "FIFO",
    val selectedCategory: String? = null,
    val statsTimeRange: String = "Hoy",
    val isLoading: Boolean = false,
    val error: String? = null,
    val actionResult: String? = null
)

@HiltViewModel
class ModerationViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val moderationRepository: ModerationRepository,
    observeNotifications: ObserveNotificationsUseCase,
    private val authRepository: AuthRepository,
    private val createNotification: CreateNotificationUseCase,
    private val observePendingPostsUseCase: ObservePendingPostsUseCase = ObservePendingPostsUseCase(postRepository),
    private val filterPendingPostsByCategoryUseCase: FilterPendingPostsByCategoryUseCase = FilterPendingPostsByCategoryUseCase(),
    private val approvePendingPostUseCase: ApprovePendingPostUseCase = ApprovePendingPostUseCase(postRepository, authRepository, createNotification),
    private val rejectPendingPostUseCase: RejectPendingPostUseCase = RejectPendingPostUseCase(postRepository, authRepository, createNotification),
    private val resolveReportUseCase: ResolveReportUseCase = ResolveReportUseCase(moderationRepository, postRepository, authRepository, createNotification)
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModerationUiState())
    val uiState: StateFlow<ModerationUiState> = _uiState.asStateFlow()

    val pendingPosts: StateFlow<List<ServicePost>> = observePendingPostsUseCase()
        .combine(_uiState) { posts, state ->
            val activeFilter = state.selectedCategory ?: state.filter
            filterPendingPostsByCategoryUseCase(posts, activeFilter)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val posts = postRepository.posts
    val reports = moderationRepository.reports
    val notifications = observeNotifications()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
    val moderatorUser = authRepository.moderatorUser

    fun setFilter(value: String) {
        _uiState.update {
            it.copy(
                filter = value,
                selectedCategory = if (value == "FIFO" || value == "Todos") null else value,
                error = null
            )
        }
    }

    fun setCategoryFilter(category: String?) {
        _uiState.update {
            it.copy(
                selectedCategory = category,
                filter = category ?: "Todos",
                error = null
            )
        }
    }

    fun setStatsTimeRange(value: String) = _uiState.update { it.copy(statsTimeRange = value) }

    fun approvePost(id: String) {
        val success = approvePendingPostUseCase(id)
        if (success) {
            _uiState.update { it.copy(actionResult = "Publicación aprobada exitosamente", error = null) }
        } else {
            val userRole = authRepository.moderatorUser.value.role
            if (userRole != UserRole.MODERATOR) {
                _uiState.update { it.copy(error = "Usuario no autorizado para moderar") }
            } else {
                _uiState.update { it.copy(error = "No se pudo aprobar la publicación (estado inválido)") }
            }
        }
    }

    fun rejectPost(id: String, reason: String, notes: String? = null) {
        val success = rejectPendingPostUseCase(id, reason, notes)
        if (success) {
            _uiState.update { it.copy(actionResult = "Publicación rechazada", error = null) }
        } else {
            val userRole = authRepository.moderatorUser.value.role
            if (userRole != UserRole.MODERATOR) {
                _uiState.update { it.copy(error = "Usuario no autorizado para moderar") }
            } else {
                _uiState.update { it.copy(error = "Motivo obligatorio o estado inválido") }
            }
        }
    }

    fun resolveReport(
        reportId: String,
        resolution: ReportResolution,
        moderatorComment: String,
        newPostStatus: PostStatus? = null
    ): Boolean {
        val success = resolveReportUseCase(reportId, resolution, moderatorComment, newPostStatus)
        _uiState.update {
            it.copy(
                actionResult = if (success) "Reporte resuelto" else null,
                error = if (success) null else "No se pudo resolver el reporte. Revisa el comentario y tu rol."
            )
        }
        return success
    }

    fun resolveReportsForPost(id: String) = moderationRepository.resolveReportsForPost(id)
    fun toggleModeratorShift() = authRepository.toggleModeratorShift()
}
