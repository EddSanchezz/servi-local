package com.servilocal.domain.model

/**
 * Modelos DTO / UI para renderizar las pantallas de la aplicación.
 * Mapean y combinan las entidades relacionales de [Models.kt] (ERD).
 */

data class UserProfile(
    val id: Long,
    val name: String,
    val email: String,
    val role: UserRole,
    val level: String = "Nivel 3: Destacado",
    val points: Int = 220,
    val nextLevelPoints: Int = 300,
    val badges: List<BadgeDisplay> = listOf(
        BadgeDisplay("1", "Primera Publicación", "🥇", true),
        BadgeDisplay("2", "Influencer", "🔥", true),
        BadgeDisplay("3", "Súper Proveedor", "⭐", false),
        BadgeDisplay("4", "Comunitario", "🤝", false)
    ),
    val staffId: String? = null,
    val activeShift: Boolean = true,
    val workingHours: String = "Lunes a sábado · 8:00 a. m. - 6:00 p. m."
)

data class BadgeDisplay(
    val id: String,
    val title: String,
    val icon: String,
    val unlocked: Boolean
)

data class ServicePost(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val providerName: String,
    val providerId: Long? = null,
    val providerAvatar: String? = null,
    val providerLevel: String = "Maestro 🏆",
    val minPrice: Int = 25000,
    val maxPrice: Int = 45000,
    val priceUnit: String = "COP",
    val isPriceToAgree: Boolean = false,
    val location: String = "Armenia, Quindío",
    val distance: String = "A 1.2 km",
    val rating: Float = 4.9f,
    val reviewCount: Int = 38,
    val upvotesCount: Int = 148,
    val userUpvoted: Boolean = false,
    val photos: List<String> = emptyList(),
    val status: PostStatus = PostStatus.APPROVED,
    val warrantyMonths: Int = 6,
    val responseTime: String = "< 30 min",
    val comments: List<ServiceComment> = emptyList(),
    val rejectionReason: String? = null,
    val rejectionNotes: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class ServiceComment(
    val id: String,
    val authorName: String,
    val date: String,
    val text: String,
    val isAuthorReply: Boolean = false,
    val isRecommended: Boolean = true
)

typealias ChatStatus = ServiceRequestStatus

data class ChatConversation(
    val id: String,
    val recipientName: String,
    val recipientAvatar: String? = null,
    val serviceTitle: String,
    val servicePrice: String,
    val status: ChatStatus,
    val lastMessage: String,
    val lastTime: String,
    val unreadCount: Int = 0,
    val isClientRole: Boolean = true,
    val requestAddress: String? = null,
    val requestProblem: String? = null,
    val requestPhone: String? = null,
    val requesterName: String? = null,
    val messages: List<ChatMessageItem> = emptyList()
)

data class ChatMessageItem(
    val id: String,
    val senderName: String,
    val text: String,
    val time: String,
    val isFromMe: Boolean,
    val read: Boolean = true
)

enum class NotificationType {
    REQUEST,
    SYSTEM,
    COMMENT,
    ACHIEVEMENT,
    INTERACTION
}

data class NotificationItem(
    val id: String,
    val title: String,
    val description: String,
    val time: String,
    val type: NotificationType,
    val read: Boolean = false,
    val relatedPostId: String? = null,
    val relatedChatId: String? = null
)

data class ModerationReportItem(
    val id: String,
    val reasonTitle: String,
    val reportCount: Int,
    val targetType: String,
    val targetTitle: String,
    val description: String,
    val targetPostId: String? = null,
    val targetChatId: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)
