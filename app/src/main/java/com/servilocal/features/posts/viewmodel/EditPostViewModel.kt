package com.servilocal.features.posts.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servilocal.R
import com.servilocal.core.util.ResourceProvider
import com.servilocal.domain.model.ServicePost
import com.servilocal.domain.usecase.posts.DeleteServicePostUseCase
import com.servilocal.domain.usecase.posts.GetPostDetailUseCase
import com.servilocal.domain.usecase.posts.UpdateServicePostUseCase
import com.servilocal.domain.repository.ImageStorageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditPostUiState(
    val submissionError: String? = null,
    val isSubmitting: Boolean = false,
    val isUpdated: Boolean = false,
    val isDeleted: Boolean = false
)

@HiltViewModel
class EditPostViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getPostDetail: GetPostDetailUseCase,
    private val updateServicePost: UpdateServicePostUseCase,
    private val deleteServicePost: DeleteServicePostUseCase,
    private val imageStorageRepository: ImageStorageRepository,
    private val resources: ResourceProvider
) : ViewModel() {
    private val postId: String = savedStateHandle.get<String>("postId").orEmpty()
    val post: StateFlow<ServicePost?> = getPostDetail(postId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val _uiState = MutableStateFlow(EditPostUiState())
    val uiState: StateFlow<EditPostUiState> = _uiState.asStateFlow()

    fun updatePost(
        postId: String,
        title: String,
        category: String,
        description: String,
        minPrice: Int,
        maxPrice: Int,
        isPriceToAgree: Boolean,
        location: String,
        latitude: Double?,
        longitude: Double?,
        photos: List<String>? = null
    ) {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submissionError = null, isUpdated = false) }
            val updated = runCatching {
                val uploadedPhotos = photos?.map { uri ->
                    if (uri.startsWith("https://")) uri
                    else imageStorageRepository.uploadImage(uri, "servilocal/posts")
                }
                updateServicePost(
                    postId,
                    title,
                    category,
                    description,
                    minPrice,
                    maxPrice,
                    isPriceToAgree,
                    location,
                    latitude,
                    longitude,
                    uploadedPhotos
                )
            }.getOrDefault(false)
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    isUpdated = updated,
                    submissionError = if (updated) null else resources.getString(R.string.edit_post_failed)
                )
            }
        }
    }

    fun deletePostPermanently(postId: String) {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submissionError = null, isDeleted = false) }
            val deleted = runCatching { deleteServicePost(postId) }.getOrDefault(false)
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    isDeleted = deleted,
                    submissionError = if (deleted) null else resources.getString(R.string.delete_post_failed)
                )
            }
        }
    }
}
