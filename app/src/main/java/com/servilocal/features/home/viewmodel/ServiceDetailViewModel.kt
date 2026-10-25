package com.servilocal.features.home.viewmodel

import androidx.lifecycle.ViewModel
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.ModerationRepository
import com.servilocal.domain.repository.PostRepository
import com.servilocal.domain.usecase.chat.StartConversationUseCase
import com.servilocal.domain.usecase.moderation.ReportPostUseCase
import com.servilocal.domain.usecase.posts.AddPostCommentUseCase
import com.servilocal.domain.usecase.posts.TogglePostLikeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject

@HiltViewModel
class ServiceDetailViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository,
    private val startConversationUseCase: StartConversationUseCase,
    private val moderationRepository: ModerationRepository,
    private val togglePostLikeUseCase: TogglePostLikeUseCase,
    private val addPostCommentUseCase: AddPostCommentUseCase,
    private val reportPostUseCase: ReportPostUseCase = ReportPostUseCase(postRepository, authRepository, moderationRepository)
) : ViewModel() {
    val posts = postRepository.posts
    val currentUser = authRepository.currentUser

    fun startConversation(postId: String): String? {
        val post = posts.value.firstOrNull { it.id == postId } ?: return null
        if (post.providerId == currentUser.value.id) return null
        return startConversationUseCase(postId)
    }
    fun toggleUpvote(postId: String): Boolean = togglePostLikeUseCase(postId)
    fun addComment(postId: String, text: String): Boolean = addPostCommentUseCase(postId, text)
    fun reportPost(postId: String, reason: String, description: String): Boolean {
        return reportPostUseCase(postId, reason, description)
    }
}
