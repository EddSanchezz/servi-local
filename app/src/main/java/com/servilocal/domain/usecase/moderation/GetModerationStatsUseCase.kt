package com.servilocal.domain.usecase.moderation

import com.servilocal.domain.model.ModerationReportItem
import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.model.ServicePost
import jakarta.inject.Inject
import java.util.Calendar

data class ModerationStats(
    val newPosts: Int = 0,
    val approvedPosts: Int = 0,
    val rejectedPosts: Int = 0,
    val reportedPosts: Int = 0,
    val pendingPosts: Int = 0
)

/** Builds the moderator dashboard metrics from the live post and report streams. */
class GetModerationStatsUseCase @Inject constructor() {

    operator fun invoke(
        posts: List<ServicePost>,
        reportHistory: List<ModerationReportItem>,
        timeRange: String,
        nowMillis: Long = System.currentTimeMillis()
    ): ModerationStats {
        val rangeStart = when (timeRange) {
            "Hoy" -> Calendar.getInstance().apply {
                timeInMillis = nowMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            "Esta semana" -> nowMillis - 7L * 24 * 60 * 60 * 1000
            else -> nowMillis - 30L * 24 * 60 * 60 * 1000
        }

        val rangePosts = posts.filter { it.createdAtMillis >= rangeStart }
        val rangeReports = reportHistory.count { it.createdAtMillis >= rangeStart }
        fun ServicePost.wasModeratedInRange(): Boolean {
            val moderationTime = moderatedAtMillis.takeIf { it > 0L } ?: createdAtMillis
            return moderationTime >= rangeStart
        }

        return ModerationStats(
            newPosts = rangePosts.size,
            approvedPosts = posts.count { it.status == PostStatus.APPROVED && it.wasModeratedInRange() },
            rejectedPosts = posts.count { it.status == PostStatus.REJECTED && it.wasModeratedInRange() },
            reportedPosts = rangeReports,
            pendingPosts = posts.count { it.status == PostStatus.PENDING_VERIFICATION }
        )
    }
}
