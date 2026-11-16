package com.servilocal.domain.usecase.chat

import com.servilocal.domain.model.NotificationType
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.ChatRepository
import com.servilocal.domain.repository.PostRepository
import com.servilocal.domain.usecase.notifications.CreateNotificationUseCase
import jakarta.inject.Inject

data class StartConversationResult(
    val conversationId: String,
    val isNewChat: Boolean,
    val postTitle: String
)

class StartConversationUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository,
    private val createNotification: CreateNotificationUseCase
) {
    operator fun invoke(postId: String): StartConversationResult? {
        val currentUser = authRepository.currentUser.value
        val post = postRepository.posts.value.firstOrNull { it.id == postId } ?: return null

        val existing = chatRepository.conversations.value.firstOrNull {
            it.postId == postId && (it.clientId == currentUser.id || it.providerId == currentUser.id)
        }

        if (existing != null) {
            chatRepository.startConversation(post, currentUser)
            return StartConversationResult(
                conversationId = existing.id,
                isNewChat = false,
                postTitle = post.title
            )
        }

        val convId = chatRepository.startConversation(post, currentUser) ?: return null
        val providerId = post.providerId ?: "demo-provider"

        if (providerId != currentUser.id) {
            createNotification(
                userId = providerId,
                title = "Nueva conversación",
                body = "${currentUser.name} ha iniciado un chat sobre tu servicio '${post.title}'.",
                type = NotificationType.REQUEST,
                relatedPostId = postId,
                relatedChatId = convId
            )
        }
        return StartConversationResult(
            conversationId = convId,
            isNewChat = true,
            postTitle = post.title
        )
    }
}
