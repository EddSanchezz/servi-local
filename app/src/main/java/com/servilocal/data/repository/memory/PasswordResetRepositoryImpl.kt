package com.servilocal.data.repository.memory

import com.servilocal.domain.repository.PasswordResetRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

@Singleton
class PasswordResetRepositoryImpl @Inject constructor() : PasswordResetRepository {

    private val codes = MutableStateFlow<Map<String, String>>(emptyMap())

    override suspend fun saveCode(email: String, code: String) {
        codes.update { current -> current + (email.normalized() to code) }
    }

    override suspend fun findCode(email: String): String? = codes.value[email.normalized()]

    override suspend fun clearCode(email: String) {
        codes.update { current -> current - email.normalized() }
    }

    private fun String.normalized(): String = trim().lowercase()
}
