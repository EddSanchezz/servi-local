package com.servilocal.domain.usecase.posts

import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.LikeRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/** Emits the set of posts liked by the signed-in user for feed-card state. */
class ObserveUserLikesUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val likeRepository: LikeRepository
) {
    operator fun invoke(): Flow<Set<String>> = combine(
        authRepository.currentUser,
        likeRepository.likes
    ) { user, likes ->
        likes.asSequence()
            .filter { it.userId == user.id }
            .map { it.postId }
            .toSet()
    }.distinctUntilChanged()
}
