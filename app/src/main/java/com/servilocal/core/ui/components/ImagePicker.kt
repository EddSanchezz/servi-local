package com.servilocal.core.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.servilocal.R
import com.servilocal.core.util.hasImageReadPermission
import com.servilocal.core.util.imageReadPermissions
import com.servilocal.core.util.createTempImageUri

/**
 * Reusable camera/gallery flow. The caller receives the selected image URI and owns
 * its preview and persistence. Camera and image-read permissions are requested only
 * when their respective source is chosen.
 */
@Composable
fun ImagePicker(
    onImageSelected: (Uri) -> Unit,
    trigger: @Composable (onClick: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var showPermissionDialog by rememberSaveable { mutableStateOf(false) }
    var showGalleryPermissionDialog by rememberSaveable { mutableStateOf(false) }
    var showCameraError by rememberSaveable { mutableStateOf(false) }
    var tempCameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let(onImageSelected) }

    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (context.hasImageReadPermission()) {
            galleryLauncher.launch("image/*")
        } else {
            showGalleryPermissionDialog = true
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) tempCameraUri?.let(onImageSelected)
        tempCameraUri = null
    }

    val launchCamera = remember(cameraLauncher) {
        { uri: Uri ->
            tempCameraUri = uri
            runCatching { cameraLauncher.launch(uri) }
                .onFailure {
                    tempCameraUri = null
                    showCameraError = true
                }
            Unit
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            runCatching { createTempImageUri(context) }
                .onSuccess(launchCamera)
                .onFailure { showCameraError = true }
        } else {
            showPermissionDialog = true
        }
    }

    trigger { showPicker = true }

    if (showPicker) {
        ImagePickerBottomSheet(
            onCameraClick = {
                showPicker = false
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    runCatching { createTempImageUri(context) }
                        .onSuccess(launchCamera)
                        .onFailure { showCameraError = true }
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onGalleryClick = {
                showPicker = false
                if (context.hasImageReadPermission()) {
                    galleryLauncher.launch("image/*")
                } else {
                    galleryPermissionLauncher.launch(imageReadPermissions())
                }
            },
            onDismiss = { showPicker = false }
        )
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text(stringResource(R.string.camera_permission_title)) },
            text = { Text(stringResource(R.string.camera_permission_required)) },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    val settingsIntent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    context.startActivity(settingsIntent)
                }) {
                    Text(stringResource(R.string.open_app_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showCameraError) {
        AlertDialog(
            onDismissRequest = { showCameraError = false },
            title = { Text(stringResource(R.string.permission_camera)) },
            text = { Text(stringResource(R.string.camera_open_failed)) },
            confirmButton = {
                TextButton(onClick = { showCameraError = false }) {
                    Text(stringResource(R.string.understood))
                }
            }
        )
    }

    if (showGalleryPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showGalleryPermissionDialog = false },
            title = { Text(stringResource(R.string.gallery_permission_title)) },
            text = { Text(stringResource(R.string.gallery_permission_required)) },
            confirmButton = {
                TextButton(onClick = {
                    showGalleryPermissionDialog = false
                    val settingsIntent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    context.startActivity(settingsIntent)
                }) {
                    Text(stringResource(R.string.open_app_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showGalleryPermissionDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
