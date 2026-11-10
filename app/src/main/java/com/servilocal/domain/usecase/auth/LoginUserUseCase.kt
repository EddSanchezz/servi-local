package com.servilocal.domain.usecase.auth

import com.servilocal.domain.model.User
import com.servilocal.domain.repository.SessionRepository
import com.servilocal.domain.repository.UserRepository
import com.servilocal.domain.repository.AuthRepository
import jakarta.inject.Inject

class LoginUserUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository? = null
) {
    suspend operator fun invoke(email: String, password: String): User? {
        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail.isBlank() || password.isBlank()) return null

        val user = userRepository.login(normalizedEmail, password) ?: return null
        sessionRepository.saveSession(user.id, user.role)
        authRepository?.syncCurrentUser(user)
        return user
    }
}
