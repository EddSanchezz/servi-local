package com.servilocal.domain.usecase.notifications

import com.servilocal.domain.repository.NotificationRepository
import com.servilocal.domain.repository.SessionRepository
import jakarta.inject.Inject

/**
 * Marca como leída una notificación del usuario en sesión. Si no se indica un id,
 * marca todas las notificaciones del usuario como leídas.
 */
class MarkNotificationReadUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke(notificationId: String? = null, isRead: Boolean = true): Boolean {
        val session = sessionRepository.session.value ?: return false
        return if (notificationId == null) {
            notificationRepository.markAllRead(session.userId)
            true
        } else {
            notificationRepository.markRead(notificationId, session.userId, isRead)
        }
    }
}
