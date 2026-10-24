package com.servilocal.domain.usecase.auth

import com.servilocal.core.util.Validators
import jakarta.inject.Inject

data class LoginValidation(
    val emailError: String? = null,
    val passwordError: String? = null
) {
    val isValid: Boolean get() = emailError == null && passwordError == null
}

class ValidateLoginCredentialsUseCase @Inject constructor() {
    operator fun invoke(email: String, password: String): LoginValidation = LoginValidation(
        emailError = if (Validators.isValidEmail(email)) null else "Ingresa un correo válido.",
        passwordError = if (password.length >= 6) null else "Usa al menos 6 caracteres."
    )
}
