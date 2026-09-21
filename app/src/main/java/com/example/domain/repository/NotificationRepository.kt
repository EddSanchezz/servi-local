package com.example.domain.repository

import com.example.data.model.NotificationItem
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    val notifications: Flow<List<NotificationItem>>
    suspend fun markAllRead()
    suspend fun toggleRead(id: String)
}