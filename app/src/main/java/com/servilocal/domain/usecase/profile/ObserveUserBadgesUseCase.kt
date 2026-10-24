package com.servilocal.domain.usecase.profile

import com.servilocal.domain.model.BadgeDisplay
import com.servilocal.domain.repository.BadgeRepository
import com.servilocal.domain.repository.SessionRepository
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * Expone el catálogo de insignias con el estado desbloqueado/bloqueado del usuario
 * en sesión. Si no hay sesión, la lista queda vacía.
 */
class ObserveUserBadgesUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val badgeRepository: BadgeRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<BadgeDisplay>> =
        sessionRepository.session.flatMapLatest { session ->
            if (session == null) {
                flowOf(emptyList())
            } else {
                combine(badgeRepository.badges, badgeRepository.userBadges) { badges, userBadges ->
                    val awardedIds = userBadges
                        .filter { it.userId == session.userId }
                        .map { it.badgeId }
                        .toSet()

                    badges.map { badge ->
                        BadgeDisplay(
                            id = badge.id,
                            title = badge.name,
                            icon = badge.iconUrl,
                            unlocked = badge.id in awardedIds
                        )
                    }
                }
            }
        }
}
