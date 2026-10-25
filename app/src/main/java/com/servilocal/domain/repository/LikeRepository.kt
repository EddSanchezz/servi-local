package com.servilocal.domain.repository

import com.servilocal.domain.model.Like
import kotlinx.coroutines.flow.StateFlow

interface LikeRepository {
    val likes: StateFlow<List<Like>>

    fun toggleLike(postId: String, userId: Long): Boolean
    fun hasLiked(postId: String, userId: Long): Boolean
}
