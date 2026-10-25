package com.servilocal.domain.usecase.chat

import com.servilocal.domain.model.NotificationType
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.ChatRepository
import com.servilocal.domain.usecase.notifications.CreateNotificationUseCase
import jakarta.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val createNotification: CreateNotificationUseCase
) {
    operator fun invoke(conversationId: String, text: String) {
        val content = text.trim()
        if (content.isEmpty()) return
        val conversation = chatRepository.conversations.value.firstOrNull { it.id == conversationId }
        val currentUser = authRepository.currentUser.value

        chatRepository.sendMessage(conversationId, content)

        if (conversation != null) {
            val recipientId = if (currentUser.id == conversation.clientId) conversation.providerId else conversation.clientId
            createNotification(
                userId = recipientId,
                title = "Nuevo mensaje de ${currentUser.name}",
                body = content,
                type = NotificationType.REQUEST,
                relatedPostId = conversation.postId,
                relatedChatId = conversationId
            )
        }
    }
}
