package com.servilocal.domain.usecase.posts

import com.servilocal.domain.model.ServiceComment
import com.servilocal.domain.repository.PostRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Observa los comentarios de una publicación, del más reciente al más antiguo. */
class ObservePostCommentsUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    operator fun invoke(postId: String): Flow<List<ServiceComment>> =
        postRepository.posts
            .map { posts -> posts.firstOrNull { it.id == postId }?.comments.orEmpty() }
            .distinctUntilChanged()
}
