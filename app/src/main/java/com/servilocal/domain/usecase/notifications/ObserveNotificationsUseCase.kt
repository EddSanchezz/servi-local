package com.servilocal.domain.usecase.notifications

import com.servilocal.domain.model.AppNotification
import com.servilocal.domain.repository.NotificationRepository
import com.servilocal.domain.repository.SessionRepository
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Entrega las notificaciones del usuario autenticado, ordenadas de la más reciente
 * a la más antigua. Si no hay sesión, el flujo queda vacío.
 */
class ObserveNotificationsUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val notificationRepository: NotificationRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<AppNotification>> =
        sessionRepository.session.flatMapLatest { session ->
            if (session == null) {
                flowOf(emptyList())
            } else {
                notificationRepository.notifications.map { notifications ->
                    notifications
                        .filter { it.userId == session.userId }
                        .sortedByDescending { it.createdAtMillis }
                }
            }
        }
}
