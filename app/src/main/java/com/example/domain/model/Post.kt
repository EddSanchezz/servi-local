package com.example.domain.model

import com.example.data.model.PostStatus

data class Post(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val providerName: String,
    val minPrice: Int,
    val maxPrice: Int,
    val isPriceToAgree: Boolean,
    val location: String,
    val rating: Float,
    val upvotesCount: Int,
    val userUpvoted: Boolean,
    val status: PostStatus = PostStatus.APPROVED
)