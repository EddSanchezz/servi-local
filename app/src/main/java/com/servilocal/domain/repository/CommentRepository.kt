package com.servilocal.domain.repository

import com.servilocal.domain.model.Comment
import kotlinx.coroutines.flow.StateFlow

interface CommentRepository {
    val comments: StateFlow<List<Comment>>

    fun addComment(postId: String, userId: String, authorName: String, content: String): Boolean
}
