package com.servilocal.features.profile.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servilocal.R
import com.servilocal.core.ui.components.ImagePicker
import com.servilocal.core.ui.components.ProfilePhotoAvatar
import com.servilocal.domain.exception.ImageUploadException
import com.servilocal.features.profile.viewmodel.ProfileViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val profileUiState by viewModel.uiState.collectAsStateWithLifecycle()
    var name by rememberSaveable(currentUser.id) { mutableStateOf(currentUser.name) }
    var email by rememberSaveable(currentUser.id) { mutableStateOf(currentUser.email) }
    var photoUrl by rememberSaveable(currentUser.id) { mutableStateOf(currentUser.photoUrl) }
    var showValidation by rememberSaveable { mutableStateOf(false) }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val validationErrorMessage = stringResource(R.string.profile_validation_error)
    val saveErrorMessage = stringResource(R.string.profile_save_error)
    val selectedPhotoMessage = stringResource(R.string.profile_photo_selected_pending_save)
    val photoUploadErrorMessage = stringResource(R.string.profile_photo_upload_error)

    fun saveProfile() {
        if (isSaving) return
        scope.launch {
            isSaving = true
            try {
                if (viewModel.updateCurrentUserProfile(name, email.trim(), photoUrl)) {
                    onBack()
                } else {
                    showValidation = true
                    snackbarHostState.showSnackbar(validationErrorMessage)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: ImageUploadException) {
                snackbarHostState.showSnackbar(photoUploadErrorMessage)
            } catch (_: Exception) {
                snackbarHostState.showSnackbar(saveErrorMessage)
            } finally {
                isSaving = false
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_profile)) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isSaving) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(onClick = ::saveProfile, enabled = !isSaving) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            ImagePicker(
                onImageSelected = {
                    photoUrl = it.toString()
                    scope.launch { snackbarHostState.showSnackbar(selectedPhotoMessage) }
                },
                trigger = { openPicker ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            ProfilePhotoAvatar(
                                name = name,
                                photoUrl = photoUrl,
                                modifier = Modifier.size(124.dp).clickable(onClick = openPicker)
                            )
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                                IconButton(onClick = openPicker) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = stringResource(R.string.edit_photo),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                        TextButton(onClick = openPicker) {
                            Text(stringResource(R.string.change_profile_photo))
                        }
                        if (photoUrl != null) {
                            TextButton(onClick = { photoUrl = null }) {
                                Text(stringResource(R.string.remove_profile_photo))
                            }
                        }
                    }
                }
            )

            if (profileUiState.isUploadingPhoto) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(stringResource(R.string.profile_photo_uploading))
                    LinearProgressIndicator(
                        progress = { profileUiState.uploadProgress ?: 0f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.full_name)) },
                singleLine = true,
                isError = showValidation && name.isBlank(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.email_label)) },
                singleLine = true,
                isError = showValidation && !email.trim().matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
