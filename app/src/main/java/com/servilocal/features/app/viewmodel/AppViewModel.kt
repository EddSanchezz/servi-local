package com.servilocal.features.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servilocal.domain.model.UserRole
import com.servilocal.domain.model.UserSession
import com.servilocal.domain.usecase.auth.LogoutUseCase
import com.servilocal.domain.usecase.auth.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppUiState(
    val activeRole: UserRole = UserRole.USER,
    val session: UserSession? = null,
    val darkTheme: Boolean = false,
    val notificationsEnabled: Boolean = true
)

/** Holds app-wide session and appearance preferences. Navigation belongs to AppNavHost. */
@HiltViewModel
class AppViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                _uiState.update {
                    it.copy(
                        session = session,
                        activeRole = session?.role ?: UserRole.USER
                    )
                }
            }
        }
    }

    fun login(role: UserRole) = _uiState.update { it.copy(activeRole = role) }
    fun switchRole(role: UserRole) = login(role)

    fun logout() {
        // La UI vuelve al login de inmediato; la sesión en memoria se limpia en segundo plano.
        _uiState.update { it.copy(activeRole = UserRole.USER, session = null) }
        viewModelScope.launch { logoutUseCase() }
    }

    fun setDarkTheme(enabled: Boolean) = _uiState.update { it.copy(darkTheme = enabled) }
    fun setNotificationsEnabled(enabled: Boolean) = _uiState.update { it.copy(notificationsEnabled = enabled) }
}
