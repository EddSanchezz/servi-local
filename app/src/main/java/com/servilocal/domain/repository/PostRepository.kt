package com.servilocal.domain.repository

import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.model.ServicePost
import kotlinx.coroutines.flow.StateFlow

interface PostRepository {
    val posts: StateFlow<List<ServicePost>>

    fun createPost(
        title: String,
        category: String,
        description: String,
        minPrice: Int,
        maxPrice: Int,
        isPriceToAgree: Boolean,
        location: String
    ): ServicePost
    fun updatePost(
        postId: String,
        title: String,
        category: String,
        description: String,
        minPrice: Int,
        maxPrice: Int,
        isPriceToAgree: Boolean,
        location: String
    ): Boolean
    fun finishPost(postId: String): Boolean
    fun approvePost(postId: String)
    fun rejectPost(postId: String, reason: String, notes: String? = null)
    fun moderatePost(
        postId: String,
        status: PostStatus,
        reason: String? = null,
        notes: String? = null
    ): Boolean
}
