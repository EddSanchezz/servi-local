package com.servilocal.features.home.viewmodel

import androidx.lifecycle.ViewModel
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.ModerationRepository
import com.servilocal.domain.repository.PostRepository
import com.servilocal.domain.usecase.chat.StartConversationUseCase
import com.servilocal.domain.usecase.moderation.ReportPostUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject

@HiltViewModel
class ServiceDetailViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository,
    private val startConversationUseCase: StartConversationUseCase,
    private val moderationRepository: ModerationRepository,
    private val reportPostUseCase: ReportPostUseCase = ReportPostUseCase(postRepository, authRepository, moderationRepository)
) : ViewModel() {
    val posts = postRepository.posts
    val currentUser = authRepository.currentUser

    fun startConversation(postId: String): String? {
        val post = posts.value.firstOrNull { it.id == postId } ?: return null
        if (post.providerId == currentUser.value.id) return null
        return startConversationUseCase(postId)
    }
    fun toggleUpvote(postId: String) = postRepository.toggleUpvote(postId)
    fun addComment(postId: String, text: String) {
        val post = posts.value.firstOrNull { it.id == postId } ?: return
        val author = currentUser.value.name
        postRepository.addComment(postId, text, author, author == post.providerName)
    }
    fun reportPost(postId: String, reason: String, description: String): Boolean {
        return reportPostUseCase(postId, reason, description)
    }
}
