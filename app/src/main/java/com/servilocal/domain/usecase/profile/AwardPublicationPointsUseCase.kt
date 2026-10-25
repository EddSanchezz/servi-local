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
    suspend operator fun invoke(providerId: Long): User? {
        if (providerId <= 0L) return null
        val updatedUser = addPoints(providerId, POINTS_PER_PUBLICATION) ?: return null
        checkAndAwardBadges(providerId)
        return updatedUser
    }

    companion object {
        const val POINTS_PER_PUBLICATION = 50
    }
}
