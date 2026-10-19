package com.servilocal.domain.model

data class HomeFeedData(
    val user: UserProfile,
    val posts: List<ServicePost>
)
