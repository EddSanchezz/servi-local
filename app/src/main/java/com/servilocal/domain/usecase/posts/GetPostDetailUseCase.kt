package com.servilocal.domain.usecase.posts

import com.servilocal.domain.model.ServicePost
import com.servilocal.domain.repository.PostRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetPostDetailUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    operator fun invoke(postId: String): Flow<ServicePost?> =
        postRepository.posts.map { posts -> posts.firstOrNull { it.id == postId } }
}
