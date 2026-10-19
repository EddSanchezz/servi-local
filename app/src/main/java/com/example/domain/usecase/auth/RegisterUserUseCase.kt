package com.example.domain.usecase.auth

import com.example.domain.repository.AuthRepository
import jakarta.inject.Inject

class RegisterUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(name: String, email: String) {
        authRepository.registerUser(name, email)
    }
}
