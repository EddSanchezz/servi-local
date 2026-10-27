package com.servilocal.domain.usecase.moderation

import com.servilocal.domain.model.NotificationType
import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.model.ReportResolution
import com.servilocal.domain.model.ReportStatus
import com.servilocal.domain.model.UserRole
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.ModerationRepository
import com.servilocal.domain.repository.PostRepository
import com.servilocal.domain.usecase.notifications.CreateNotificationUseCase
import jakarta.inject.Inject

/**
 * Resuelve un reporte como moderador.
 *
 * - DISMISSED: el reporte no es válido; no se toca el estado de la publicación.
 * - UPHELD: el reporte es procedente; opcionalmente se cambia el estado de la
 *   publicación a REJECTED o FINISHED.
 */
class ResolveReportUseCase @Inject constructor(
    private val moderationRepository: ModerationRepository,
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository,
    private val createNotification: CreateNotificationUseCase
) {
    operator fun invoke(
        reportId: String,
        resolution: ReportResolution,
        moderatorComment: String,
        newPostStatus: PostStatus? = null
    ): Boolean {
        if (authRepository.currentUser.value.role != UserRole.MODERATOR) return false
        if (resolution == ReportResolution.UPHELD && moderatorComment.isBlank()) return false

        val report = moderationRepository.reports.value.firstOrNull { it.id == reportId } ?: return false
        if (report.status != ReportStatus.PENDING) return false

        val statusChange = if (resolution == ReportResolution.UPHELD) newPostStatus else null
        if (statusChange != null && statusChange != PostStatus.REJECTED && statusChange != PostStatus.FINISHED) {
            return false
        }
        val post = postRepository.posts.value.firstOrNull { it.id == report.targetPostId }
        if (statusChange != null && post == null) {
            return false
        }

        if (!moderationRepository.resolveReport(reportId, resolution, moderatorComment.trim())) {
            return false
        }

        if (statusChange != null && post != null) {
            postRepository.moderatePost(
                postId = report.targetPostId,
                status = statusChange,
                reason = if (statusChange == PostStatus.REJECTED) "Reporte: ${report.reasonTitle}" else null,
                notes = moderatorComment.trim()
            )

            val statusLabel = if (statusChange == PostStatus.REJECTED) "rechazada" else "finalizada"
            post.providerId?.let { authorId ->
                createNotification(
                    userId = authorId,
                    title = "Publicación $statusLabel por reporte",
                    body = "Tu publicación '${post.title}' fue $statusLabel tras la revisión de un reporte. Comentario del moderador: ${moderatorComment.trim()}",
                    type = NotificationType.SYSTEM,
                    relatedPostId = post.id
                )
            }
        }

        // Notificar al denunciante
        createNotification(
            userId = report.reporterId,
            title = "Estado de reporte",
            body = "El reporte sobre '${report.targetTitle}' ha sido resuelto por moderación.",
            type = NotificationType.SYSTEM,
            relatedPostId = report.targetPostId
        )

        return true
    }
}
