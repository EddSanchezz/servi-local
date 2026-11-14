package com.servilocal.domain.usecase.posts

import com.servilocal.core.util.relativeTime
import com.servilocal.domain.model.ServiceComment
import com.servilocal.domain.repository.CommentRepository
import com.servilocal.domain.repository.PostRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Observa los comentarios de una publicación, del más reciente al más antiguo. */
class ObservePostCommentsUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository
) {
    operator fun invoke(postId: String): Flow<List<ServiceComment>> =
        commentRepository.comments
            .map { comments ->
                val post = postRepository.posts.value.firstOrNull { it.id == postId }
                comments
                    .filter { it.postId == postId }
                    .sortedByDescending { it.createdAtMillis }
                    .map { comment ->
                        ServiceComment(
                            id = comment.id,
                            authorName = comment.authorName.ifBlank { "Usuario" },
                            date = relativeTime(comment.createdAtMillis),
                            text = comment.content,
                            isAuthorReply = post?.providerId != null && comment.userId == post.providerId,
                            authorId = comment.userId
                        )
                    }
            }
            .distinctUntilChanged()
}
