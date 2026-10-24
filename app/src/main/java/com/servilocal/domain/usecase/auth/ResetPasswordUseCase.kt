package com.servilocal.domain.usecase.auth

import com.servilocal.core.security.PasswordHasher
import com.servilocal.core.util.Validators
import com.servilocal.domain.repository.PasswordResetRepository
import com.servilocal.domain.repository.UserRepository
import jakarta.inject.Inject

sealed interface ResetPasswordResult {
    data object Success : ResetPasswordResult
    data object InvalidEmail : ResetPasswordResult
    data object UserNotFound : ResetPasswordResult
    data object InvalidCode : ResetPasswordResult
    data object WeakPassword : ResetPasswordResult
}

/**
 * Verifica el código de recuperación y reemplaza la contraseña del usuario.
 * El código es de un solo uso: se elimina cuando el cambio se completa.
 */
class ResetPasswordUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val passwordResetRepository: PasswordResetRepository
) {
    suspend operator fun invoke(email: String, code: String, newPassword: String): ResetPasswordResult {
        val normalizedEmail = email.trim().lowercase()
        if (!Validators.isValidEmail(normalizedEmail)) return ResetPasswordResult.InvalidEmail

        val user = userRepository.findByEmail(normalizedEmail) ?: return ResetPasswordResult.UserNotFound
        val storedCode = passwordResetRepository.findCode(normalizedEmail)
        if (storedCode == null || storedCode != code.trim()) return ResetPasswordResult.InvalidCode
        if (newPassword.length < MIN_PASSWORD_LENGTH) return ResetPasswordResult.WeakPassword

        userRepository.updatePassword(user.id, PasswordHasher.hash(newPassword))
        passwordResetRepository.clearCode(normalizedEmail)
        return ResetPasswordResult.Success
    }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 8
    }
}
