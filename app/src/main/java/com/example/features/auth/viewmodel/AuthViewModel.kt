package com.example.features.auth.viewmodel

import androidx.lifecycle.ViewModel
import com.example.core.data.repository.MockDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AuthUiState(
    val emailError: String? = null,
    val passwordError: String? = null,
    val nameError: String? = null
)

/** Validation state for the local sign-in and registration forms. */
class AuthViewModel(private val repository: MockDataRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String): Boolean {
        val emailError = if (EMAIL_PATTERN.matches(email.trim())) null else "Ingresa un correo válido."
        val passwordError = if (password.length >= 6) null else "Usa al menos 6 caracteres."
        _uiState.value = AuthUiState(emailError = emailError, passwordError = passwordError)
        return emailError == null && passwordError == null
    }

    fun register(name: String, email: String): Boolean {
        val nameError = if (name.isBlank()) "Escribe tu nombre completo." else null
        val emailError = if (EMAIL_PATTERN.matches(email.trim())) null else "Ingresa un correo válido."
        _uiState.value = AuthUiState(nameError = nameError, emailError = emailError)
        if (nameError != null || emailError != null) return false
        repository.registerUser(name, email.trim())
        return true
    }

    fun clearErrors() { _uiState.value = AuthUiState() }

    private companion object {
        val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
