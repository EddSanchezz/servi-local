package com.example.features.profile.viewmodel

import androidx.lifecycle.ViewModel
import com.example.core.data.repository.MockDataRepository

class ProfileViewModel(private val repository: MockDataRepository) : ViewModel() {
    val currentUser = repository.currentUser
    val posts = repository.posts
    val notifications = repository.notifications

    fun updateCurrentUser(name: String, email: String): Boolean {
        if (name.isBlank() || !email.trim().matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) return false
        repository.updateCurrentUser(name.trim(), email.trim())
        return true
    }
}
