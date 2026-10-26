package com.servilocal.domain.usecase.posts

import com.servilocal.domain.model.ServicePost
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.PostRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveMyPostsUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<List<ServicePost>> = combine(
        postRepository.posts,
        authRepository.currentUser
    ) { posts, user -> posts.filter { it.providerId == user.id } }
}
