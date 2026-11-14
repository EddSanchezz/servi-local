package com.servilocal.domain.repository

import com.servilocal.domain.model.UserProfile
import com.servilocal.domain.model.User
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface AuthRepository {
    val currentUser: StateFlow<UserProfile>
    val moderatorUser: StateFlow<UserProfile>

    /** Emite un aviso de bloqueo para que la UI pueda informarlo incluso tras un sign-out automático. */
    val blockedAccountNotice: Flow<String?> get() = emptyFlow()
    suspend fun clearBlockedAccountNotice() {}

    suspend fun updateCurrentUser(name: String, email: String)
    suspend fun updateCurrentUserPhoto(photoUrl: String?)
    suspend fun toggleModeratorShift()
    suspend fun syncCurrentUser(user: User) {}
}
