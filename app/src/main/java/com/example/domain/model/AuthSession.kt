package com.example.domain.model

data class AuthSession(
    val userId: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val isAuthenticated: Boolean = true
)