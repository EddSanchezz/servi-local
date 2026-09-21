package com.example.domain.repository

import com.example.domain.model.AuthSession
import com.example.domain.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val user: StateFlow<AuthSession?>
    val isLoggedIn: Flow<Boolean>

    suspend fun login(email: String, password: String): AuthSession
    suspend fun login(role: UserRole): AuthSession
    suspend fun register(email: String, password: String, role: UserRole): AuthSession
    suspend fun logout()
    suspend fun clearSession()
}