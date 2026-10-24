package com.servilocal.features.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servilocal.core.util.RequestResult
import com.servilocal.domain.model.UserRole
import com.servilocal.domain.usecase.auth.LoginUserUseCase
import com.servilocal.domain.usecase.auth.RecoverPasswordResult
import com.servilocal.domain.usecase.auth.RecoverPasswordUseCase
import com.servilocal.domain.usecase.auth.RegisterResult
import com.servilocal.domain.usecase.auth.RegisterUserUseCase
import com.servilocal.domain.usecase.auth.ValidateLoginCredentialsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val emailError: String? = null,
    val passwordError: String? = null,
    val nameError: String? = null,
    val loginResult: RequestResult? = null,
    val authenticatedRole: UserRole? = null,
    val registerResult: RequestResult? = null,
    val registeredEmail: String? = null,
    val recoveryResult: RequestResult? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val validateLogin: ValidateLoginCredentialsUseCase,
    private val loginUser: LoginUserUseCase,
    private val registerUser: RegisterUserUseCase,
    private val recoverPasswordUseCase: RecoverPasswordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        if (_uiState.value.loginResult is RequestResult.Loading) return

        val validation = validateLogin(email, password)
        _uiState.update {
            it.copy(
                emailError = validation.emailError,
                passwordError = validation.passwordError,
                loginResult = null,
                authenticatedRole = null
            )
        }
        if (!validation.isValid) return

        viewModelScope.launch {
            _uiState.update { it.copy(loginResult = RequestResult.Loading) }
            val user = loginUser(email, password)
            _uiState.update { state ->
                if (user != null) {
                    state.copy(
                        loginResult = RequestResult.Success("Inicio de sesión correcto."),
                        authenticatedRole = user.role
                    )
                } else {
                    state.copy(loginResult = RequestResult.Failure("Correo o contraseña incorrectos."))
                }
            }
        }
    }

    fun register(name: String, email: String, password: String) {
        if (_uiState.value.registerResult is RequestResult.Loading) return

        _uiState.update {
            it.copy(
                nameError = null,
                emailError = null,
                passwordError = null,
                registerResult = null,
                registeredEmail = null
            )
        }

        viewModelScope.launch {
            _uiState.update { it.copy(registerResult = RequestResult.Loading) }
            when (val result = registerUser(name, email, password)) {
                is RegisterResult.Success -> _uiState.update {
                    it.copy(
                        registerResult = RequestResult.Success("Cuenta creada correctamente."),
                        registeredEmail = result.user.email
                    )
                }

                RegisterResult.EmailAlreadyExists -> _uiState.update {
                    it.copy(
                        emailError = "Ese correo ya está registrado.",
                        registerResult = RequestResult.Failure("Ese correo ya está registrado.")
                    )
                }

                RegisterResult.InvalidName -> _uiState.update {
                    it.copy(
                        nameError = "Escribe tu nombre completo.",
                        registerResult = RequestResult.Failure("Revisa tu nombre.")
                    )
                }

                RegisterResult.InvalidEmail -> _uiState.update {
                    it.copy(
                        emailError = "Ingresa un correo válido.",
                        registerResult = RequestResult.Failure("Revisa el correo.")
                    )
                }

                RegisterResult.WeakPassword -> _uiState.update {
                    it.copy(
                        passwordError = "Usa al menos 8 caracteres.",
                        registerResult = RequestResult.Failure("Revisa la contraseña.")
                    )
                }
            }
        }
    }

    fun recoverPassword(email: String) {
        if (_uiState.value.recoveryResult is RequestResult.Loading) return

        _uiState.update { it.copy(recoveryResult = RequestResult.Loading) }
        viewModelScope.launch {
            val result = recoverPasswordUseCase(email)
            _uiState.update { state ->
                when (result) {
                    RecoverPasswordResult.Success -> state.copy(
                        recoveryResult = RequestResult.Success(
                            "Solicitud registrada. Revisa tu correo para continuar."
                        )
                    )

                    RecoverPasswordResult.InvalidEmail -> state.copy(
                        recoveryResult = RequestResult.Failure("Ingresa un correo válido para continuar.")
                    )

                    RecoverPasswordResult.UserNotFound -> state.copy(
                        recoveryResult = RequestResult.Failure("No encontramos una cuenta con ese correo.")
                    )
                }
            }
        }
    }

    fun clearRecoveryFeedback() {
        _uiState.update { it.copy(recoveryResult = null) }
    }

    fun clearLoginFeedback() {
        _uiState.update {
            it.copy(
                emailError = null,
                passwordError = null,
                loginResult = null,
                authenticatedRole = null
            )
        }
    }

    fun clearRegisterFeedback() {
        _uiState.update {
            it.copy(
                nameError = null,
                emailError = null,
                passwordError = null,
                registerResult = null,
                registeredEmail = null
            )
        }
    }
}
