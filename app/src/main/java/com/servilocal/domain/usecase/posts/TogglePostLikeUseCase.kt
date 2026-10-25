package com.servilocal.domain.usecase.posts

import com.servilocal.domain.model.NotificationType
import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.LikeRepository
import com.servilocal.domain.repository.PostRepository
import com.servilocal.domain.usecase.notifications.CreateNotificationUseCase
import jakarta.inject.Inject

sealed interface ToggleLikeResult {
    /** liked indica si el post quedó con el like del usuario después del cambio. */
    data class Success(val liked: Boolean) : ToggleLikeResult
    data object PostNotFound : ToggleLikeResult
    data object PostNotVerified : ToggleLikeResult
    data object OwnPost : ToggleLikeResult
}

class TogglePostLikeUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val likeRepository: LikeRepository,
    private val authRepository: AuthRepository,
    private val createNotification: CreateNotificationUseCase
) {
    operator fun invoke(postId: String): ToggleLikeResult {
        val post = postRepository.posts.value.firstOrNull { it.id == postId }
            ?: return ToggleLikeResult.PostNotFound
        val currentUser = authRepository.currentUser.value

        if (post.status != PostStatus.APPROVED) return ToggleLikeResult.PostNotVerified
        if (isAuthor(post.providerId, post.providerName, currentUser.id, currentUser.name)) {
            return ToggleLikeResult.OwnPost
        }

        val liked = likeRepository.toggleLike(postId, currentUser.id)
        // Solo se avisa al autor cuando recibe un apoyo nuevo, no cuando se retira.
        val authorId = post.providerId
        if (liked && authorId != null) {
            createNotification(
                userId = authorId,
                title = "Nuevo apoyo en tu servicio",
                body = "${currentUser.name} apoyó tu publicación '${post.title}'.",
                type = NotificationType.INTERACTION,
                relatedPostId = post.id
            )
        }
        return ToggleLikeResult.Success(liked)
    }

    private fun isAuthor(
        providerId: Long?,
        providerName: String,
        userId: Long,
        userName: String
    ): Boolean = (providerId != null && providerId == userId) ||
        providerName.equals(userName, ignoreCase = true)
}
