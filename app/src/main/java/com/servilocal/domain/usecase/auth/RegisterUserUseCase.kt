package com.servilocal.domain.usecase.auth

import com.servilocal.core.security.PasswordHasher
import com.servilocal.core.util.Validators
import com.servilocal.domain.model.User
import com.servilocal.domain.model.UserRole
import com.servilocal.domain.repository.UserRepository
import jakarta.inject.Inject
import java.util.UUID

sealed interface RegisterResult {
    data class Success(val user: User) : RegisterResult
    data object EmailAlreadyExists : RegisterResult
    data object InvalidName : RegisterResult
    data object InvalidEmail : RegisterResult
    data object WeakPassword : RegisterResult
}

class RegisterUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(name: String, email: String, password: String): RegisterResult {
        val normalizedName = name.trim()
        val normalizedEmail = email.trim().lowercase()

        if (normalizedName.isBlank()) return RegisterResult.InvalidName
        if (!Validators.isValidEmail(normalizedEmail)) return RegisterResult.InvalidEmail
        if (password.length < MIN_PASSWORD_LENGTH) return RegisterResult.WeakPassword
        if (userRepository.findByEmail(normalizedEmail) != null) return RegisterResult.EmailAlreadyExists

        val user = User(
            id = UUID.randomUUID().toString(),
            name = normalizedName,
            email = normalizedEmail,
            passwordHash = PasswordHasher.hash(password),
            role = UserRole.USER
        )
        userRepository.save(user)
        return RegisterResult.Success(user)
    }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 8
    }
}
