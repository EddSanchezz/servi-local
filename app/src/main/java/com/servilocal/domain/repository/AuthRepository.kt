package com.servilocal.domain.repository

import com.servilocal.domain.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUser: StateFlow<UserProfile>
    val moderatorUser: StateFlow<UserProfile>

    fun updateCurrentUser(name: String, email: String)
    fun toggleModeratorShift()
}
