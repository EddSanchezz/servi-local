package com.servilocal.features.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.PostRepository
import com.servilocal.domain.usecase.notifications.ObserveNotificationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    postRepository: PostRepository,
    observeNotifications: ObserveNotificationsUseCase
) : ViewModel() {
    val currentUser = authRepository.currentUser
    val posts = postRepository.posts
    val notifications = observeNotifications()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun updateCurrentUser(name: String, email: String): Boolean {
        if (name.isBlank() || !email.trim().matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) return false
        authRepository.updateCurrentUser(name.trim(), email.trim())
        return true
    }
}
