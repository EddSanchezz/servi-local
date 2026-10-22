package com.servilocal.domain.usecase.moderation

import com.servilocal.domain.model.PostCategory
import com.servilocal.domain.model.ServicePost
import jakarta.inject.Inject

class FilterPendingPostsByCategoryUseCase @Inject constructor() {
    operator fun invoke(posts: List<ServicePost>, category: String?): List<ServicePost> {
        if (category.isNullOrBlank() || category.equals("Todos", ignoreCase = true) || category.equals("FIFO", ignoreCase = true)) {
            return posts
        }
        val trimmed = category.trim().lowercase()
        return posts.filter { post ->
            post.category.lowercase().contains(trimmed) ||
                    post.title.lowercase().contains(trimmed) ||
                    PostCategory.entries.any { cat ->
                        (cat.name.lowercase().contains(trimmed) || trimmed.contains(cat.name.lowercase())) &&
                                (post.category.lowercase().contains(cat.name.lowercase()) || post.title.lowercase().contains(cat.name.lowercase()))
                    }
        }
    }
}
