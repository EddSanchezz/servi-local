package com.servilocal.domain.usecase.chat

import com.servilocal.domain.model.NotificationType
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.ChatRepository
import com.servilocal.domain.repository.PostRepository
import com.servilocal.domain.usecase.notifications.CreateNotificationUseCase
import jakarta.inject.Inject

class CreateServiceRequestUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository,
    private val createNotification: CreateNotificationUseCase
) {
    operator fun invoke(
        postId: String,
        applicantName: String,
        contactMethod: String,
        address: String,
        description: String
    ): String {
        val convId = chatRepository.createServiceRequest(postId, applicantName, contactMethod, address, description)
        val post = postRepository.posts.value.firstOrNull { it.id == postId }
        val providerId = post?.providerId ?: 2L
        val currentUser = authRepository.currentUser.value

        if (providerId != currentUser.id) {
            createNotification(
                userId = providerId,
                title = "Nueva solicitud de servicio",
                body = "$applicantName solicitó tu servicio '${post?.title ?: "Servicio"}'.",
                type = NotificationType.REQUEST,
                relatedPostId = postId,
                relatedChatId = convId
            )
        }
        return convId
    }
}
