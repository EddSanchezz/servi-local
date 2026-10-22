package com.servilocal.domain.usecase.moderation

import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.model.ServicePost
import com.servilocal.domain.repository.PostRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObservePendingPostsUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    operator fun invoke(): Flow<List<ServicePost>> = postRepository.posts.map { posts ->
        posts
            .filter { it.status == PostStatus.PENDING_VERIFICATION }
            .sortedWith(compareBy<ServicePost> { it.createdAtMillis }.thenBy { it.id })
    }
}
