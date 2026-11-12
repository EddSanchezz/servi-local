package com.servilocal.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.servilocal.R
import com.servilocal.core.ui.localizedDistance
import com.servilocal.core.ui.localizedServiceCategory
import com.servilocal.domain.model.ServicePost
import com.servilocal.domain.model.PostStatus

@Composable
fun ServicePostCard(
    post: ServicePost,
    onClick: () -> Unit,
    onUpvote: (() -> Unit)? = null,
    showUpvote: Boolean = false,
    statusBadge: String? = null,
    showProviderAvatar: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (showProviderAvatar) {
                    ProfilePhotoAvatar(post.providerName, post.providerAvatar, Modifier.size(44.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(post.providerName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${localizedDistance(post.distance)} · ${post.location}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (statusBadge != null) {
                    val (containerColor, contentColor) = when (post.status) {
                        PostStatus.APPROVED -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                        PostStatus.PENDING_VERIFICATION -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                        PostStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                        PostStatus.FINISHED -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Surface(color = containerColor, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = statusBadge,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            color = contentColor,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else if (showUpvote && onUpvote != null) {
                    IconButton(onClick = onUpvote) {
                        Icon(
                            if (post.userUpvoted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            stringResource(R.string.like_post)
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (post.photos.firstOrNull().isNullOrBlank()) {
                    Box(
                        Modifier.size(96.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ImageNotSupported, stringResource(R.string.no_photos), tint = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    AsyncImage(
                        model = post.photos.first(),
                        contentDescription = stringResource(R.string.image_gallery),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(96.dp).clip(RoundedCornerShape(12.dp))
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(post.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(localizedServiceCategory(post.category), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text("$ ${post.minPrice} - $ ${post.maxPrice} COP", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
