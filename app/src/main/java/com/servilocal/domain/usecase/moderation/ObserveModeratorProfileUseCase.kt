package com.servilocal.domain.usecase.moderation

import com.servilocal.domain.model.UserProfile
import com.servilocal.domain.repository.AuthRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.StateFlow

class ObserveModeratorProfileUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): StateFlow<UserProfile> = authRepository.moderatorUser
}
