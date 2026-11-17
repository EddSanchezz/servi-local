package com.servilocal.domain.usecase.profile

import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.repository.BadgeRepository
import com.servilocal.domain.repository.CommentRepository
import com.servilocal.domain.repository.PostRepository
import com.servilocal.domain.usecase.user.ObserveCurrentUserUseCase
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Combina el usuario autenticado con sus publicaciones e insignias para construir
 * las estadísticas del perfil y del dashboard de gamificación.
 */
class GetProfileStatsUseCase @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val postRepository: PostRepository,
    private val badgeRepository: BadgeRepository,
    private val commentRepository: CommentRepository
) {
    operator fun invoke(): Flow<ProfileStats?> = combine(
        observeCurrentUser(),
        postRepository.posts,
        badgeRepository.userBadges,
        commentRepository.comments
    ) { user, posts, userBadges, comments ->
        if (user == null) {
            null
        } else {
            val userPosts = posts.filter { post ->
                post.providerId == user.id ||
                    (post.providerId == null && post.providerName.equals(user.name, ignoreCase = true))
            }
            val nextLevelPoints = user.level.next?.minPoints
            val userPostIds = userPosts.map { it.id }.toSet()
            val commentIds = (userPosts.flatMap { post ->
                post.comments.filter { it.authorId != user.id }.map { it.id }
            } + comments.filter { comment ->
                (comment.postOwnerId == user.id || comment.postId in userPostIds) && comment.userId != user.id
            }.map { it.id })
                .toSet()
            val progress = nextLevelPoints?.let { next ->
                val levelSpan = (next - user.level.minPoints).coerceAtLeast(1)
                ((user.points - user.level.minPoints).toFloat() / levelSpan).coerceIn(0f, 1f)
            } ?: 1f

            ProfileStats(
                user = user,
                levelLabel = user.level.displayName(),
                nextLevelPoints = nextLevelPoints,
                progress = progress,
                publishedPosts = userPosts.count { it.status == PostStatus.APPROVED },
                pendingPosts = userPosts.count { it.status == PostStatus.PENDING_VERIFICATION },
                totalPosts = userPosts.size,
                badgeCount = userBadges.count { it.userId == user.id },
                likesReceived = userPosts.sumOf { it.upvotesCount.coerceAtLeast(0) },
                commentsReceived = commentIds.size
            )
        }
    }
}
