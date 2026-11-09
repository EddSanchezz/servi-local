package com.servilocal.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.servilocal.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePickerBottomSheet(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = stringResource(R.string.select_image),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.take_photo)) },
                leadingContent = { Icon(Icons.Outlined.PhotoCamera, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onCameraClick)
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.choose_from_gallery)) },
                leadingContent = { Icon(Icons.Outlined.Image, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onGalleryClick)
            )
        }
    }
}
