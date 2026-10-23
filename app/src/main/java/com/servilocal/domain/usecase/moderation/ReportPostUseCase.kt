package com.servilocal.domain.usecase.moderation

import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.ModerationRepository
import com.servilocal.domain.repository.PostRepository
import jakarta.inject.Inject

class ReportPostUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository,
    private val moderationRepository: ModerationRepository
) {
    operator fun invoke(postId: String, reason: String, description: String): Boolean {
        val post = postRepository.posts.value.firstOrNull { it.id == postId } ?: return false
        val currentUser = authRepository.currentUser.value

        // Rule: A post cannot be reported by the user who created it
        val isAuthor = (post.providerId != null && post.providerId == currentUser.id) ||
                post.providerName.equals(currentUser.name, ignoreCase = true)

        if (isAuthor) {
            return false
        }

        moderationRepository.createReport(
            reason = reason,
            description = description.ifBlank { "Reporte enviado por la comunidad." },
            targetType = "Publicación",
            targetTitle = post.title,
            targetPostId = post.id
        )
        return true
    }
}
