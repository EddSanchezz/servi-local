package com.example.features.notifications.viewmodel

import androidx.lifecycle.ViewModel
import com.example.core.data.repository.MockDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class NotificationsUiState(val filter: String = "all")

class NotificationsViewModel(private val repository: MockDataRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()
    val notifications = repository.notifications
    fun setFilter(value: String) = _uiState.update { it.copy(filter = value) }
    fun markAllRead() = repository.markAllNotificationsRead()
    fun toggleRead(id: String) = repository.toggleNotificationRead(id)
}
