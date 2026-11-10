package com.servilocal.features.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servilocal.core.util.RequestResult
import com.servilocal.core.util.ResourceProvider
import com.servilocal.R
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
    val actionCode: String = "",
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
    private val resetPasswordUseCase: ResetPasswordUseCase,
    private val resources: ResourceProvider
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
                        demoCode = null,
                        requestResult = RequestResult.Success(
                            resources.getString(R.string.recovery_email_sent)
                        )
                    )
                }
                RecoverPasswordResult.UserNotFound -> _uiState.update {
                    it.copy(
                        email = it.email.trim().lowercase(),
                        demoCode = null,
                        requestResult = RequestResult.Success(
                            resources.getString(R.string.recovery_email_sent)
                        )
                    )
                }

                RecoverPasswordResult.InvalidEmail -> _uiState.update {
                    it.copy(
                        emailError = resources.getString(R.string.email_invalid_continue),
                        requestResult = RequestResult.Failure(
                            resources.getString(R.string.email_invalid_continue)
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

    fun prepareReset(email: String, actionCode: String = "") {
        _uiState.update {
            it.copy(
                email = email.trim().lowercase(),
                actionCode = actionCode,
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
        val codeValid = state.actionCode.isNotBlank() || state.isCodeComplete
        if (!codeValid || !passwordValid || !passwordsMatch) {
            _uiState.update {
                it.copy(
                    codeError = if (!codeValid) {
                        resources.getString(R.string.reset_code_digits_required)
                    } else null,
                    passwordError = if (!passwordValid) {
                        resources.getString(R.string.min_eight_characters)
                    } else null,
                    confirmPasswordError = if (!passwordsMatch) {
                        resources.getString(R.string.passwords_dont_match)
                    } else null,
                    resetResult = null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(resetResult = RequestResult.Loading) }
            val code = state.actionCode.ifBlank { state.code }
            when (resetPasswordUseCase(state.email, code, state.newPassword)) {
                ResetPasswordResult.Success -> _uiState.update {
                    it.copy(
                        demoCode = null,
                        resetResult = RequestResult.Success(
                            resources.getString(R.string.password_reset_success)
                        )
                    )
                }

                ResetPasswordResult.InvalidEmail -> _uiState.update {
                    it.copy(
                        resetResult = RequestResult.Failure(resources.getString(R.string.invalid_email))
                    )
                }

                ResetPasswordResult.UserNotFound -> _uiState.update {
                    it.copy(
                        resetResult = RequestResult.Failure(
                            resources.getString(R.string.account_not_found)
                        )
                    )
                }

                ResetPasswordResult.InvalidCode -> _uiState.update {
                    it.copy(
                        codeError = resources.getString(R.string.invalid_or_used_code),
                        resetResult = RequestResult.Failure(
                            resources.getString(R.string.invalid_or_used_code)
                        )
                    )
                }

                ResetPasswordResult.WeakPassword -> _uiState.update {
                    it.copy(
                        passwordError = resources.getString(R.string.min_eight_characters),
                        resetResult = RequestResult.Failure(resources.getString(R.string.review_new_password))
                    )
                }
                ResetPasswordResult.Failed -> _uiState.update {
                    it.copy(
                        resetResult = RequestResult.Failure(
                            resources.getString(R.string.login_failure)
                        )
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
