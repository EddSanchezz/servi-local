package com.servilocal.domain.repository

import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.model.ServicePost
import kotlinx.coroutines.flow.StateFlow

interface PostRepository {
    val posts: StateFlow<List<ServicePost>>

    suspend fun createPost(
        title: String,
        category: String,
        description: String,
        minPrice: Int,
        maxPrice: Int,
        isPriceToAgree: Boolean,
        location: String,
        latitude: Double? = null,
        longitude: Double? = null,
        photos: List<String> = emptyList()
    ): ServicePost
    suspend fun updatePost(
        postId: String,
        title: String,
        category: String,
        description: String,
        minPrice: Int,
        maxPrice: Int,
        isPriceToAgree: Boolean,
        location: String,
        latitude: Double? = null,
        longitude: Double? = null,
        photos: List<String>? = null
    ): Boolean
    suspend fun deletePost(postId: String): Boolean
    suspend fun finishPost(postId: String): Boolean
    suspend fun approvePost(postId: String)
    suspend fun rejectPost(postId: String, reason: String, notes: String? = null)
    suspend fun moderatePost(
        postId: String,
        status: PostStatus,
        reason: String? = null,
        notes: String? = null
    ): Boolean
}
