package com.servilocal.domain.repository

import com.servilocal.domain.model.ModerationReportItem
import com.servilocal.domain.model.ReportResolution
import kotlinx.coroutines.flow.StateFlow

interface ModerationRepository {
    val reports: StateFlow<List<ModerationReportItem>>

    fun createReport(
        reason: String,
        description: String,
        targetTitle: String,
        targetPostId: String
    )
    fun resolveReport(
        reportId: String,
        resolution: ReportResolution,
        moderatorComment: String
    ): Boolean
    fun resolveReportsForPost(postId: String)
}
