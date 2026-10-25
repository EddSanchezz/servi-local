package com.servilocal.domain.usecase.chat

import com.servilocal.domain.model.ChatStatus
import com.servilocal.domain.model.NotificationType
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.ChatRepository
import com.servilocal.domain.usecase.notifications.CreateNotificationUseCase
import jakarta.inject.Inject

class UpdateConversationStatusUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val createNotification: CreateNotificationUseCase
) {
    operator fun invoke(conversationId: String, status: ChatStatus) {
        val conversation = chatRepository.conversations.value.firstOrNull { it.id == conversationId }
        val currentUser = authRepository.currentUser.value

        chatRepository.updateConversationStatus(conversationId, status)

        if (conversation != null) {
            val recipientId = if (currentUser.id == conversation.clientId) conversation.providerId else conversation.clientId
            val statusLabel = when (status) {
                com.servilocal.domain.model.ServiceRequestStatus.IN_PROGRESS -> "aceptada"
                com.servilocal.domain.model.ServiceRequestStatus.COMPLETED -> "completada"
                com.servilocal.domain.model.ServiceRequestStatus.DECLINED -> "rechazada"
                else -> "actualizada"
            }
            createNotification(
                userId = recipientId,
                title = "Solicitud de servicio $statusLabel",
                body = "La solicitud para '${conversation.serviceTitle}' ha sido $statusLabel.",
                type = NotificationType.REQUEST,
                relatedPostId = conversation.postId,
                relatedChatId = conversationId
            )
        }
    }
}
