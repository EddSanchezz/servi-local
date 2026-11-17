package com.servilocal.domain.usecase.profile

import com.servilocal.domain.model.User
import jakarta.inject.Inject

/**
 * Otorga la reputación de una publicación recién creada y vuelve a evaluar
 * las insignias del proveedor.
 */
class AwardPublicationPointsUseCase @Inject constructor(
    private val addPoints: AddPointsUseCase,
    private val checkAndAwardBadges: CheckAndAwardBadgesUseCase
) {
    suspend operator fun invoke(providerId: String, postId: String): User? {
        if (providerId.isBlank() || postId.isBlank()) return null
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
