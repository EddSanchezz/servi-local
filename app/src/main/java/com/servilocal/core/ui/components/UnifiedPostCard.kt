package com.servilocal.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.model.ServicePost

sealed interface PostCardHeaderSlot {
    data class HomeOtherUser(
        val likesCount: Int,
        val isLiked: Boolean,
        val onLikeClick: () -> Unit
    ) : PostCardHeaderSlot

    data class HomeOwner(val likesCount: Int) : PostCardHeaderSlot
    data class MyPostsStatus(val status: PostStatus) : PostCardHeaderSlot
    data class ModeratorAuditDate(val createdAtFormatted: String) : PostCardHeaderSlot
    data class ModeratorReportReason(val reportReason: String) : PostCardHeaderSlot
}

@Composable
fun UnifiedPostCard(
    post: ServicePost,
    headerSlot: PostCardHeaderSlot,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProfilePhotoAvatar(post.providerName, post.providerAvatar, Modifier.size(44.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        post.providerName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${localizedDistance(post.distance)} · ${post.location}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                HeaderSlotContent(headerSlot)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PostThumbnail(post)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        post.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            post.description,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                localizedServiceCategory(post.category),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(
                        if (post.isPriceToAgree) stringResource(R.string.price_to_agree)
                        else "$ ${post.minPrice} - $ ${post.maxPrice} ${post.priceUnit}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun PostThumbnail(post: ServicePost) {
    val photo = post.photos.firstOrNull()
    if (photo.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(R.string.no_photos),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    } else {
        AsyncImage(
            model = photo,
            contentDescription = stringResource(R.string.image_gallery),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(12.dp))
        )
    }
}

@Composable
private fun HeaderSlotContent(slot: PostCardHeaderSlot) {
    when (slot) {
        is PostCardHeaderSlot.HomeOtherUser -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(onClick = slot.onLikeClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (slot.isLiked) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = stringResource(R.string.like_post),
                        tint = if (slot.isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    slot.likesCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
        is PostCardHeaderSlot.HomeOwner -> LikeCount(slot.likesCount)
        is PostCardHeaderSlot.MyPostsStatus -> {
            val (label, container, content) = when (slot.status) {
                PostStatus.PENDING_VERIFICATION -> Triple(R.string.pending_review_status, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                PostStatus.APPROVED -> Triple(R.string.published_status, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                PostStatus.REJECTED -> Triple(R.string.rejected_status, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
                PostStatus.FINISHED -> Triple(R.string.finished_status, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HeaderBadge(stringResource(label), container, content)
        }
        is PostCardHeaderSlot.ModeratorAuditDate -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    slot.createdAtFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        is PostCardHeaderSlot.ModeratorReportReason ->
            HeaderBadge(
                slot.reportReason,
                MaterialTheme.colorScheme.errorContainer,
                MaterialTheme.colorScheme.onErrorContainer
            )
    }
}

@Composable
private fun LikeCount(likesCount: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(Icons.Rounded.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
        Text(likesCount.toString(), style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun HeaderBadge(text: String, containerColor: androidx.compose.ui.graphics.Color, contentColor: androidx.compose.ui.graphics.Color) {
    Surface(color = containerColor, shape = RoundedCornerShape(8.dp)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
