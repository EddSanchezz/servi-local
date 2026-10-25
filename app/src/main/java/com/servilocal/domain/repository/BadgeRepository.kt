package com.servilocal.domain.repository

import com.servilocal.domain.model.Badge
import com.servilocal.domain.model.UserBadge
import kotlinx.coroutines.flow.StateFlow

interface BadgeRepository {
    /** Catálogo de insignias disponibles. */
    val badges: StateFlow<List<Badge>>

    /** Insignias otorgadas a los usuarios. */
    val userBadges: StateFlow<List<UserBadge>>

    /** Otorga una insignia una sola vez. Devuelve true si se otorgó ahora. */
    suspend fun award(userId: Long, badgeId: String): Boolean
}
