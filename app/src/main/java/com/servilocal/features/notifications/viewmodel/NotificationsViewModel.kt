package com.servilocal.features.notifications.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servilocal.core.util.relativeTime
import com.servilocal.domain.model.AppNotification
import com.servilocal.domain.model.NotificationItem
import com.servilocal.domain.usecase.notifications.MarkNotificationReadUseCase
import com.servilocal.domain.usecase.notifications.ObserveNotificationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsUiState(val filter: String = "all")

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    observeNotifications: ObserveNotificationsUseCase,
    private val markNotificationRead: MarkNotificationReadUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    val notifications: StateFlow<List<NotificationItem>> = observeNotifications()
        .map { notifications -> notifications.map { it.toNotificationItem() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun setFilter(value: String) = _uiState.update { it.copy(filter = value) }

    fun markAllRead() {
        viewModelScope.launch { markNotificationRead() }
    }

    fun toggleRead(id: String) {
        val current = notifications.value.firstOrNull { it.id == id } ?: return
        viewModelScope.launch { markNotificationRead(id, isRead = !current.read) }
    }
}

private fun AppNotification.toNotificationItem(): NotificationItem = NotificationItem(
    id = id,
    title = title,
    description = body,
    time = relativeTime(createdAtMillis),
    type = type,
    read = isRead,
    relatedPostId = relatedPostId,
    relatedChatId = relatedChatId
)
