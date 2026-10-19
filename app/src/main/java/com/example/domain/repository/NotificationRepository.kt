package com.example.domain.repository

import com.example.domain.model.NotificationItem
import kotlinx.coroutines.flow.StateFlow

interface NotificationRepository {
    val notifications: StateFlow<List<NotificationItem>>

    fun markAllNotificationsRead()
    fun toggleNotificationRead(notificationId: String)
}
