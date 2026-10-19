package com.example.core.domain.model

import com.example.core.data.model.UserRole

data class AuthSession(
    val userId: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val isAuthenticated: Boolean = true
)
