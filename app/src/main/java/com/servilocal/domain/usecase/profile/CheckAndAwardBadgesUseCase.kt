package com.servilocal.domain.usecase.profile

import com.servilocal.domain.model.Badge
import com.servilocal.domain.model.NotificationType
import com.servilocal.domain.repository.BadgeRepository
import com.servilocal.domain.repository.SessionRepository
import com.servilocal.domain.repository.UserRepository
import com.servilocal.domain.usecase.notifications.CreateNotificationUseCase
import jakarta.inject.Inject

/**
 * Revisa los puntos del usuario contra el catálogo y otorga las insignias que
 * todavía no tiene. Cada insignia nueva genera una notificación.
 * Es un caso de uso interno.
 */
class CheckAndAwardBadgesUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
    private val badgeRepository: BadgeRepository,
    private val createNotification: CreateNotificationUseCase
) {
    suspend operator fun invoke(userId: String? = null): List<Badge> {
        val targetUserId = userId ?: sessionRepository.session.value?.userId ?: return emptyList()
        val user = userRepository.findById(targetUserId) ?: return emptyList()
        val awardedIds = badgeRepository.userBadges.value
            .filter { it.userId == targetUserId }
            .map { it.badgeId }
            .toSet()

        val newBadges = badgeRepository.badges.value.filter { badge ->
            badge.pointsRequired <= user.points && badge.id !in awardedIds
        }

        val awarded = mutableListOf<Badge>()
        newBadges.forEach { badge ->
            if (badgeRepository.award(targetUserId, badge.id)) {
                awarded += badge
                createNotification(
                    userId = targetUserId,
                    title = "¡Nuevo logro desbloqueado: ${badge.name}!",
                    body = badge.description,
                    type = NotificationType.ACHIEVEMENT
                )
            }
        }
        return awarded
    }
}
