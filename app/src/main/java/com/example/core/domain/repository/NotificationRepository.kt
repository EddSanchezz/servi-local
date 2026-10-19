package com.example.core.domain.repository

import com.example.core.data.model.NotificationItem
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    val notifications: Flow<List<NotificationItem>>
    suspend fun markAllRead()
    suspend fun toggleRead(id: String)
}