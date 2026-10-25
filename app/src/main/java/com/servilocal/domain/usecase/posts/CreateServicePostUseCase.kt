package com.servilocal.domain.usecase.posts

import com.servilocal.domain.model.NotificationType
import com.servilocal.domain.model.ServicePost
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.PostRepository
import com.servilocal.domain.usecase.notifications.CreateNotificationUseCase
import jakarta.inject.Inject

class CreateServicePostUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository,
    private val createNotification: CreateNotificationUseCase
) {
    operator fun invoke(
        title: String,
        category: String,
        description: String,
        minPrice: Int,
        maxPrice: Int,
        isPriceToAgree: Boolean,
        location: String
    ): ServicePost {
        val post = postRepository.createPost(
            title,
            category,
            description,
            minPrice,
            maxPrice,
            isPriceToAgree,
            location
        )
        val currentUser = authRepository.currentUser.value
        val authorId = post.providerId ?: currentUser.id
        createNotification(
            userId = authorId,
            title = "Publicación enviada",
            body = "Tu publicación '${post.title}' fue creada y se encuentra pendiente de verificación.",
            type = NotificationType.SYSTEM,
            relatedPostId = post.id
        )
        return post
    }
}
