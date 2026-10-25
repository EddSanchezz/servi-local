package com.servilocal.domain.usecase.posts

import com.servilocal.domain.model.NotificationType
import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.CommentRepository
import com.servilocal.domain.repository.PostRepository
import com.servilocal.domain.usecase.notifications.CreateNotificationUseCase
import jakarta.inject.Inject

sealed interface AddCommentResult {
    data object Success : AddCommentResult
    data object EmptyContent : AddCommentResult
    data object TooLong : AddCommentResult
    data object PostNotFound : AddCommentResult
    data object PostNotVerified : AddCommentResult
}

class AddPostCommentUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val authRepository: AuthRepository,
    private val createNotification: CreateNotificationUseCase
) {
    operator fun invoke(postId: String, text: String): AddCommentResult {
        val content = text.trim()
        if (content.isEmpty()) return AddCommentResult.EmptyContent
        if (content.length > MAX_LENGTH) return AddCommentResult.TooLong

        val post = postRepository.posts.value.firstOrNull { it.id == postId }
            ?: return AddCommentResult.PostNotFound
        val currentUser = authRepository.currentUser.value

        if (post.status != PostStatus.APPROVED) return AddCommentResult.PostNotVerified

        val added = commentRepository.addComment(
            postId = postId,
            userId = currentUser.id,
            authorName = currentUser.name,
            content = content
        )
        if (!added) return AddCommentResult.PostNotFound

        val authorId = post.providerId
        if (authorId != null && authorId != currentUser.id) {
            createNotification(
                userId = authorId,
                title = "Nuevo comentario en tu servicio",
                body = "${currentUser.name} comentó en '${post.title}': \"$content\"",
                type = NotificationType.COMMENT,
                relatedPostId = post.id
            )
        }
        return AddCommentResult.Success
    }

    companion object {
        const val MAX_LENGTH = 500
    }
}
