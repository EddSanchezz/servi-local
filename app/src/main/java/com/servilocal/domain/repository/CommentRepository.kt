package com.servilocal.domain.repository

import com.servilocal.domain.model.Comment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface CommentRepository {
    val comments: StateFlow<List<Comment>>

    fun observeForPost(postId: String): Flow<List<Comment>>

    suspend fun addComment(postId: String, userId: String, authorName: String, content: String): Boolean
}
