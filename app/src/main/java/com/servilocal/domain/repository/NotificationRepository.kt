package com.servilocal.domain.repository

import com.servilocal.domain.model.NotificationItem
import kotlinx.coroutines.flow.StateFlow

interface NotificationRepository {
    val notifications: StateFlow<List<NotificationItem>>

    fun markAllNotificationsRead()
    fun toggleNotificationRead(notificationId: String)
}
