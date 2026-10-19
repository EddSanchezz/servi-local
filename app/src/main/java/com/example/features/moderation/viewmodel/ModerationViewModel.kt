package com.example.features.moderation.viewmodel

import androidx.lifecycle.ViewModel
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.ModerationRepository
import com.example.domain.repository.NotificationRepository
import com.example.domain.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ModerationUiState(
    val filter: String = "FIFO",
    val statsTimeRange: String = "Hoy"
)

@HiltViewModel
class ModerationViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val moderationRepository: ModerationRepository,
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ModerationUiState())
    val uiState: StateFlow<ModerationUiState> = _uiState.asStateFlow()
    val posts = postRepository.posts
    val reports = moderationRepository.reports
    val notifications = notificationRepository.notifications
    val moderatorUser = authRepository.moderatorUser
    fun setFilter(value: String) = _uiState.update { it.copy(filter = value) }
    fun setStatsTimeRange(value: String) = _uiState.update { it.copy(statsTimeRange = value) }
    fun approvePost(id: String) = postRepository.approvePost(id)
    fun rejectPost(id: String, reason: String, notes: String?) = postRepository.rejectPost(id, reason, notes)
    fun dismissReport(id: String) = moderationRepository.dismissReport(id)
    fun resolveReportsForPost(id: String) = moderationRepository.resolveReportsForPost(id)
    fun toggleModeratorShift() = authRepository.toggleModeratorShift()
}
