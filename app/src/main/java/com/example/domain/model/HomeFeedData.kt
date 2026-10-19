package com.example.domain.model

data class HomeFeedData(
    val user: UserProfile,
    val posts: List<ServicePost>
)
