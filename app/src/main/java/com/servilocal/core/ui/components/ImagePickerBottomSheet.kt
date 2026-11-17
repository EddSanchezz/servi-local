package com.servilocal.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.servilocal.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePickerBottomSheet(
    title: String,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onRemoveClick: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.take_photo)) },
                leadingContent = { Icon(Icons.Rounded.PhotoCamera, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onCameraClick)
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.choose_from_gallery)) },
                leadingContent = { Icon(Icons.Rounded.PhotoLibrary, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onGalleryClick)
            )
            if (onRemoveClick != null) {
                ListItem(
                    headlineContent = {
                        Text(stringResource(R.string.remove_profile_photo), color = MaterialTheme.colorScheme.error)
                    },
                    leadingContent = {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.clickable(onClick = onRemoveClick)
                )
            }
        }
    }
}
