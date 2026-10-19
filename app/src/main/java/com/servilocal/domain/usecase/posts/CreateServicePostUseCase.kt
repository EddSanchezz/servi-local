package com.servilocal.domain.usecase.posts

import com.servilocal.domain.model.ServicePost
import com.servilocal.domain.repository.PostRepository
import jakarta.inject.Inject

class CreateServicePostUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    operator fun invoke(
        title: String,
        category: String,
        description: String,
        minPrice: Int,
        maxPrice: Int,
        isPriceToAgree: Boolean,
        location: String
    ): ServicePost = postRepository.createPost(
        title,
        category,
        description,
        minPrice,
        maxPrice,
        isPriceToAgree,
        location
    )
}
