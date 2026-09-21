package com.example.domain.repository

import com.example.domain.model.Post
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    val posts: Flow<List<Post>>
    suspend fun toggleUpvote(postId: String)
    suspend fun addComment(postId: String, text: String, authorName: String, isAuthorReply: Boolean = false)
    suspend fun createPost(title: String, category: String, description: String, minPrice: Int, maxPrice: Int, isPriceToAgree: Boolean, location: String)
    suspend fun approvePost(postId: String)
    suspend fun rejectPost(postId: String, reason: String, notes: String? = null)
}