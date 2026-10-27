package com.servilocal.domain.usecase.moderation

import com.servilocal.domain.model.UserRole
import com.servilocal.domain.repository.AuthRepository
import jakarta.inject.Inject

class ToggleModeratorShiftUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Boolean {
        if (authRepository.currentUser.value.role != UserRole.MODERATOR) return false
        authRepository.toggleModeratorShift()
        return true
    }
}
