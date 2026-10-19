package com.example.domain.repository

import com.example.domain.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUser: StateFlow<UserProfile>
    val moderatorUser: StateFlow<UserProfile>

    fun registerUser(name: String, email: String)
    fun updateCurrentUser(name: String, email: String)
    fun toggleModeratorShift()
}
