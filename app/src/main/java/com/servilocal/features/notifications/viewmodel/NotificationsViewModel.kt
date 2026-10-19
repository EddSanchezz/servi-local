package com.servilocal.features.notifications.viewmodel

import androidx.lifecycle.ViewModel
import com.servilocal.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class NotificationsUiState(val filter: String = "all")

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()
    val notifications = repository.notifications
    fun setFilter(value: String) = _uiState.update { it.copy(filter = value) }
    fun markAllRead() = repository.markAllNotificationsRead()
    fun toggleRead(id: String) = repository.toggleNotificationRead(id)
}
