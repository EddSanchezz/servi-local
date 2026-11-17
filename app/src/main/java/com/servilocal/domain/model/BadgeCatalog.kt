package com.servilocal.domain.model

/** The fixed achievement definitions; each user's unlocked state is persisted remotely. */
object BadgeCatalog {
    val all: List<Badge> = listOf(
        Badge(
            id = "badge_1",
            name = "Primera Publicación",
            description = "Publicaste tu primer servicio en la comunidad.",
            iconUrl = "🥇",
            criterion = BadgeCriterion.CREATED_POSTS,
            threshold = 1
        ),
        Badge(
            id = "badge_2",
            name = "Influencer",
            description = "Alcanzaste 100 puntos de reputación.",
            iconUrl = "🔥",
            criterion = BadgeCriterion.POINTS,
            threshold = 100
        ),
        Badge(
            id = "badge_3",
            name = "Súper Proveedor",
            description = "Alcanzaste 250 puntos de reputación.",
            iconUrl = "⭐",
            criterion = BadgeCriterion.POINTS,
            threshold = 250
        ),
        Badge(
            id = "badge_4",
            name = "Comunitario",
            description = "Alcanzaste 500 puntos de reputación.",
            iconUrl = "🤝",
            criterion = BadgeCriterion.POINTS,
            threshold = 500
        )
    )
}
