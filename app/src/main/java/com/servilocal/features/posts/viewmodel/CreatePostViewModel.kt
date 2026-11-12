package com.servilocal.features.posts.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestoreException
import com.servilocal.R
import com.servilocal.core.util.ResourceProvider
import com.servilocal.domain.exception.ImageUploadException
import com.servilocal.domain.repository.ImageStorageRepository
import com.servilocal.domain.usecase.posts.CreateServicePostUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreatePostUiState(
    val selectedImageUri: String? = null,
    val uploadedImageUrl: String? = null,
    val uploadedImageSourceUri: String? = null,
    val uploadProgress: Float? = null,
    val isSubmitting: Boolean = false,
    val isCreated: Boolean = false,
    val submissionError: String? = null
)

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val createServicePost: CreateServicePostUseCase,
    private val imageStorageRepository: ImageStorageRepository,
    private val resources: ResourceProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    fun onImageSelected(uri: String) {
        _uiState.update {
            it.copy(
                selectedImageUri = uri,
                uploadedImageUrl = null,
                uploadedImageSourceUri = null,
                submissionError = null,
                isCreated = false
            )
        }
    }

    fun createPost(
        title: String,
        category: String,
        description: String,
        minPrice: Int,
        maxPrice: Int,
        isPriceToAgree: Boolean,
        location: String,
        latitude: Double?,
        longitude: Double?
    ) {
        val state = _uiState.value
        if (state.isSubmitting) return
        if (latitude == null || longitude == null) {
            _uiState.update { it.copy(submissionError = resources.getString(R.string.post_location_not_selected)) }
            return
        }
        val localImageUri = state.selectedImageUri
        if (localImageUri.isNullOrBlank()) {
            _uiState.update { it.copy(submissionError = resources.getString(R.string.image_required_for_post)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, isCreated = false, submissionError = null) }
            try {
                val uploadedUrl = localImageUri.takeIf { it.startsWith("https://", ignoreCase = true) }
                    ?: state.uploadedImageUrl?.takeIf { state.uploadedImageSourceUri == localImageUri }
                    ?: imageStorageRepository.uploadImage(
                        localUri = localImageUri,
                        folder = "servilocal/posts",
                        onProgress = { progress -> _uiState.update { it.copy(uploadProgress = progress) } }
                    ).also { url ->
                        // Retain the uploaded URL so a Firestore retry does not upload the same file again.
                        _uiState.update {
                            it.copy(uploadedImageUrl = url, uploadedImageSourceUri = localImageUri)
                        }
                    }

                val post = createServicePost(
                    title = title,
                    category = category,
                    description = description,
                    minPrice = minPrice,
                    maxPrice = maxPrice,
                    isPriceToAgree = isPriceToAgree,
                    location = location,
                    latitude = latitude,
                    longitude = longitude,
                    photos = listOf(uploadedUrl)
                )
                _uiState.update { it.copy(isSubmitting = false, isCreated = true, uploadProgress = null) }
            } catch (_: ImageUploadException) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        uploadProgress = null,
                        submissionError = resources.getString(R.string.post_image_upload_failed)
                    )
                }
            } catch (error: FirebaseFirestoreException) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        uploadProgress = null,
                        submissionError = if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            resources.getString(R.string.firestore_write_denied)
                        } else {
                            resources.getString(R.string.post_create_network_failed)
                        }
                    )
                }
            } catch (error: IllegalArgumentException) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        uploadProgress = null,
                        submissionError = error.message ?: resources.getString(R.string.create_post_invalid_data)
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        uploadProgress = null,
                        submissionError = resources.getString(R.string.post_create_network_failed)
                    )
                }
            }
        }
    }
}
