package com.servilocal.domain.usecase.posts

import com.servilocal.domain.repository.PostRepository
import jakarta.inject.Inject

class DeleteServicePostUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(postId: String): Boolean = postRepository.deletePost(postId)
}
