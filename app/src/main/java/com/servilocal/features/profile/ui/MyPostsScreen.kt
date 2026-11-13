package com.servilocal.features.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.servilocal.R
import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.model.ServicePost
import com.servilocal.domain.model.ModerationReportItem
import com.servilocal.core.ui.components.PostCardHeaderSlot
import com.servilocal.core.ui.components.UnifiedPostCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPostsScreen(
    posts: List<ServicePost>,
    reportHistory: List<ModerationReportItem> = emptyList(),
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit,
    onOpenReportCase: (String) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_publications)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        if (posts.isEmpty()) {
            EmptyMyPosts(Modifier.fillMaxSize().padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(posts, key = ServicePost::id) { post ->
                    androidx.compose.foundation.layout.Column {
                        UnifiedPostCard(
                            post = post,
                            onClick = { onOpenDetail(post.id) },
                            headerSlot = PostCardHeaderSlot.MyPostsStatus(post.status)
                        )
                        if (post.status == PostStatus.REJECTED) {
                            val caseReports = reportHistory.filter { it.targetPostId == post.id }
                            val wasReported = post.reportCount > 0 || caseReports.isNotEmpty() || post.rejectionReason.orEmpty().startsWith("Reporte:", true)
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                            ) {
                                androidx.compose.foundation.layout.Column(Modifier.padding(12.dp)) {
                                    Text(
                                        if (wasReported) stringResource(R.string.my_post_reported_reason, maxOf(post.reportCount, caseReports.size))
                                        else stringResource(R.string.my_post_rejection_reason, post.rejectionReason ?: stringResource(R.string.rejected_status)),
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    post.rejectionNotes?.takeIf(String::isNotBlank)?.let {
                                        Text(it, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                                    }
                                    caseReports.firstOrNull()?.let { report ->
                                        TextButton(onClick = { onOpenReportCase(report.id) }) {
                                            Text(stringResource(R.string.my_post_view_report_case))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyMyPosts(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(modifier = modifier.padding(24.dp)) {
        Text(stringResource(R.string.no_posts_yet), modifier = Modifier.fillMaxWidth())
    }
}
