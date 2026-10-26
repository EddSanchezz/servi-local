package com.servilocal.domain.usecase.posts

import com.servilocal.domain.repository.PostRepository
import jakarta.inject.Inject

class DeleteServicePostUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    operator fun invoke(postId: String): Boolean = postRepository.deletePost(postId)
}
