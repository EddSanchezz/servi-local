package com.example.features.moderation.viewmodel

import androidx.lifecycle.ViewModel
import com.example.core.data.repository.MockDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ModerationUiState(
    val filter: String = "FIFO",
    val statsTimeRange: String = "Hoy"
)

class ModerationViewModel(private val repository: MockDataRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ModerationUiState())
    val uiState: StateFlow<ModerationUiState> = _uiState.asStateFlow()
    val posts = repository.posts
    val reports = repository.reports
    val notifications = repository.notifications
    val moderatorUser = repository.moderatorUser
    fun setFilter(value: String) = _uiState.update { it.copy(filter = value) }
    fun setStatsTimeRange(value: String) = _uiState.update { it.copy(statsTimeRange = value) }
    fun approvePost(id: String) = repository.approvePost(id)
    fun rejectPost(id: String, reason: String, notes: String?) = repository.rejectPost(id, reason, notes)
    fun dismissReport(id: String) = repository.dismissReport(id)
    fun resolveReportsForPost(id: String) = repository.resolveReportsForPost(id)
    fun toggleModeratorShift() = repository.toggleModeratorShift()
}
