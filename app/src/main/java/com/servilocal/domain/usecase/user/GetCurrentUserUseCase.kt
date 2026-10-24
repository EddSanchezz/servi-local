package com.servilocal.domain.usecase.user

import com.servilocal.domain.model.User
import com.servilocal.domain.repository.SessionRepository
import com.servilocal.domain.repository.UserRepository
import jakarta.inject.Inject

/**
 * Devuelve el usuario dueño de la sesión activa. Si no hay sesión, o el usuario
 * ya no existe en el repositorio, retorna null.
 *
 * Es el punto único de consulta del usuario autenticado: los demás casos de uso
 * de perfil no deben leer la sesión ni el repositorio de usuarios por su cuenta.
 */
class GetCurrentUserUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): User? {
        val session = sessionRepository.session.value ?: return null
        return userRepository.findById(session.userId)
    }
}
