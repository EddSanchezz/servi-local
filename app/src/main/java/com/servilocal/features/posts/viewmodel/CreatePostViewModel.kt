package com.servilocal.features.posts.viewmodel

import androidx.lifecycle.ViewModel
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.servilocal.R
import com.servilocal.core.util.ResourceProvider
import com.servilocal.domain.exception.ImageUploadException
import com.servilocal.domain.repository.ImageStorageRepository
import com.servilocal.domain.usecase.posts.CreateServicePostUseCase
import com.servilocal.features.posts.coordinator.PublishPostCoordinator
import com.servilocal.features.posts.coordinator.PublishPostRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreatePostUiState(
    val selectedImageUri: String? = null,
    val selectedImageUris: List<String> = emptyList(),
    val uploadedImageUrl: String? = null,
    val uploadedImageSourceUri: String? = null,
    val uploadProgress: Float? = null,
    val isSubmitting: Boolean = false,
    val isCreated: Boolean = false,
    val submissionError: String? = null
)

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val publishPostCoordinator: PublishPostCoordinator,
    private val resources: ResourceProvider
) : ViewModel() {
    private var legacyCreateServicePost: CreateServicePostUseCase? = null
    private var legacyImageStorageRepository: ImageStorageRepository? = null

    constructor(
        createServicePost: CreateServicePostUseCase,
        imageStorageRepository: ImageStorageRepository,
        resources: ResourceProvider
    ) : this(
        PublishPostCoordinator(createServicePost, imageStorageRepository, resources),
        resources
    ) {
        legacyCreateServicePost = createServicePost
        legacyImageStorageRepository = imageStorageRepository
    }
    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    fun onImageSelected(uri: String) {
        onImagesSelected(listOf(uri))
    }

    fun onImagesSelected(uris: List<String>) {
        _uiState.update {
            val combined = (it.selectedImageUris + uris)
                .filter { uri -> uri.isNotBlank() }
                .distinct()
                .take(MAX_POST_PHOTOS)
            it.copy(
                selectedImageUri = combined.lastOrNull(),
                selectedImageUris = combined,
                uploadedImageUrl = null,
                uploadedImageSourceUri = null,
                submissionError = null,
                isCreated = false
            )
        }

    }

    fun clearImage() {
        _uiState.update {
            it.copy(
                selectedImageUri = null,
                selectedImageUris = emptyList(),
                uploadedImageUrl = null,
                uploadedImageSourceUri = null,
                submissionError = null
            )
        }

    }

    fun removeImageAt(index: Int) {
        _uiState.update {
            val remaining = it.selectedImageUris.toMutableList().apply {
                if (index in indices) removeAt(index)
            }
            it.copy(
                selectedImageUri = remaining.lastOrNull(),
                selectedImageUris = remaining,
                uploadedImageUrl = null,
                uploadedImageSourceUri = null
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
        val localImageUris = state.selectedImageUris.ifEmpty { listOfNotNull(state.selectedImageUri) }
        if (localImageUris.isEmpty()) {
            _uiState.update { it.copy(submissionError = resources.getString(R.string.image_required_for_post)) }
            return
        }

        if (localImageUris.size > MAX_POST_PHOTOS || localImageUris.any(::isInvalidPhotoUri)) {
            _uiState.update {
                it.copy(submissionError = resources.getString(R.string.invalid_post_images))
            }
            return
        }

        val legacyCreate = legacyCreateServicePost
        val legacyStorage = legacyImageStorageRepository
        if (legacyCreate != null && legacyStorage != null) {
            viewModelScope.launch {
                _uiState.update { it.copy(isSubmitting = true, isCreated = false, submissionError = null) }
                try {
                    val uploadedUrls = localImageUris.mapIndexed { index, uri ->
                        legacyStorage.uploadImage(
                            localUri = uri,
                            folder = "servilocal/posts",
                            onProgress = { progress ->
                                _uiState.update {
                                    it.copy(uploadProgress = (index + progress) / localImageUris.size)
                                }
                            }
                        )
                    }
                    legacyCreate(
                        title, category, description, minPrice, maxPrice, isPriceToAgree,
                        location, latitude, longitude, uploadedUrls
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
                } catch (error: Exception) {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            uploadProgress = null,
                            submissionError = error.message ?: resources.getString(R.string.post_create_network_failed)
                        )
                    }
                }
            }
            return
        }

        publishPostCoordinator.submit(
            PublishPostRequest(
                title = title,
                category = category,
                description = description,
                minPrice = minPrice,
                maxPrice = maxPrice,
                isPriceToAgree = isPriceToAgree,
                location = location,
                latitude = latitude,
                longitude = longitude,
                localPhotoUris = localImageUris
            )
        )
        _uiState.update { it.copy(isSubmitting = false, isCreated = true, submissionError = null) }
    }

    private fun isInvalidPhotoUri(uri: String): Boolean {
        val parsed = Uri.parse(uri)
        return uri.isBlank() || parsed.scheme.isNullOrBlank()
    }

    companion object {
        const val MAX_POST_PHOTOS = 5
    }
}
