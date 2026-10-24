package com.servilocal.domain.usecase.auth

import com.servilocal.domain.model.UserSession
import com.servilocal.domain.repository.SessionRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/**
 * Expone la sesión activa como un flujo observable para que la app
 * reaccione a inicios y cierres de sesión sin consultar la UI.
 */
class ObserveSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    operator fun invoke(): StateFlow<UserSession?> = sessionRepository.session
}
