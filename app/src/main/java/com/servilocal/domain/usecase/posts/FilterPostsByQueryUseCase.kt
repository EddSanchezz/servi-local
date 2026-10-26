package com.servilocal.domain.usecase.posts

import com.servilocal.domain.model.ServicePost
import jakarta.inject.Inject

class FilterPostsByQueryUseCase @Inject constructor() {
    operator fun invoke(posts: List<ServicePost>, query: String): List<ServicePost> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return posts
        return posts.filter { post ->
            post.title.contains(normalizedQuery, ignoreCase = true) ||
                post.description.contains(normalizedQuery, ignoreCase = true) ||
                post.providerName.contains(normalizedQuery, ignoreCase = true) ||
                post.category.contains(normalizedQuery, ignoreCase = true) ||
                post.location.contains(normalizedQuery, ignoreCase = true)
        }
    }
}
