package com.servilocal.data.repository.memory

import com.servilocal.domain.model.Badge
import com.servilocal.domain.model.BadgeCatalog
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

    private val _badges = MutableStateFlow(BadgeCatalog.all)
    override val badges: StateFlow<List<Badge>> = _badges.asStateFlow()

    private val _userBadges = MutableStateFlow(seedUserBadges())
    override val userBadges: StateFlow<List<UserBadge>> = _userBadges.asStateFlow()

    override suspend fun award(userId: String, badgeId: String, sourcePostId: String?): Boolean {
        if (userId <= "0" || _badges.value.none { it.id == badgeId }) return false

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

    private fun seedUserBadges(): List<UserBadge> = listOf(
        UserBadge(id = "user_badge_val_1", userId = "1", badgeId = "badge_1"),
        UserBadge(id = "user_badge_val_2", userId = "1", badgeId = "badge_2")
    )
}
