package com.example.features.explore.viewmodel

import androidx.lifecycle.ViewModel
import com.example.domain.repository.ChatRepository
import com.example.domain.repository.NotificationRepository
import com.example.domain.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ExploreUiState(
    val searchQuery: String = "",
    val selectedCategory: String = "Todos",
    val displayMode: ViewDisplayMode = ViewDisplayMode.LIST
)

enum class ViewDisplayMode { LIST, MAP }

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val chatRepository: ChatRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()
    val posts = postRepository.posts
    val notifications = notificationRepository.notifications

    fun setSearchQuery(value: String) = _uiState.update { it.copy(searchQuery = value) }
    fun setSelectedCategory(value: String) = _uiState.update { it.copy(selectedCategory = value) }
    fun setDisplayMode(value: ViewDisplayMode) = _uiState.update { it.copy(displayMode = value) }
    fun toggleUpvote(postId: String) = postRepository.toggleUpvote(postId)
    fun startConversation(postId: String): String? = chatRepository.startConversation(postId)
}
