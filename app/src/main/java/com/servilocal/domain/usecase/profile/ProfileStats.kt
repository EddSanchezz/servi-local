package com.servilocal.domain.usecase.profile

import com.servilocal.domain.model.User

/** Estadísticas que alimentan el dashboard del perfil. */
data class ProfileStats(
    val user: User,
    val levelLabel: String,
    val nextLevelPoints: Int?,
    val progress: Float,
    val publishedPosts: Int,
    val pendingPosts: Int,
    val totalPosts: Int,
    val badgeCount: Int
) {
    val remainingPoints: Int
        get() = nextLevelPoints?.let { (it - user.points).coerceAtLeast(0) } ?: 0
}
