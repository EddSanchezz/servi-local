package com.servilocal.domain.usecase.profile

import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.model.UserLevel
import com.servilocal.domain.repository.BadgeRepository
import com.servilocal.domain.repository.PostRepository
import com.servilocal.domain.repository.SessionRepository
import com.servilocal.domain.repository.UserRepository
import jakarta.inject.Inject

/**
 * Combina el usuario autenticado con sus publicaciones e insignias para construir
 * las estadísticas del perfil y del dashboard de gamificación.
 */
class GetProfileStatsUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val badgeRepository: BadgeRepository
) {
    suspend operator fun invoke(): ProfileStats? {
        val session = sessionRepository.session.value ?: return null
        val user = userRepository.findById(session.userId) ?: return null

        val userPosts = postRepository.posts.value.filter { post ->
            post.providerId == user.id || post.providerName == user.name
        }
        val nextLevelPoints = user.level.next?.minPoints ?: MASTER_LEVEL_POINTS
        val progress = if (nextLevelPoints <= 0) {
            1f
        } else {
            (user.points.toFloat() / nextLevelPoints).coerceIn(0f, 1f)
        }

        return ProfileStats(
            user = user,
            levelLabel = user.level.displayName(),
            nextLevelPoints = nextLevelPoints,
            progress = progress,
            publishedPosts = userPosts.count { it.status == PostStatus.APPROVED },
            pendingPosts = userPosts.count { it.status == PostStatus.PENDING_VERIFICATION },
            totalPosts = userPosts.size,
            badgeCount = badgeRepository.userBadges.value.count { it.userId == user.id }
        )
    }

    private fun UserLevel.displayName(): String = when (this) {
        UserLevel.PRINCIPIANTE -> "Nivel 1: Principiante"
        UserLevel.PROFESIONAL -> "Nivel 2: Profesional"
        UserLevel.EXPERTO -> "Nivel 3: Experto"
        UserLevel.MAESTRO -> "Nivel 4: Maestro"
    }

    private companion object {
        const val MASTER_LEVEL_POINTS = 1000
    }
}
