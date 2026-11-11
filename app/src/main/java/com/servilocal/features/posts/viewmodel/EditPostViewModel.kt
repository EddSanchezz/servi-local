package com.servilocal.features.posts.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servilocal.R
import com.servilocal.core.util.ResourceProvider
import com.servilocal.domain.model.ServicePost
import com.servilocal.domain.usecase.posts.FinishServicePostUseCase
import com.servilocal.domain.usecase.posts.GetPostDetailUseCase
import com.servilocal.domain.usecase.posts.UpdateServicePostUseCase
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
    val isFinished: Boolean = false
)

@HiltViewModel
class EditPostViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getPostDetail: GetPostDetailUseCase,
    private val updateServicePost: UpdateServicePostUseCase,
    private val finishServicePost: FinishServicePostUseCase,
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
        longitude: Double?
    ) {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submissionError = null, isUpdated = false) }
            val updated = runCatching {
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
                    null
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

    fun finishPost(postId: String) {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submissionError = null, isFinished = false) }
            val finished = runCatching { finishServicePost(postId) }.getOrDefault(false)
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    isFinished = finished,
                    submissionError = if (finished) null else resources.getString(R.string.finish_post_failed)
                )
            }
        }
    }
}
