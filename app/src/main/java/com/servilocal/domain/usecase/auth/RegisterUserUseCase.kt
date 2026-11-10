package com.servilocal.domain.usecase.auth

import com.servilocal.core.util.Validators
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.servilocal.domain.model.User
import com.servilocal.domain.model.UserRole
import com.servilocal.domain.repository.UserRepository
import jakarta.inject.Inject

sealed interface RegisterResult {
    data class Success(val user: User) : RegisterResult
    data object EmailAlreadyExists : RegisterResult
    data object InvalidName : RegisterResult
    data object InvalidEmail : RegisterResult
    data object WeakPassword : RegisterResult
    data object Failed : RegisterResult
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

        val savedUser = try {
            userRepository.createAccount(normalizedName, normalizedEmail, password)
        } catch (error: FirebaseAuthException) {
            if (error.errorCode == "ERROR_EMAIL_ALREADY_IN_USE") {
                return RegisterResult.EmailAlreadyExists
            }
            return RegisterResult.Failed
        } catch (_: Exception) {
            return RegisterResult.Failed
        } ?: return RegisterResult.Failed
        return RegisterResult.Success(savedUser)
    }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 8
    }
}
