package com.servilocal.domain.usecase.posts

import com.servilocal.domain.model.ServiceComment
import com.servilocal.domain.repository.CommentRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Observa los comentarios de una publicación, del más reciente al más antiguo. */
class ObservePostCommentsUseCase @Inject constructor(
    private val commentRepository: CommentRepository
) {
    operator fun invoke(postId: String): Flow<List<ServiceComment>> =
        commentRepository.observeForPost(postId)
            .map { comments -> comments.map { it.toServiceComment() } }
            .distinctUntilChanged()

    private fun com.servilocal.domain.model.Comment.toServiceComment(): ServiceComment = ServiceComment(
        id = id,
        authorName = authorName.orEmpty().ifBlank { "Usuario" },
        date = displayDate ?: SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            .format(Date(createdAtMillis)),
        text = content,
        isAuthorReply = postOwnerId != null && postOwnerId == userId,
        authorId = userId,
        createdAtMillis = createdAtMillis
    )
}
