package com.servilocal.domain.usecase.profile

import com.servilocal.domain.model.Badge
import com.servilocal.domain.model.BadgeCriterion
import com.servilocal.domain.model.NotificationType
import com.servilocal.domain.model.User
import com.servilocal.domain.repository.BadgeRepository
import com.servilocal.domain.repository.PostRepository
import com.servilocal.domain.repository.SessionRepository
import com.servilocal.domain.repository.UserRepository
import com.servilocal.domain.usecase.notifications.CreateNotificationUseCase
import jakarta.inject.Inject

/**
 * Revisa los puntos y las publicaciones del usuario contra el catálogo y otorga
 * las insignias que todavía no tiene. Cada insignia nueva genera una notificación.
 * Es un caso de uso interno.
 */
class CheckAndAwardBadgesUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
    private val badgeRepository: BadgeRepository,
    private val postRepository: PostRepository,
    private val createNotification: CreateNotificationUseCase
) {
    suspend operator fun invoke(userId: Long? = null): List<Badge> {
        val targetUserId = userId ?: sessionRepository.session.value?.userId ?: return emptyList()
        val user = userRepository.findById(targetUserId) ?: return emptyList()
        val awardedIds = badgeRepository.userBadges.value
            .filter { it.userId == targetUserId }
            .map { it.badgeId }
            .toSet()

        val publishedPosts = postRepository.posts.value.count { post ->
            post.providerId == user.id ||
                (post.providerId == null && post.providerName.equals(user.name, ignoreCase = true))
        }

        val newBadges = badgeRepository.badges.value.filter { badge ->
            badge.id !in awardedIds && badge.isEligible(user, publishedPosts)
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

    private fun Badge.isEligible(user: User, publishedPosts: Int): Boolean = when (criterion) {
        BadgeCriterion.POINTS -> user.points >= threshold
        BadgeCriterion.CREATED_POSTS -> publishedPosts >= threshold
    }
}
