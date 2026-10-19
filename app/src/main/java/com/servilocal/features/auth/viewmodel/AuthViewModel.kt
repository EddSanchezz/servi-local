package com.servilocal.features.auth.viewmodel

import androidx.lifecycle.ViewModel
import com.servilocal.domain.usecase.auth.RegisterUserUseCase
import com.servilocal.domain.usecase.auth.ValidateLoginCredentialsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AuthUiState(
    val emailError: String? = null,
    val passwordError: String? = null,
    val nameError: String? = null
)

/** Validation state for the local sign-in and registration forms. */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val validateLogin: ValidateLoginCredentialsUseCase,
    private val registerUser: RegisterUserUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String): Boolean {
        val validation = validateLogin(email, password)
        _uiState.value = AuthUiState(emailError = validation.emailError, passwordError = validation.passwordError)
        return validation.isValid
    }

    fun register(name: String, email: String): Boolean {
        val nameError = if (name.isBlank()) "Escribe tu nombre completo." else null
        val emailError = if (EMAIL_PATTERN.matches(email.trim())) null else "Ingresa un correo válido."
        _uiState.value = AuthUiState(nameError = nameError, emailError = emailError)
        if (nameError != null || emailError != null) return false
        registerUser(name, email.trim())
        return true
    }

    fun clearErrors() { _uiState.value = AuthUiState() }

    private companion object { val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") }
}
