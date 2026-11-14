package com.servilocal.domain.repository

import com.servilocal.domain.model.ModerationReportItem
import com.servilocal.domain.model.ReportResolution
import kotlinx.coroutines.flow.StateFlow

interface ModerationRepository {
    val reports: StateFlow<List<ModerationReportItem>>
    val reportHistory: StateFlow<List<ModerationReportItem>>

    suspend fun createReport(
        reason: String,
        description: String,
        targetTitle: String,
        targetPostId: String
    )
    suspend fun resolveReport(
        reportId: String,
        resolution: ReportResolution,
        moderatorComment: String,
        newPostStatus: com.servilocal.domain.model.PostStatus? = null,
        blockUser: Boolean = false
    ): Boolean
    suspend fun resolveReportsForPost(postId: String): Boolean
}
