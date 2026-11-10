package com.servilocal.domain.usecase.auth

import com.servilocal.core.util.Validators
import com.servilocal.domain.repository.PasswordResetRepository
import com.servilocal.domain.repository.UserRepository
import jakarta.inject.Inject

sealed interface RecoverPasswordResult {
    data class Success(val code: String = "") : RecoverPasswordResult
    data object InvalidEmail : RecoverPasswordResult
    @Deprecated("Firebase intentionally does not reveal whether an account exists")
    data object UserNotFound : RecoverPasswordResult
}

class RecoverPasswordUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    private var legacyResetRepository: PasswordResetRepository? = null

    @Suppress("UNUSED_PARAMETER")
    constructor(
        userRepository: UserRepository,
        legacyResetRepository: PasswordResetRepository
    ) : this(userRepository) {
        this.legacyResetRepository = legacyResetRepository
    }

    suspend operator fun invoke(email: String): RecoverPasswordResult {
        val normalizedEmail = email.trim().lowercase()
        if (!Validators.isValidEmail(normalizedEmail)) return RecoverPasswordResult.InvalidEmail
        legacyResetRepository?.let { legacy ->
            if (userRepository.findByEmail(normalizedEmail) == null) {
                return RecoverPasswordResult.UserNotFound
            }
            val code = (10000..99999).random().toString()
            legacy.saveCode(normalizedEmail, code)
            return RecoverPasswordResult.Success(code)
        }
        // Firebase deliberately returns the same UX for known and unknown accounts.
        userRepository.sendPasswordResetEmail(normalizedEmail)
        return RecoverPasswordResult.Success()
    }
}
