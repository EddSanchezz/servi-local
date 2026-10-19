package com.example.features.posts.viewmodel

import androidx.lifecycle.ViewModel
import com.example.core.data.repository.MockDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CreatePostUiState(val submissionError: String? = null)

class CreatePostViewModel(private val repository: MockDataRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    fun createPost(
        title: String, category: String, description: String, minPrice: Int, maxPrice: Int,
        isPriceToAgree: Boolean, location: String
    ): Boolean {
        return try {
            repository.createPost(title, category, description, minPrice, maxPrice, isPriceToAgree, location)
            _uiState.value = CreatePostUiState()
            true
        } catch (error: IllegalArgumentException) {
            _uiState.value = CreatePostUiState(error.message ?: "Revisa los datos de la publicación.")
            false
        }
    }
}
