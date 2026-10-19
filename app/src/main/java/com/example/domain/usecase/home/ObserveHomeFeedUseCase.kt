package com.example.domain.usecase.home

import com.example.domain.model.HomeFeedData
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.PostRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveHomeFeedUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val postRepository: PostRepository
) {
    operator fun invoke(): Flow<HomeFeedData> = combine(
        authRepository.currentUser,
        postRepository.posts
    ) { user, posts -> HomeFeedData(user, posts) }
}
