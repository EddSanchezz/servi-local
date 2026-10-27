package com.servilocal.domain.usecase.moderation

import com.servilocal.domain.model.NotificationType
import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.model.UserRole
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.PostRepository
import com.servilocal.domain.usecase.notifications.CreateNotificationUseCase
import jakarta.inject.Inject

class RejectPostUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository,
    private val createNotification: CreateNotificationUseCase
) {
    operator fun invoke(postId: String, reason: String, notes: String? = null): Boolean {
        if (authRepository.currentUser.value.role != UserRole.MODERATOR || reason.isBlank()) {
            return false
        }
        val post = postRepository.posts.value.find { it.id == postId }
        if (post == null || post.status !in setOf(PostStatus.PENDING_VERIFICATION, PostStatus.APPROVED)) {
            return false
        }
        val rejected = postRepository.moderatePost(
            postId = postId,
            status = PostStatus.REJECTED,
            reason = reason.trim(),
            notes = notes?.trim()
        )
        if (!rejected) return false

        post.providerId?.let { authorId ->
            createNotification(
                userId = authorId,
                title = "Publicación rechazada",
                body = "Tu publicación '${post.title}' fue rechazada. Motivo: ${reason.trim()}.",
                type = NotificationType.SYSTEM,
                relatedPostId = post.id
            )
        }
        return true
    }
}
