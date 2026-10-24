package com.servilocal.domain.usecase.auth

import com.servilocal.domain.repository.SessionRepository
import jakarta.inject.Inject

/**
 * Elimina la sesión activa. En la fase 2 la sesión vive en memoria,
 * por lo que el cierre es inmediato y no requiere persistencia.
 */
class LogoutUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke() {
        sessionRepository.clearSession()
    }
}
