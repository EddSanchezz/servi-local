package com.servilocal.domain.usecase.posts

import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.LikeRepository
import com.servilocal.domain.repository.PostRepository
import jakarta.inject.Inject

class TogglePostLikeUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val likeRepository: LikeRepository,
    private val authRepository: AuthRepository
) {
    operator fun invoke(postId: String): Boolean {
        val post = postRepository.posts.value.firstOrNull { it.id == postId } ?: return false
        val currentUser = authRepository.currentUser.value

        if (post.status != PostStatus.APPROVED) return false
        if (isAuthor(post.providerId, post.providerName, currentUser.id, currentUser.name)) return false

        return likeRepository.toggleLike(postId, currentUser.id)
    }

    private fun isAuthor(
        providerId: Long?,
        providerName: String,
        userId: Long,
        userName: String
    ): Boolean = (providerId != null && providerId == userId) ||
        providerName.equals(userName, ignoreCase = true)
}
