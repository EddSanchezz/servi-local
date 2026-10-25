package com.servilocal.features.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.PostRepository
import com.servilocal.domain.usecase.notifications.ObserveNotificationsUseCase
import com.servilocal.domain.usecase.posts.TogglePostLikeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class HomeUiState(
    val searchQuery: String = "",
    val selectedCategory: String = "Todos",
    val displayMode: ViewDisplayMode = ViewDisplayMode.LIST
)

enum class ViewDisplayMode { LIST, MAP }

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val postRepository: PostRepository,
    authRepository: AuthRepository,
    observeNotifications: ObserveNotificationsUseCase,
    private val togglePostLikeUseCase: TogglePostLikeUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    val posts = postRepository.posts
    val currentUser = authRepository.currentUser
    val notifications = observeNotifications()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun setSearchQuery(value: String) = _uiState.update { it.copy(searchQuery = value) }
    fun setSelectedCategory(value: String) = _uiState.update { it.copy(selectedCategory = value) }
    fun setDisplayMode(value: ViewDisplayMode) = _uiState.update { it.copy(displayMode = value) }
    fun toggleUpvote(postId: String): Boolean = togglePostLikeUseCase(postId)
}
