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
import com.servilocal.core.ui.components.ServicePostCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPostsScreen(
    posts: List<ServicePost>,
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit
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
                        val statusLabel = when (post.status) {
                            PostStatus.APPROVED -> R.string.published_status
                            PostStatus.PENDING_VERIFICATION -> R.string.pending_review_status
                            PostStatus.REJECTED -> R.string.rejected_status
                            PostStatus.FINISHED -> R.string.finished_status
                        }
                        ServicePostCard(
                            post = post,
                            onClick = { onOpenDetail(post.id) },
                            statusBadge = androidx.compose.ui.res.stringResource(statusLabel)
                        )
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
