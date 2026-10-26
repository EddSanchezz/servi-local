package com.servilocal.domain.usecase.posts

import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.model.ServicePost
import com.servilocal.domain.repository.PostRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Emits only verified publications available in the public feed. */
class ObservePostsUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    operator fun invoke(): Flow<List<ServicePost>> = postRepository.posts.map { posts ->
        posts.filter { it.status == PostStatus.APPROVED }
    }
}
