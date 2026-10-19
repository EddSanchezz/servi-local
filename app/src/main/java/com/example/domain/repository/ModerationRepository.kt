package com.example.domain.repository

import com.example.domain.model.ModerationReportItem
import kotlinx.coroutines.flow.StateFlow

interface ModerationRepository {
    val reports: StateFlow<List<ModerationReportItem>>

    fun createReport(
        reason: String,
        description: String,
        targetType: String,
        targetTitle: String,
        targetPostId: String? = null,
        targetChatId: String? = null
    )
    fun dismissReport(reportId: String)
    fun resolveReportsForPost(postId: String)
}
