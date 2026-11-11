package com.servilocal.domain.usecase.posts

import jakarta.inject.Inject

class FinishServicePostUseCase @Inject constructor(
    private val markPostAsResolved: MarkPostAsResolvedUseCase
) {
    suspend operator fun invoke(postId: String): Boolean = markPostAsResolved(postId)
}
