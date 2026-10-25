package com.servilocal.domain.usecase.user

import com.servilocal.domain.model.User
import com.servilocal.domain.repository.SessionRepository
import com.servilocal.domain.repository.UserRepository
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Observa al usuario dueño de la sesión activa y reacciona a sus cambios
 * (por ejemplo, puntos y nivel). Si no hay sesión, emite null.
 */
class ObserveCurrentUserUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<User?> = sessionRepository.session.flatMapLatest { session ->
        if (session == null) {
            flowOf(null)
        } else {
            userRepository.users.map { users -> users.firstOrNull { it.id == session.userId } }
        }
    }
}
