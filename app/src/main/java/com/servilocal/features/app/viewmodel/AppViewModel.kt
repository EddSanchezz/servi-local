package com.servilocal.features.app.viewmodel

import androidx.lifecycle.ViewModel
import com.servilocal.domain.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AppUiState(
    val activeRole: UserRole = UserRole.USER,
    val darkTheme: Boolean = false,
    val notificationsEnabled: Boolean = true
)

/** Holds app-wide session and appearance preferences. Navigation belongs to AppNavHost. */
@HiltViewModel
class AppViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    fun login(role: UserRole) = _uiState.update { it.copy(activeRole = role) }
    fun switchRole(role: UserRole) = login(role)
    fun logout() = _uiState.update { it.copy(activeRole = UserRole.USER) }
    fun setDarkTheme(enabled: Boolean) = _uiState.update { it.copy(darkTheme = enabled) }
    fun setNotificationsEnabled(enabled: Boolean) = _uiState.update { it.copy(notificationsEnabled = enabled) }
}
