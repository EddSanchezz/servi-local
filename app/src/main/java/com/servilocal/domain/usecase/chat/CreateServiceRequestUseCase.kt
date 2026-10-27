package com.servilocal.domain.usecase.chat

import com.servilocal.domain.model.NotificationType
import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.ChatRepository
import com.servilocal.domain.repository.PostRepository
import com.servilocal.domain.usecase.notifications.CreateNotificationUseCase
import jakarta.inject.Inject

sealed interface CreateServiceRequestResult {
    data class Success(val conversationId: String) : CreateServiceRequestResult
    data object PostNotFound : CreateServiceRequestResult
    data object PostNotApproved : CreateServiceRequestResult
    data object OwnPost : CreateServiceRequestResult
    data object MissingApplicantName : CreateServiceRequestResult
    data object MissingContactMethod : CreateServiceRequestResult
    data object MissingAddress : CreateServiceRequestResult
    data object MissingDescription : CreateServiceRequestResult
    data object RequestCouldNotBeCreated : CreateServiceRequestResult
}

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
    ): CreateServiceRequestResult {
        val name = applicantName.trim()
        val contact = contactMethod.trim()
        val exactAddress = address.trim()
        val problem = description.trim()
        if (name.isEmpty()) return CreateServiceRequestResult.MissingApplicantName
        if (contact.isEmpty()) return CreateServiceRequestResult.MissingContactMethod
        if (exactAddress.isEmpty()) return CreateServiceRequestResult.MissingAddress
        if (problem.isEmpty()) return CreateServiceRequestResult.MissingDescription

        val post = postRepository.posts.value.firstOrNull { it.id == postId }
            ?: return CreateServiceRequestResult.PostNotFound
        if (post.status != PostStatus.APPROVED) return CreateServiceRequestResult.PostNotApproved
        val currentUser = authRepository.currentUser.value
        if (post.providerId == currentUser.id || post.providerName.equals(currentUser.name, ignoreCase = true)) {
            return CreateServiceRequestResult.OwnPost
        }
        if (post.providerId == null) return CreateServiceRequestResult.RequestCouldNotBeCreated

        val convId = chatRepository.createServiceRequest(postId, name, contact, exactAddress, problem)
            ?: return CreateServiceRequestResult.RequestCouldNotBeCreated

        post.providerId.let { providerId ->
            createNotification(
                userId = providerId,
                title = "Nueva solicitud de servicio",
                body = "$name solicitó tu servicio '${post.title}'.",
                type = NotificationType.REQUEST,
                relatedPostId = postId,
                relatedChatId = convId
            )
        }
        return CreateServiceRequestResult.Success(convId)
    }
}
