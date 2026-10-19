package com.servilocal.domain.usecase.auth

import jakarta.inject.Inject

data class LoginValidation(
    val emailError: String? = null,
    val passwordError: String? = null
) {
    val isValid: Boolean get() = emailError == null && passwordError == null
}

class ValidateLoginCredentialsUseCase @Inject constructor() {
    operator fun invoke(email: String, password: String): LoginValidation = LoginValidation(
        emailError = if (EMAIL_PATTERN.matches(email.trim())) null else "Ingresa un correo válido.",
        passwordError = if (password.length >= 6) null else "Usa al menos 6 caracteres."
    )

    private companion object {
        val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
