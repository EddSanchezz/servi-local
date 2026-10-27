package com.servilocal.domain.usecase.posts

import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.model.UserRole
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.PostRepository
import jakarta.inject.Inject

/** Lets an owner finish an approved post and lets a moderator resolve a post during review. */
class MarkPostAsResolvedUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository
) {
    operator fun invoke(postId: String): Boolean {
        val post = postRepository.posts.value.firstOrNull { it.id == postId } ?: return false
        return when (authRepository.currentUser.value.role) {
            UserRole.USER -> postRepository.finishPost(postId)
            UserRole.MODERATOR -> {
                if (post.status == PostStatus.REJECTED || post.status == PostStatus.FINISHED) return false
                postRepository.moderatePost(postId, PostStatus.FINISHED)
            }
        }
    }
}
