package com.servilocal.domain.repository

import com.servilocal.domain.model.UserRole
import com.servilocal.domain.model.UserSession
import kotlinx.coroutines.flow.StateFlow

interface SessionRepository {
    val session: StateFlow<UserSession?>

    suspend fun saveSession(userId: Long, role: UserRole)
    suspend fun clearSession()
}
