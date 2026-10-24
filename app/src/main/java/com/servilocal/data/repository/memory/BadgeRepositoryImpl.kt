package com.servilocal.data.repository.memory

import com.servilocal.domain.model.Badge
import com.servilocal.domain.model.UserBadge
import com.servilocal.domain.repository.BadgeRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

@Singleton
class BadgeRepositoryImpl @Inject constructor() : BadgeRepository {

    private val _badges = MutableStateFlow(seedBadges())
    override val badges: StateFlow<List<Badge>> = _badges.asStateFlow()

    private val _userBadges = MutableStateFlow(seedUserBadges())
    override val userBadges: StateFlow<List<UserBadge>> = _userBadges.asStateFlow()

    override suspend fun award(userId: String, badgeId: String): Boolean {
        if (userId.isBlank() || _badges.value.none { it.id == badgeId }) return false

        val alreadyAwarded = _userBadges.value.any {
            it.userId == userId && it.badgeId == badgeId
        }
        if (alreadyAwarded) return false

        _userBadges.update { current ->
            current + UserBadge(
                id = "user_badge_${UUID.randomUUID()}",
                userId = userId,
                badgeId = badgeId
            )
        }
        return true
    }

    private fun seedBadges(): List<Badge> = listOf(
        Badge(
            id = "badge_1",
            name = "Primera Publicación",
            description = "Publicaste tu primer servicio en la comunidad.",
            iconUrl = "🥇",
            pointsRequired = 0
        ),
        Badge(
            id = "badge_2",
            name = "Influencer",
            description = "Alcanzaste 100 puntos de reputación.",
            iconUrl = "🔥",
            pointsRequired = 100
        ),
        Badge(
            id = "badge_3",
            name = "Súper Proveedor",
            description = "Alcanzaste 250 puntos de reputación.",
            iconUrl = "⭐",
            pointsRequired = 250
        ),
        Badge(
            id = "badge_4",
            name = "Comunitario",
            description = "Alcanzaste 500 puntos de reputación.",
            iconUrl = "🤝",
            pointsRequired = 500
        )
    )

    private fun seedUserBadges(): List<UserBadge> = listOf(
        UserBadge(id = "user_badge_val_1", userId = "user_val_1", badgeId = "badge_1"),
        UserBadge(id = "user_badge_val_2", userId = "user_val_1", badgeId = "badge_2")
    )
}
