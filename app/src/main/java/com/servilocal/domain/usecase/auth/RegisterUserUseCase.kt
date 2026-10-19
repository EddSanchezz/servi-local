package com.servilocal.domain.usecase.auth

import com.servilocal.domain.repository.AuthRepository
import jakarta.inject.Inject

class RegisterUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(name: String, email: String) {
        authRepository.registerUser(name, email)
    }
}
