package com.example.data.model

enum class UserRole {
    USER,       // Client & Provider unified role
    MODERATOR   // Community auditor / staff role
}

data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val level: String = "Nivel 3: Destacado",
    val points: Int = 220,
    val nextLevelPoints: Int = 300,
    val badges: List<Badge> = listOf(
        Badge("1", "Primera Publicación", "🥇", true),
        Badge("2", "Influencer", "🔥", true),
        Badge("3", "Súper Proveedor", "⭐", false),
        Badge("4", "Comunitario", "🤝", false)
    ),
    val staffId: String? = null,
    val activeShift: Boolean = true
)

data class Badge(
    val id: String,
    val title: String,
    val icon: String,
    val unlocked: Boolean
)

enum class PostStatus {
    PENDING_VERIFICATION,
    APPROVED,
    REJECTED,
    FINISHED
}

data class ServicePost(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val providerName: String,
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
    val rejectionNotes: String? = null
)

data class ServiceComment(
    val id: String,
    val authorName: String,
    val date: String,
    val text: String,
    val isAuthorReply: Boolean = false,
    val isRecommended: Boolean = true
)

enum class ChatStatus {
    IN_PROGRESS,
    PENDING,
    COMPLETED,
    NEW_REQUEST
}

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
    val isClientRole: Boolean = true, // true = User is client, false = User is provider
    val requestAddress: String? = null,
    val requestProblem: String? = null,
    val requestPhone: String? = null,
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
    val read: Boolean = false
)

data class ModerationReportItem(
    val id: String,
    val reasonTitle: String,
    val reportCount: Int,
    val targetType: String, // "Publicación" o "Chat"
    val targetTitle: String,
    val description: String
)
