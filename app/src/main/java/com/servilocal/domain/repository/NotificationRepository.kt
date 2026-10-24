package com.servilocal.domain.repository

import com.servilocal.domain.model.AppNotification
import kotlinx.coroutines.flow.StateFlow

interface NotificationRepository {
    val notifications: StateFlow<List<AppNotification>>

    suspend fun create(notification: AppNotification)

    /** Marca una notificación solo si pertenece al usuario indicado. */
    suspend fun markRead(notificationId: String, userId: String, isRead: Boolean): Boolean

    suspend fun markAllRead(userId: String)
}
