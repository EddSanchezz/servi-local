package com.servilocal.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.servilocal.R

@Composable
fun PostPhotoUploader(
    photos: List<String>,
    activeIndex: Int,
    maxPhotos: Int = 5,
    isUploading: Boolean = false,
    onActiveIndexChange: (Int) -> Unit,
    onAddFromGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemovePhoto: (Int) -> Unit
) {
    val safeIndex = if (photos.isEmpty()) 0 else activeIndex.coerceIn(0, photos.lastIndex)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.post_photo_uploader_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                stringResource(R.string.post_photo_uploader_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            if (photos.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(42.dp)
                    )
                    Text(stringResource(R.string.no_selected_photos), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(R.string.photo_picker_help),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        stringResource(R.string.photo_count, 0),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .pointerInput(photos, safeIndex) {
                            detectHorizontalDragGestures { _, dragAmount ->
                                if (dragAmount > 40f) {
                                    onActiveIndexChange((safeIndex - 1 + photos.size) % photos.size)
                                } else if (dragAmount < -40f) {
                                    onActiveIndexChange((safeIndex + 1) % photos.size)
                                }
                            }
                        }
                ) {
                    AsyncImage(
                        model = photos[safeIndex],
                        contentDescription = stringResource(R.string.photo_position, safeIndex + 1, photos.size),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        color = Color.Black.copy(alpha = .55f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text(
                            stringResource(R.string.photo_position, safeIndex + 1, photos.size),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    IconButton(
                        onClick = { onActiveIndexChange((safeIndex - 1 + photos.size) % photos.size) },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(Icons.Default.ChevronLeft, stringResource(R.string.previous_photo), tint = Color.White)
                    }
                    IconButton(
                        onClick = { onActiveIndexChange((safeIndex + 1) % photos.size) },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(Icons.Default.ChevronRight, stringResource(R.string.next_photo), tint = Color.White)
                    }
                    IconButton(
                        onClick = { onRemovePhoto(safeIndex) },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            stringResource(R.string.remove_photo, safeIndex + 1),
                            tint = Color.White
                        )
                    }
                }
            }

            photos.chunked(4).forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEachIndexed { columnIndex, photo ->
                        val index = rowIndex * 4 + columnIndex
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    2.dp,
                                    if (index == safeIndex) MaterialTheme.colorScheme.primary
                                    else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onActiveIndexChange(index) }
                        ) {
                            AsyncImage(
                                model = photo,
                                contentDescription = stringResource(R.string.photo_position, index + 1, photos.size),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { onRemovePhoto(index) },
                                modifier = Modifier.align(Alignment.TopEnd).size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, stringResource(R.string.remove_photo, index + 1), tint = Color.White)
                            }
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }

            if (photos.size < maxPhotos) {
                OutlinedButton(
                    onClick = onAddFromGallery,
                    enabled = !isUploading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, stringResource(R.string.add_more_photos))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.add_more_photos))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.attached_photos), style = MaterialTheme.typography.bodySmall)
                Text(
                    if (photos.size == 1) stringResource(R.string.photo_count_singular, photos.size)
                    else stringResource(R.string.photo_count, photos.size),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onTakePhoto,
                    enabled = !isUploading,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.PhotoCamera, stringResource(R.string.take_photo))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.take_photo), maxLines = 1)
                }
                OutlinedButton(
                    onClick = onAddFromGallery,
                    enabled = !isUploading,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.Image, stringResource(R.string.choose_from_gallery))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.gallery_short_label), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
        }
    }
}
