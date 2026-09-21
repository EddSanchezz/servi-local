package com.example.domain.repository

import com.example.data.model.ModerationReportItem
import kotlinx.coroutines.flow.Flow

interface ModerationRepository {
    val reports: Flow<List<ModerationReportItem>>
    suspend fun approvePost(postId: String)
    suspend fun rejectPost(postId: String, reason: String, notes: String?)
    suspend fun dismissReport(reportId: String)
}