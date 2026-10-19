package com.servilocal.features.explore.viewmodel

import androidx.lifecycle.ViewModel
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.ChatRepository
import com.servilocal.domain.repository.ModerationRepository
import com.servilocal.domain.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ServiceDetailUiState(val showRequestSheet: Boolean = false)

@HiltViewModel
class ServiceDetailViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val moderationRepository: ModerationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ServiceDetailUiState())
    val uiState: StateFlow<ServiceDetailUiState> = _uiState.asStateFlow()
    val posts = postRepository.posts
    val currentUser = authRepository.currentUser

    fun showRequestDialog(show: Boolean) = _uiState.update { it.copy(showRequestSheet = show) }
    fun toggleUpvote(postId: String) = postRepository.toggleUpvote(postId)
    fun addComment(postId: String, text: String) {
        val post = posts.value.firstOrNull { it.id == postId } ?: return
        val author = currentUser.value.name
        postRepository.addComment(postId, text, author, author == post.providerName)
    }
    fun submitServiceRequest(postId: String, name: String, contact: String, address: String, description: String): String {
        val chatId = chatRepository.createServiceRequest(postId, name, contact, address, description)
        showRequestDialog(false)
        return chatId
    }
    fun reportPost(postId: String, reason: String, description: String) {
        val post = posts.value.firstOrNull { it.id == postId } ?: return
        moderationRepository.createReport(reason, description.ifBlank { "Reporte enviado por la comunidad." }, "Publicación", post.title, targetPostId = post.id)
    }
}
