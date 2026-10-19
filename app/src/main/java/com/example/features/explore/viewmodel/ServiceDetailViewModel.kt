package com.example.features.explore.viewmodel

import androidx.lifecycle.ViewModel
import com.example.core.data.repository.MockDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ServiceDetailUiState(val showRequestSheet: Boolean = false)

class ServiceDetailViewModel(private val repository: MockDataRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ServiceDetailUiState())
    val uiState: StateFlow<ServiceDetailUiState> = _uiState.asStateFlow()
    val posts = repository.posts
    val currentUser = repository.currentUser

    fun showRequestDialog(show: Boolean) = _uiState.update { it.copy(showRequestSheet = show) }
    fun toggleUpvote(postId: String) = repository.toggleUpvote(postId)
    fun addComment(postId: String, text: String) {
        val post = posts.value.firstOrNull { it.id == postId } ?: return
        val author = currentUser.value.name
        repository.addComment(postId, text, author, author == post.providerName)
    }
    fun submitServiceRequest(postId: String, name: String, contact: String, address: String, description: String): String {
        val chatId = repository.createServiceRequest(postId, name, contact, address, description)
        showRequestDialog(false)
        return chatId
    }
    fun reportPost(postId: String, reason: String, description: String) {
        val post = posts.value.firstOrNull { it.id == postId } ?: return
        repository.createReport(reason, description.ifBlank { "Reporte enviado por la comunidad." }, "Publicación", post.title, targetPostId = post.id)
    }
}
