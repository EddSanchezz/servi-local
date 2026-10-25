package com.servilocal.domain.repository

import com.servilocal.domain.model.Like
import kotlinx.coroutines.flow.StateFlow

interface LikeRepository {
    val likes: StateFlow<List<Like>>

    /** Agrega o quita el like del usuario. Devuelve true si el post quedó con like. */
    fun toggleLike(postId: String, userId: Long): Boolean
    fun hasLiked(postId: String, userId: Long): Boolean
}
