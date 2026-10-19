package com.servilocal.features.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.model.ServicePost
import com.servilocal.domain.usecase.home.ObserveHomeFeedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val greetingName: String = "",
    val featuredPosts: List<ServicePost> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeHomeFeed: ObserveHomeFeedUseCase
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = observeHomeFeed().map { data ->
        HomeUiState(
            greetingName = data.user.name.substringBefore(' '),
            featuredPosts = data.posts.filter { it.status == PostStatus.APPROVED }.take(3)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
