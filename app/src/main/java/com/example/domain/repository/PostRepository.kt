package com.example.domain.repository

import com.example.domain.model.ServicePost
import kotlinx.coroutines.flow.StateFlow

interface PostRepository {
    val posts: StateFlow<List<ServicePost>>

    fun toggleUpvote(postId: String)
    fun addComment(postId: String, text: String, authorName: String, isAuthorReply: Boolean = false)
    fun createPost(
        title: String,
        category: String,
        description: String,
        minPrice: Int,
        maxPrice: Int,
        isPriceToAgree: Boolean,
        location: String
    ): ServicePost
    fun approvePost(postId: String)
    fun rejectPost(postId: String, reason: String, notes: String? = null)
}
