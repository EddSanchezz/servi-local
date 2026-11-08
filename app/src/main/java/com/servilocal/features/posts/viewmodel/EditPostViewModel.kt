package com.servilocal.features.posts.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.servilocal.R
import com.servilocal.core.util.ResourceProvider
import com.servilocal.domain.model.ServicePost
import com.servilocal.domain.usecase.posts.GetPostDetailUseCase
import com.servilocal.domain.usecase.posts.FinishServicePostUseCase
import com.servilocal.domain.usecase.posts.UpdateServicePostUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

data class EditPostUiState(val submissionError: String? = null)

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
        location: String
    ): Boolean {
        val updated = updateServicePost(
            postId,
            title,
            category,
            description,
            minPrice,
            maxPrice,
            isPriceToAgree,
            location
        )
        _uiState.value = if (updated) {
            EditPostUiState()
        } else {
            EditPostUiState(resources.getString(R.string.edit_post_failed))
        }
        return updated
    }

    fun finishPost(postId: String): Boolean {
        val finished = finishServicePost(postId)
        _uiState.value = if (finished) {
            EditPostUiState()
        } else {
            EditPostUiState(resources.getString(R.string.finish_post_failed))
        }
        return finished
    }
}
