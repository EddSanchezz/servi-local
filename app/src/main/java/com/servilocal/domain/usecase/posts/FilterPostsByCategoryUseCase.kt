package com.servilocal.domain.usecase.posts

import com.servilocal.domain.model.ServicePost
import jakarta.inject.Inject

class FilterPostsByCategoryUseCase @Inject constructor() {
    operator fun invoke(posts: List<ServicePost>, category: String): List<ServicePost> =
        if (category.isBlank() || category.equals(ALL_CATEGORIES, ignoreCase = true)) posts
        else posts.filter { it.category.equals(category, ignoreCase = true) }

    companion object {
        const val ALL_CATEGORIES = "Todos"
    }
}
