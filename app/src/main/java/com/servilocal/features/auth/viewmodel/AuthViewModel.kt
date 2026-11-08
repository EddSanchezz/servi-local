package com.servilocal.features.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servilocal.core.util.RequestResult
import com.servilocal.core.util.ResourceProvider
import com.servilocal.R
import com.servilocal.domain.model.UserRole
import com.servilocal.domain.usecase.auth.LoginUserUseCase
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
    val registeredEmail: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val validateLogin: ValidateLoginCredentialsUseCase,
    private val loginUser: LoginUserUseCase,
    private val registerUser: RegisterUserUseCase,
    private val resources: ResourceProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        if (_uiState.value.loginResult is RequestResult.Loading) return

        val validation = validateLogin(email, password)
        _uiState.update {
            it.copy(
                emailError = if (validation.emailError != null) resources.getString(R.string.valid_email_required) else null,
                passwordError = if (validation.passwordError != null) resources.getString(R.string.login_password_minimum) else null,
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
                        loginResult = RequestResult.Success(resources.getString(R.string.login_success)),
                        authenticatedRole = user.role
                    )
                } else {
                    state.copy(loginResult = RequestResult.Failure(resources.getString(R.string.login_failure)))
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
                        registerResult = RequestResult.Success(resources.getString(R.string.register_success)),
                        registeredEmail = result.user.email
                    )
                }

                RegisterResult.EmailAlreadyExists -> _uiState.update {
                    it.copy(
                        emailError = resources.getString(R.string.email_already_registered),
                        registerResult = RequestResult.Failure(resources.getString(R.string.email_already_registered))
                    )
                }

                RegisterResult.InvalidName -> _uiState.update {
                    it.copy(
                        nameError = resources.getString(R.string.name_required),
                        registerResult = RequestResult.Failure(resources.getString(R.string.review_name))
                    )
                }

                RegisterResult.InvalidEmail -> _uiState.update {
                    it.copy(
                        emailError = resources.getString(R.string.valid_email_required),
                        registerResult = RequestResult.Failure(resources.getString(R.string.review_email))
                    )
                }

                RegisterResult.WeakPassword -> _uiState.update {
                    it.copy(
                        passwordError = resources.getString(R.string.min_eight_characters),
                        registerResult = RequestResult.Failure(resources.getString(R.string.review_password))
                    )
                }
            }
        }
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
