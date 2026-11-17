package com.servilocal.domain.usecase.profile

import com.servilocal.domain.model.User
import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.repository.PostRepository
import jakarta.inject.Inject

/**
 * Otorga reputación por una publicación aprobada y vuelve a evaluar las
 * insignias del proveedor. Los llamadores deben sincronizar únicamente posts aprobados.
 */
class AwardPublicationPointsUseCase @Inject constructor(
    private val addPoints: AddPointsUseCase,
    private val checkAndAwardBadges: CheckAndAwardBadgesUseCase,
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(providerId: String, postId: String): User? {
        if (providerId.isBlank() || postId.isBlank()) return null
        val sourcePost = postRepository.posts.value.firstOrNull {
            it.id == postId && it.providerId == providerId
        } ?: return null
        if (sourcePost.status != PostStatus.APPROVED) return null
        val updatedUser = addPoints(
            userId = providerId,
            points = POINTS_PER_PUBLICATION,
            eventId = "$PUBLICATION_EVENT_PREFIX$postId"
        ) ?: return null
        checkAndAwardBadges(providerId)
        return updatedUser
    }

    companion object {
        const val POINTS_PER_PUBLICATION = 50
        const val PUBLICATION_EVENT_PREFIX = "publication_"
    }
}
