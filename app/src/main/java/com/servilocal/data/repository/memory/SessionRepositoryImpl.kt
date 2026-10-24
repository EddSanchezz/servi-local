package com.servilocal.data.repository.memory

import com.servilocal.domain.model.UserRole
import com.servilocal.domain.model.UserSession
import com.servilocal.domain.repository.SessionRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class SessionRepositoryImpl @Inject constructor() : SessionRepository {

    private val _session = MutableStateFlow<UserSession?>(null)
    override val session: StateFlow<UserSession?> = _session.asStateFlow()

    override suspend fun saveSession(userId: String, role: UserRole) {
        _session.value = UserSession(userId = userId, role = role)
    }

    override suspend fun clearSession() {
        _session.value = null
    }
}
