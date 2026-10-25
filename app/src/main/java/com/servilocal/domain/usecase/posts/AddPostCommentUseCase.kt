package com.servilocal.domain.usecase.posts

import com.servilocal.domain.model.NotificationType
import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.CommentRepository
import com.servilocal.domain.repository.PostRepository
import com.servilocal.domain.usecase.notifications.CreateNotificationUseCase
import jakarta.inject.Inject

class AddPostCommentUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val authRepository: AuthRepository,
    private val createNotification: CreateNotificationUseCase
) {
    operator fun invoke(postId: String, text: String): Boolean {
        val content = text.trim()
        if (content.isEmpty()) return false

        val post = postRepository.posts.value.firstOrNull { it.id == postId } ?: return false
        val currentUser = authRepository.currentUser.value

        if (post.status != PostStatus.APPROVED) return false

        val added = commentRepository.addComment(
            postId = postId,
            userId = currentUser.id,
            authorName = currentUser.name,
            content = content
        )
        if (added) {
            val targetUserId = post.providerId ?: 2L
            if (targetUserId != currentUser.id) {
                createNotification(
                    userId = targetUserId,
                    title = "Nuevo comentario en tu servicio",
                    body = "${currentUser.name} comentó en '${post.title}': \"$content\"",
                    type = NotificationType.COMMENT,
                    relatedPostId = post.id
                )
            }
        }
        return added
    }
}
