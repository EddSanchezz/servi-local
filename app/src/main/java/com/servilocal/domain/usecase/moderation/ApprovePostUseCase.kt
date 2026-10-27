package com.servilocal.domain.usecase.moderation

import com.servilocal.domain.model.NotificationType
import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.model.UserRole
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.PostRepository
import com.servilocal.domain.usecase.notifications.CreateNotificationUseCase
import jakarta.inject.Inject

class ApprovePostUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository,
    private val createNotification: CreateNotificationUseCase
) {
    operator fun invoke(postId: String): Boolean {
        if (authRepository.currentUser.value.role != UserRole.MODERATOR) {
            return false
        }
        val post = postRepository.posts.value.find { it.id == postId }
        if (post == null || post.status != PostStatus.PENDING_VERIFICATION) {
            return false
        }
        postRepository.approvePost(postId)

        post.providerId?.let { authorId ->
            createNotification(
                userId = authorId,
                title = "Publicación aprobada",
                body = "¡Buenas noticias! Tu publicación '${post.title}' ha sido aprobada y ya se encuentra visible.",
                type = NotificationType.SYSTEM,
                relatedPostId = post.id
            )
        }
        return true
    }
}
