package com.servilocal

import com.servilocal.domain.model.ModerationReportItem
import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.model.ReportStatus
import com.servilocal.domain.model.ServicePost
import com.servilocal.domain.usecase.moderation.GetModerationStatsUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class GetModerationStatsUseCaseTest {

    private val useCase = GetModerationStatsUseCase()

    @Test
    fun `counts period posts by status and includes reviewed reports from history`() {
        val now = 1_000_000_000L
        val day = 24L * 60 * 60 * 1000
        val posts = listOf(
            post("approved", PostStatus.APPROVED, now - day),
            post("rejected", PostStatus.REJECTED, now - 2 * day),
            post("pending", PostStatus.PENDING_VERIFICATION, now - 3 * day),
            post("outside-range", PostStatus.APPROVED, now - 8 * day)
        )
        val reports = listOf(
            report("pending-report", ReportStatus.PENDING, now - day),
            report("reviewed-report", ReportStatus.REVIEWED, now - 2 * day),
            report("old-report", ReportStatus.REVIEWED, now - 8 * day)
        )

        val stats = useCase(posts, reports, timeRange = "Esta semana", nowMillis = now)

        assertEquals(3, stats.newPosts)
        assertEquals(1, stats.approvedPosts)
        assertEquals(1, stats.rejectedPosts)
        assertEquals(2, stats.reportedPosts)
        assertEquals(1, stats.pendingPosts)
    }

    @Test
    fun `moderation decisions use decision date while pending is the live queue`() {
        val now = 1_000_000_000L
        val day = 24L * 60 * 60 * 1000
        val posts = listOf(
            post("approved-old", PostStatus.APPROVED, now - 20 * day).copy(moderatedAtMillis = now - day),
            post("rejected-old", PostStatus.REJECTED, now - 20 * day).copy(moderatedAtMillis = now - 2 * day),
            post("pending-old", PostStatus.PENDING_VERIFICATION, now - 20 * day),
            post("new-pending", PostStatus.PENDING_VERIFICATION, now - day)
        )

        val stats = useCase(posts, emptyList(), timeRange = "Esta semana", nowMillis = now)

        assertEquals(1, stats.newPosts)
        assertEquals(1, stats.approvedPosts)
        assertEquals(1, stats.rejectedPosts)
        assertEquals(2, stats.pendingPosts)
    }

    private fun post(id: String, status: PostStatus, createdAtMillis: Long) = ServicePost(
        id = id,
        title = id,
        category = "Hogar",
        description = "Descripción",
        providerName = "Proveedor",
        status = status,
        createdAtMillis = createdAtMillis
    )

    private fun report(id: String, status: ReportStatus, createdAtMillis: Long) = ModerationReportItem(
        id = id,
        reasonTitle = "Motivo",
        targetTitle = "Publicación",
        description = "Descripción",
        targetPostId = "post",
        status = status,
        createdAtMillis = createdAtMillis
    )
}
