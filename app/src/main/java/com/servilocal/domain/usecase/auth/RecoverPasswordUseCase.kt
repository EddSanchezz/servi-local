package com.servilocal.domain.usecase.auth

import com.servilocal.core.util.Validators
import com.servilocal.domain.repository.UserRepository
import jakarta.inject.Inject

sealed interface RecoverPasswordResult {
    data object Success : RecoverPasswordResult
    data object InvalidEmail : RecoverPasswordResult
    data object UserNotFound : RecoverPasswordResult
}

/**
 * Valida la solicitud de recuperación. En la fase 2 no hay servicio de correo:
 * solo se comprueba que el correo tenga formato válido y pertenezca a una cuenta.
 */
class RecoverPasswordUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(email: String): RecoverPasswordResult {
        val normalizedEmail = email.trim().lowercase()
        if (!Validators.isValidEmail(normalizedEmail)) return RecoverPasswordResult.InvalidEmail
        if (userRepository.findByEmail(normalizedEmail) == null) return RecoverPasswordResult.UserNotFound
        return RecoverPasswordResult.Success
    }
}
