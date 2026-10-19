package com.example.domain.usecase.posts

import com.example.domain.model.ServicePost
import com.example.domain.repository.PostRepository
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
