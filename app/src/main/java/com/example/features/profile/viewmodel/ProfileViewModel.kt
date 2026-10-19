package com.example.features.profile.viewmodel

import androidx.lifecycle.ViewModel
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.NotificationRepository
import com.example.domain.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    postRepository: PostRepository,
    notificationRepository: NotificationRepository
) : ViewModel() {
    val currentUser = authRepository.currentUser
    val posts = postRepository.posts
    val notifications = notificationRepository.notifications

    fun updateCurrentUser(name: String, email: String): Boolean {
        if (name.isBlank() || !email.trim().matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) return false
        authRepository.updateCurrentUser(name.trim(), email.trim())
        return true
    }
}
