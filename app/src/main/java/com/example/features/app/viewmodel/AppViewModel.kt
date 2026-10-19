package com.example.features.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.core.data.model.UserRole
import com.example.core.data.repository.MockDataRepository
import com.example.features.auth.viewmodel.AuthViewModel
import com.example.features.chat.viewmodel.ChatDetailViewModel
import com.example.features.chat.viewmodel.MessagesViewModel
import com.example.features.explore.viewmodel.ExploreViewModel
import com.example.features.explore.viewmodel.ServiceDetailViewModel
import com.example.features.home.viewmodel.HomeViewModel
import com.example.features.moderation.viewmodel.ModerationViewModel
import com.example.features.notifications.viewmodel.NotificationsViewModel
import com.example.features.posts.viewmodel.CreatePostViewModel
import com.example.features.profile.viewmodel.ProfileViewModel
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
class AppViewModel(private val repository: MockDataRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    fun login(role: UserRole) = _uiState.update { it.copy(activeRole = role) }
    fun switchRole(role: UserRole) = login(role)
    fun logout() = _uiState.update { it.copy(activeRole = UserRole.USER) }
    fun setDarkTheme(enabled: Boolean) = _uiState.update { it.copy(darkTheme = enabled) }
    fun setNotificationsEnabled(enabled: Boolean) = _uiState.update { it.copy(notificationsEnabled = enabled) }
    fun toggleModeratorShift() = repository.toggleModeratorShift()
}

class AppViewModelFactory(private val repository: MockDataRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val viewModel = when (modelClass) {
            AppViewModel::class.java -> AppViewModel(repository)
            AuthViewModel::class.java -> AuthViewModel(repository)
            HomeViewModel::class.java -> HomeViewModel(repository)
            ExploreViewModel::class.java -> ExploreViewModel(repository)
            ServiceDetailViewModel::class.java -> ServiceDetailViewModel(repository)
            CreatePostViewModel::class.java -> CreatePostViewModel(repository)
            MessagesViewModel::class.java -> MessagesViewModel(repository)
            ChatDetailViewModel::class.java -> ChatDetailViewModel(repository)
            NotificationsViewModel::class.java -> NotificationsViewModel(repository)
            ModerationViewModel::class.java -> ModerationViewModel(repository)
            ProfileViewModel::class.java -> ProfileViewModel(repository)
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
        return viewModel as T
    }
}
