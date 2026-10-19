package com.example.features.explore.viewmodel

import androidx.lifecycle.ViewModel
import com.example.core.data.repository.MockDataRepository
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

class ExploreViewModel(private val repository: MockDataRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()
    val posts = repository.posts
    val notifications = repository.notifications

    fun setSearchQuery(value: String) = _uiState.update { it.copy(searchQuery = value) }
    fun setSelectedCategory(value: String) = _uiState.update { it.copy(selectedCategory = value) }
    fun setDisplayMode(value: ViewDisplayMode) = _uiState.update { it.copy(displayMode = value) }
    fun toggleUpvote(postId: String) = repository.toggleUpvote(postId)
    fun startConversation(postId: String): String? = repository.startConversation(postId)
}
