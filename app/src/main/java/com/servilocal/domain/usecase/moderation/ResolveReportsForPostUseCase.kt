package com.servilocal.domain.usecase.moderation

import com.servilocal.domain.model.UserRole
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.ModerationRepository
import jakarta.inject.Inject

class ResolveReportsForPostUseCase @Inject constructor(
    private val moderationRepository: ModerationRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(postId: String): Boolean {
        if (authRepository.currentUser.value.role != UserRole.MODERATOR) return false
        return moderationRepository.resolveReportsForPost(postId)
    }
}
