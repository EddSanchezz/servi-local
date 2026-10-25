package com.servilocal.domain.usecase.posts

import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.CommentRepository
import com.servilocal.domain.repository.PostRepository
import jakarta.inject.Inject

class AddPostCommentUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val authRepository: AuthRepository
) {
    operator fun invoke(postId: String, text: String): Boolean {
        val content = text.trim()
        if (content.isEmpty()) return false

        val post = postRepository.posts.value.firstOrNull { it.id == postId } ?: return false
        val currentUser = authRepository.currentUser.value

        if (post.status != PostStatus.APPROVED) return false

        return commentRepository.addComment(
            postId = postId,
            userId = currentUser.id,
            authorName = currentUser.name,
            content = content
        )
    }
}
