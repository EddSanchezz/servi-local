package com.servilocal.domain.usecase.chat

import com.servilocal.domain.model.NotificationType
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.ChatRepository
import com.servilocal.domain.repository.PostRepository
import com.servilocal.domain.usecase.notifications.CreateNotificationUseCase
import jakarta.inject.Inject

class StartConversationUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository,
    private val createNotification: CreateNotificationUseCase
) {
    operator fun invoke(postId: String): String? {
        val convId = chatRepository.startConversation(postId) ?: return null
        val post = postRepository.posts.value.firstOrNull { it.id == postId }
        val currentUser = authRepository.currentUser.value
        val providerId = post?.providerId ?: 2L

        if (providerId != currentUser.id) {
            createNotification(
                userId = providerId,
                title = "Nueva conversación",
                body = "${currentUser.name} ha iniciado un chat sobre tu servicio '${post?.title ?: "Servicio"}'.",
                type = NotificationType.REQUEST,
                relatedPostId = postId,
                relatedChatId = convId
            )
        }
        return convId
    }
}
