package com.servilocal.domain.usecase.notifications

import com.servilocal.domain.model.AppNotification
import com.servilocal.domain.model.NotificationType
import com.servilocal.domain.repository.NotificationRepository
import jakarta.inject.Inject
import java.util.UUID

/**
 * Crea una notificación dirigida a un usuario. No la invoca la UI directamente:
 * la usan otros casos de uso internos (solicitudes, comentarios, moderación o insignias).
 */
class CreateNotificationUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke(
        userId: String,
        title: String,
        body: String,
        type: NotificationType = NotificationType.SYSTEM,
        relatedPostId: String? = null,
        relatedChatId: String? = null
    ): AppNotification? {
        if (userId.isBlank() || title.isBlank() || body.isBlank()) return null

        val notification = AppNotification(
            id = "notif_${UUID.randomUUID()}",
            userId = userId,
            title = title.trim(),
            body = body.trim(),
            type = type,
            relatedPostId = relatedPostId,
            relatedChatId = relatedChatId
        )
        notificationRepository.create(notification)
        return notification
    }
}
