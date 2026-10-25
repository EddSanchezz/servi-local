package com.servilocal.domain.usecase.posts

import com.servilocal.domain.repository.PostRepository
import jakarta.inject.Inject

class UpdateServicePostUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    operator fun invoke(
        postId: String,
        title: String,
        category: String,
        description: String,
        minPrice: Int,
        maxPrice: Int,
        isPriceToAgree: Boolean,
        location: String
    ): Boolean = postRepository.updatePost(
        postId,
        title,
        category,
        description,
        minPrice,
        maxPrice,
        isPriceToAgree,
        location
    )
}
