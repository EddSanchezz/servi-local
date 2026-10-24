package com.servilocal.features.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servilocal.core.util.RequestResult
import com.servilocal.domain.usecase.auth.RecoverPasswordResult
import com.servilocal.domain.usecase.auth.RecoverPasswordUseCase
import com.servilocal.domain.usecase.auth.ResetPasswordResult
import com.servilocal.domain.usecase.auth.ResetPasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val RESET_CODE_LENGTH = 5
private const val MIN_PASSWORD_LENGTH = 8

data class PasswordRecoveryUiState(
    val email: String = "",
    val emailError: String? = null,
    val codeDigits: List<String> = List(RESET_CODE_LENGTH) { "" },
    val codeError: String? = null,
    val newPassword: String = "",
    val confirmPassword: String = "",
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val requestResult: RequestResult? = null,
    val resetResult: RequestResult? = null,
    val demoCode: String? = null
) {
    val code: String get() = codeDigits.joinToString("")
    val isCodeComplete: Boolean get() = codeDigits.all { it.length == 1 }
}

@HiltViewModel
class PasswordRecoveryViewModel @Inject constructor(
    private val recoverPasswordUseCase: RecoverPasswordUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PasswordRecoveryUiState())
    val uiState: StateFlow<PasswordRecoveryUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update {
            it.copy(
                email = value,
                emailError = null,
                requestResult = null,
                demoCode = null
            )
        }
    }

    fun requestCode() {
        if (_uiState.value.requestResult is RequestResult.Loading) return

        viewModelScope.launch {
            val email = _uiState.value.email
            _uiState.update {
                it.copy(
                    emailError = null,
                    requestResult = RequestResult.Loading,
                    demoCode = null
                )
            }
            when (val result = recoverPasswordUseCase(email)) {
                is RecoverPasswordResult.Success -> _uiState.update {
                    it.copy(
                        email = it.email.trim().lowercase(),
                        demoCode = result.code,
                        requestResult = RequestResult.Success(
                            "Código generado. Úsalo para restablecer tu contraseña."
                        )
                    )
                }

                RecoverPasswordResult.InvalidEmail -> _uiState.update {
                    it.copy(
                        emailError = "Ingresa un correo válido para continuar.",
                        requestResult = RequestResult.Failure(
                            "Ingresa un correo válido para continuar."
                        )
                    )
                }

                RecoverPasswordResult.UserNotFound -> _uiState.update {
                    it.copy(
                        emailError = "No encontramos una cuenta con ese correo.",
                        requestResult = RequestResult.Failure(
                            "No encontramos una cuenta con ese correo."
                        )
                    )
                }
            }
        }
    }

    fun clearRequestFeedback() {
        _uiState.update {
            it.copy(emailError = null, requestResult = null, demoCode = null)
        }
    }

    fun prepareReset(email: String) {
        _uiState.update {
            it.copy(
                email = email.trim().lowercase(),
                emailError = null,
                codeDigits = List(RESET_CODE_LENGTH) { "" },
                codeError = null,
                newPassword = "",
                confirmPassword = "",
                passwordError = null,
                confirmPasswordError = null,
                resetResult = null
            )
        }
    }

    fun onCodeDigitChange(index: Int, value: String) {
        if (index !in 0 until RESET_CODE_LENGTH) return

        val digits = value.filter(Char::isDigit)
        _uiState.update { state ->
            val updated = state.codeDigits.toMutableList()
            when {
                value.isEmpty() -> updated[index] = ""
                digits.isEmpty() -> return@update state
                digits.length == 1 -> updated[index] = digits
                else -> digits
                    .take(RESET_CODE_LENGTH - index)
                    .forEachIndexed { offset, digit -> updated[index + offset] = digit.toString() }
            }
            state.copy(codeDigits = updated, codeError = null, resetResult = null)
        }
    }

    fun onNewPasswordChange(value: String) {
        _uiState.update {
            it.copy(newPassword = value, passwordError = null, resetResult = null)
        }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update {
            it.copy(confirmPassword = value, confirmPasswordError = null, resetResult = null)
        }
    }

    fun submitReset() {
        val state = _uiState.value
        if (state.resetResult is RequestResult.Loading) return

        val passwordValid = state.newPassword.length >= MIN_PASSWORD_LENGTH
        val passwordsMatch = state.newPassword == state.confirmPassword
        if (!state.isCodeComplete || !passwordValid || !passwordsMatch) {
            _uiState.update {
                it.copy(
                    codeError = if (!state.isCodeComplete) {
                        "Ingresa los 5 dígitos del código."
                    } else null,
                    passwordError = if (!passwordValid) {
                        "Usa al menos 8 caracteres."
                    } else null,
                    confirmPasswordError = if (!passwordsMatch) {
                        "Las contraseñas no coinciden."
                    } else null,
                    resetResult = null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(resetResult = RequestResult.Loading) }
            when (resetPasswordUseCase(state.email, state.code, state.newPassword)) {
                ResetPasswordResult.Success -> _uiState.update {
                    it.copy(
                        demoCode = null,
                        resetResult = RequestResult.Success(
                            "Contraseña actualizada. Ya puedes iniciar sesión."
                        )
                    )
                }

                ResetPasswordResult.InvalidEmail -> _uiState.update {
                    it.copy(
                        resetResult = RequestResult.Failure("El correo no es válido.")
                    )
                }

                ResetPasswordResult.UserNotFound -> _uiState.update {
                    it.copy(
                        resetResult = RequestResult.Failure(
                            "No encontramos una cuenta con ese correo."
                        )
                    )
                }

                ResetPasswordResult.InvalidCode -> _uiState.update {
                    it.copy(
                        codeError = "El código no es correcto o ya fue usado.",
                        resetResult = RequestResult.Failure(
                            "El código no es correcto o ya fue usado."
                        )
                    )
                }

                ResetPasswordResult.WeakPassword -> _uiState.update {
                    it.copy(
                        passwordError = "Usa al menos 8 caracteres.",
                        resetResult = RequestResult.Failure("Revisa la nueva contraseña.")
                    )
                }
            }
        }
    }

    fun clearResetFeedback() {
        _uiState.update {
            it.copy(
                codeError = null,
                passwordError = null,
                confirmPasswordError = null,
                resetResult = null
            )
        }
    }
}
