package com.example.features.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.data.model.PostStatus
import com.example.core.data.model.ServicePost
import com.example.core.data.repository.MockDataRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val greetingName: String = "",
    val featuredPosts: List<ServicePost> = emptyList()
)

class HomeViewModel(repository: MockDataRepository) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = combine(repository.currentUser, repository.posts) { user, posts ->
        HomeUiState(
            greetingName = user.name.substringBefore(' '),
            featuredPosts = posts.filter { it.status == PostStatus.APPROVED }.take(3)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
