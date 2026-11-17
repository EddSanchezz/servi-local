package com.servilocal.domain.usecase.posts

import com.servilocal.domain.model.ServicePost
import com.servilocal.domain.repository.PostRepository
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class GetPostDetailUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(postId: String): Flow<ServicePost?> =
        postRepository.posts.flatMapLatest { posts ->
            val post = posts.firstOrNull { it.id == postId }
            val providerId = post?.providerId
            if (post == null || providerId.isNullOrBlank()) {
                flowOf(post)
            } else {
                postRepository.observeProviderLevel(providerId).map { currentLevel ->
                    currentLevel?.let { post.copy(providerLevel = it) } ?: post
                }
            }
        }
}
