package com.servilocal.features.posts.viewmodel

import androidx.lifecycle.ViewModel
import com.servilocal.domain.usecase.posts.CreateServicePostUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CreatePostUiState(val submissionError: String? = null)

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val createServicePost: CreateServicePostUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    fun createPost(
        title: String, category: String, description: String, minPrice: Int, maxPrice: Int,
        isPriceToAgree: Boolean, location: String
    ): Boolean {
        return try {
            createServicePost(title, category, description, minPrice, maxPrice, isPriceToAgree, location)
            _uiState.value = CreatePostUiState()
            true
        } catch (error: IllegalArgumentException) {
            _uiState.value = CreatePostUiState(error.message ?: "Revisa los datos de la publicación.")
            false
        }
    }
}
