package com.servilocal.domain.usecase.profile

import com.servilocal.domain.model.User
import com.servilocal.domain.model.UserLevel
import com.servilocal.domain.repository.UserRepository
import jakarta.inject.Inject

/**
 * Suma (o resta) puntos de reputación y recalcula el nivel del usuario.
 * Es un caso de uso interno: lo invocan las acciones que generan reputación,
 * no la UI directamente.
 */
class AddPointsUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: String, points: Int): User? {
        val user = userRepository.findById(userId) ?: return null
        val updatedPoints = (user.points + points).coerceAtLeast(0)
        val updatedUser = user.copy(
            points = updatedPoints,
            level = UserLevel.fromPoints(updatedPoints)
        )
        userRepository.save(updatedUser)
        return updatedUser
    }
}
