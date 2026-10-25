package com.servilocal.features.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servilocal.domain.model.BadgeDisplay
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.PostRepository
import com.servilocal.domain.usecase.notifications.ObserveNotificationsUseCase
import com.servilocal.domain.usecase.profile.CheckAndAwardBadgesUseCase
import com.servilocal.domain.usecase.profile.GetProfileStatsUseCase
import com.servilocal.domain.usecase.profile.ObserveUserBadgesUseCase
import com.servilocal.domain.usecase.profile.ProfileStats
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val stats: ProfileStats? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    postRepository: PostRepository,
    observeNotifications: ObserveNotificationsUseCase,
    observeUserBadges: ObserveUserBadgesUseCase,
    private val getProfileStats: GetProfileStatsUseCase,
    private val checkAndAwardBadges: CheckAndAwardBadgesUseCase
) : ViewModel() {

    val currentUser = authRepository.currentUser
    val posts = postRepository.posts
    val notifications = observeNotifications()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val badges: StateFlow<List<BadgeDisplay>> = observeUserBadges()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getProfileStats().collect { stats ->
                _uiState.update { it.copy(stats = stats, isLoading = false) }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            checkAndAwardBadges()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun updateCurrentUser(name: String, email: String): Boolean {
        if (name.isBlank() || !email.trim().matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) return false
        authRepository.updateCurrentUser(name.trim(), email.trim())
        refresh()
        return true
    }
}
