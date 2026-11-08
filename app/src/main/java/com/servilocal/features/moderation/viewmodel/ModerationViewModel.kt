package com.servilocal.features.moderation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servilocal.R
import com.servilocal.core.util.ResourceProvider
import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.model.ReportResolution
import com.servilocal.domain.model.ServicePost
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
import com.servilocal.domain.usecase.notifications.ObserveNotificationsUseCase
import com.servilocal.domain.usecase.posts.MarkPostAsResolvedUseCase
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
    observeNotifications: ObserveNotificationsUseCase,
    private val observePendingPostsUseCase: ObservePendingPostsUseCase,
    private val filterPendingPostsByCategoryUseCase: FilterPendingPostsByCategoryUseCase,
    private val approvePostUseCase: ApprovePostUseCase,
    private val rejectPostUseCase: RejectPostUseCase,
    private val markPostAsResolvedUseCase: MarkPostAsResolvedUseCase,
    private val resolveReportUseCase: ResolveReportUseCase,
    observeModerationPosts: ObserveModerationPostsUseCase,
    observeReports: ObserveReportsUseCase,
    observeModeratorProfile: ObserveModeratorProfileUseCase,
    private val resolveReportsForPostUseCase: ResolveReportsForPostUseCase,
    private val toggleModeratorShiftUseCase: ToggleModeratorShiftUseCase,
    private val resources: ResourceProvider
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

    val posts = observeModerationPosts()
    val reports = observeReports()
    val notifications = observeNotifications()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
    val moderatorUser = observeModeratorProfile()

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

    fun approvePost(id: String): Boolean {
        val success = approvePostUseCase(id)
        if (success) {
            _uiState.update { it.copy(actionResult = resources.getString(R.string.post_approved_success), error = null) }
        } else {
            _uiState.update { it.copy(error = resources.getString(R.string.post_approve_failed)) }
        }
        return success
    }

    fun rejectPost(id: String, reason: String, notes: String? = null): Boolean {
        val success = rejectPostUseCase(id, reason, notes)
        if (success) {
            _uiState.update { it.copy(actionResult = resources.getString(R.string.post_rejected_success), error = null) }
        } else {
            _uiState.update { it.copy(error = resources.getString(R.string.post_reject_failed)) }
        }
        return success
    }

    fun markPostResolved(id: String): Boolean {
        val success = markPostAsResolvedUseCase(id)
        _uiState.update {
            it.copy(
                actionResult = if (success) resources.getString(R.string.post_resolved_success) else null,
                error = if (success) null else resources.getString(R.string.post_resolve_failed)
            )
        }
        return success
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
                actionResult = if (success) resources.getString(R.string.report_resolved) else null,
                error = if (success) null else resources.getString(R.string.report_resolve_failed)
            )
        }
        return success
    }

    fun resolveReportsForPost(id: String): Boolean = resolveReportsForPostUseCase(id)
    fun toggleModeratorShift(): Boolean = toggleModeratorShiftUseCase()

    fun clearFeedback() {
        _uiState.update { it.copy(error = null, actionResult = null) }
    }
}
