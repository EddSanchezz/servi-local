package com.servilocal.domain.usecase.moderation

import com.servilocal.domain.model.ServicePost
import com.servilocal.domain.repository.PostRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** Observes all publication states for the moderator's detail and metrics screens. */
class ObserveModerationPostsUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    operator fun invoke(): StateFlow<List<ServicePost>> = postRepository.posts
}
