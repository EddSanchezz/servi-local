package com.servilocal.domain.repository

import com.servilocal.domain.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUser: StateFlow<UserProfile>
    val moderatorUser: StateFlow<UserProfile>

    suspend fun updateCurrentUser(name: String, email: String)
    suspend fun updateCurrentUserPhoto(photoUrl: String?)
    fun toggleModeratorShift()
}
