package com.servilocal.domain.usecase.auth

import com.servilocal.core.util.Validators
import com.servilocal.domain.repository.PasswordResetRepository
import com.servilocal.domain.repository.UserRepository
import jakarta.inject.Inject
import java.security.SecureRandom

sealed interface RecoverPasswordResult {
    data class Success(val code: String) : RecoverPasswordResult
    data object InvalidEmail : RecoverPasswordResult
    data object UserNotFound : RecoverPasswordResult
}

/**
 * Valida la solicitud de recuperación y genera un código de verificación de 5 dígitos.
 * En la fase 2 no hay servicio de correo, así que el código se conserva en memoria y
 * se muestra en la UI como dato de demostración.
 */
class RecoverPasswordUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val passwordResetRepository: PasswordResetRepository
) {
    suspend operator fun invoke(email: String): RecoverPasswordResult {
        val normalizedEmail = email.trim().lowercase()
        if (!Validators.isValidEmail(normalizedEmail)) return RecoverPasswordResult.InvalidEmail
        if (userRepository.findByEmail(normalizedEmail) == null) return RecoverPasswordResult.UserNotFound

        val code = generateCode()
        passwordResetRepository.saveCode(normalizedEmail, code)
        return RecoverPasswordResult.Success(code)
    }

    private fun generateCode(): String =
        SecureRandom().nextInt(CODE_UPPER_BOUND).toString().padStart(CODE_LENGTH, '0')

    private companion object {
        const val CODE_LENGTH = 5
        const val CODE_UPPER_BOUND = 100_000
    }
}
