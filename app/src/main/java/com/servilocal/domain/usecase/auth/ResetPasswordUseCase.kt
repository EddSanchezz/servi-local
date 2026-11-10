package com.servilocal.domain.usecase.auth

import com.servilocal.core.util.Validators
import com.servilocal.core.security.PasswordHasher
import com.google.firebase.auth.FirebaseAuthException
import com.servilocal.domain.repository.PasswordResetRepository
import com.servilocal.domain.repository.UserRepository
import jakarta.inject.Inject

sealed interface ResetPasswordResult {
    data object Success : ResetPasswordResult
    data object InvalidEmail : ResetPasswordResult
    data object UserNotFound : ResetPasswordResult
    data object InvalidCode : ResetPasswordResult
    data object WeakPassword : ResetPasswordResult
    data object Failed : ResetPasswordResult
}

class ResetPasswordUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    @Suppress("UNUSED_PARAMETER")
    private var legacyResetRepository: PasswordResetRepository? = null

    constructor(
        userRepository: UserRepository,
        legacyResetRepository: PasswordResetRepository
    ) : this(userRepository) {
        this.legacyResetRepository = legacyResetRepository
    }

    suspend operator fun invoke(email: String, code: String, newPassword: String): ResetPasswordResult {
        val normalizedEmail = email.trim().lowercase()
        if (legacyResetRepository != null && !Validators.isValidEmail(normalizedEmail)) {
            return ResetPasswordResult.InvalidEmail
        }
        if (newPassword.length < 8) return ResetPasswordResult.WeakPassword
        legacyResetRepository?.let { legacy ->
            val user = userRepository.findByEmail(normalizedEmail)
                ?: return ResetPasswordResult.UserNotFound
            if (legacy.findCode(normalizedEmail) != code) {
                return ResetPasswordResult.InvalidCode
            }
            userRepository.save(user.copy(passwordHash = PasswordHasher.hash(newPassword)))
            legacy.clearCode(normalizedEmail)
            return ResetPasswordResult.Success
        }
        if (code.isBlank()) return ResetPasswordResult.InvalidCode
        return try {
            userRepository.confirmPasswordResetCode(code, newPassword)
            ResetPasswordResult.Success
        } catch (error: FirebaseAuthException) {
            if (error.errorCode == "ERROR_INVALID_ACTION_CODE" ||
                error.errorCode == "ERROR_EXPIRED_ACTION_CODE"
            ) {
                ResetPasswordResult.InvalidCode
            } else {
                ResetPasswordResult.Failed
            }
        } catch (_: Exception) {
            ResetPasswordResult.Failed
        }
    }
}
