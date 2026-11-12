package com.servilocal.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.servilocal.R

@Composable
fun PostPhotoViewer(
    photos: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    if (photos.isEmpty()) return
    var index by remember(photos, initialIndex) {
        mutableIntStateOf(initialIndex.coerceIn(photos.indices))
    }
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(photos, index) {
                    detectHorizontalDragGestures { _, amount ->
                        if (amount > 40f) index = (index - 1 + photos.size) % photos.size
                        if (amount < -40f) index = (index + 1) % photos.size
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = photos[index],
                contentDescription = stringResource(R.string.photo_position, index + 1, photos.size),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth()
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
            ) {
                Icon(Icons.Default.Close, stringResource(R.string.close), tint = Color.White)
            }
            if (photos.size > 1) {
                IconButton(
                    onClick = { index = (index - 1 + photos.size) % photos.size },
                    modifier = Modifier.align(Alignment.CenterStart).padding(8.dp)
                ) {
                    Icon(Icons.Default.ChevronLeft, stringResource(R.string.previous_photo), tint = Color.White)
                }
                IconButton(
                    onClick = { index = (index + 1) % photos.size },
                    modifier = Modifier.align(Alignment.CenterEnd).padding(8.dp)
                ) {
                    Icon(Icons.Default.ChevronRight, stringResource(R.string.next_photo), tint = Color.White)
                }
            }
        }
    }
}
