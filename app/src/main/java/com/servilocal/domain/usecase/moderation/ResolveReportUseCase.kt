package com.servilocal.domain.usecase.moderation

import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.model.ReportResolution
import com.servilocal.domain.model.ReportStatus
import com.servilocal.domain.model.UserRole
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.ModerationRepository
import com.servilocal.domain.repository.PostRepository
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
    private val authRepository: AuthRepository
) {
    operator fun invoke(
        reportId: String,
        resolution: ReportResolution,
        moderatorComment: String,
        newPostStatus: PostStatus? = null
    ): Boolean {
        if (authRepository.moderatorUser.value.role != UserRole.MODERATOR) return false
        if (resolution == ReportResolution.UPHELD && moderatorComment.isBlank()) return false

        val report = moderationRepository.reports.value.firstOrNull { it.id == reportId } ?: return false
        if (report.status != ReportStatus.PENDING) return false

        val statusChange = if (resolution == ReportResolution.UPHELD) newPostStatus else null
        if (statusChange != null && statusChange != PostStatus.REJECTED && statusChange != PostStatus.FINISHED) {
            return false
        }
        if (statusChange != null && postRepository.posts.value.none { it.id == report.targetPostId }) {
            return false
        }

        if (!moderationRepository.resolveReport(reportId, resolution, moderatorComment.trim())) {
            return false
        }

        if (statusChange != null) {
            postRepository.moderatePost(
                postId = report.targetPostId,
                status = statusChange,
                reason = if (statusChange == PostStatus.REJECTED) "Reporte: ${report.reasonTitle}" else null,
                notes = moderatorComment.trim()
            )
        }
        return true
    }
}
