package com.servilocal.domain.usecase.posts

import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.LikeRepository
import com.servilocal.domain.repository.PostRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

data class PostLikes(
    val count: Int = 0,
    val likedByCurrentUser: Boolean = false
)

/**
 * Observa cuántos apoyos tiene una publicación y si el usuario actual ya la apoyó.
 * El conteo parte de los apoyos de la semilla y suma los likes registrados en la tabla.
 */
class ObservePostLikesUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val likeRepository: LikeRepository,
    private val authRepository: AuthRepository
) {
    operator fun invoke(postId: String): Flow<PostLikes> = combine(
        postRepository.posts,
        likeRepository.likes,
        authRepository.currentUser
    ) { posts, likes, user ->
        PostLikes(
            count = posts.firstOrNull { it.id == postId }?.upvotesCount ?: 0,
            likedByCurrentUser = likes.any { it.postId == postId && it.userId == user.id }
        )
    }.distinctUntilChanged()
}
