package com.servilocal.domain.repository

interface PasswordResetRepository {
    suspend fun saveCode(email: String, code: String)
    suspend fun findCode(email: String): String?
    suspend fun clearCode(email: String)
}
