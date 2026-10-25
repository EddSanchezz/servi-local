package com.servilocal.domain.usecase.posts

import com.servilocal.domain.model.NotificationType
import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.LikeRepository
import com.servilocal.domain.repository.PostRepository
import com.servilocal.domain.usecase.notifications.CreateNotificationUseCase
import jakarta.inject.Inject

class TogglePostLikeUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val likeRepository: LikeRepository,
    private val authRepository: AuthRepository,
    private val createNotification: CreateNotificationUseCase
) {
    operator fun invoke(postId: String): Boolean {
        val post = postRepository.posts.value.firstOrNull { it.id == postId } ?: return false
        val currentUser = authRepository.currentUser.value

        if (post.status != PostStatus.APPROVED) return false
        if (isAuthor(post.providerId, post.providerName, currentUser.id, currentUser.name)) return false

        val liked = likeRepository.toggleLike(postId, currentUser.id)
        if (liked) {
            val targetUserId = post.providerId ?: 2L
            createNotification(
                userId = targetUserId,
                title = "Nuevo apoyo en tu servicio",
                body = "${currentUser.name} apoyó tu publicación '${post.title}'.",
                type = NotificationType.INTERACTION,
                relatedPostId = post.id
            )
        }
        return liked
    }

    private fun isAuthor(
        providerId: Long?,
        providerName: String,
        userId: Long,
        userName: String
    ): Boolean = (providerId != null && providerId == userId) ||
        providerName.equals(userName, ignoreCase = true)
}
