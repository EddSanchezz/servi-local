package com.servilocal.domain.usecase.moderation

import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.model.UserRole
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.PostRepository
import jakarta.inject.Inject

class RejectPostUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository
) {
    operator fun invoke(postId: String, reason: String, notes: String? = null): Boolean {
        if (authRepository.moderatorUser.value.role != UserRole.MODERATOR) {
            return false
        }
        val post = postRepository.posts.value.find { it.id == postId }
        if (post == null || post.status != PostStatus.PENDING_VERIFICATION) {
            return false
        }
        postRepository.rejectPost(postId, reason, notes)
        return true
    }
}
