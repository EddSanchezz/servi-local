package com.servilocal.features.posts.viewmodel

import androidx.lifecycle.ViewModel
import com.servilocal.domain.repository.PostRepository
import com.servilocal.domain.usecase.posts.FinishServicePostUseCase
import com.servilocal.domain.usecase.posts.UpdateServicePostUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EditPostUiState(val submissionError: String? = null)

@HiltViewModel
class EditPostViewModel @Inject constructor(
    postRepository: PostRepository,
    private val updateServicePost: UpdateServicePostUseCase,
    private val finishServicePost: FinishServicePostUseCase
) : ViewModel() {
    val posts = postRepository.posts
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
            EditPostUiState("No se pudo actualizar la publicación. Solo puedes editar tus publicaciones aprobadas.")
        }
        return updated
    }

    fun finishPost(postId: String): Boolean {
        val finished = finishServicePost(postId)
        _uiState.value = if (finished) {
            EditPostUiState()
        } else {
            EditPostUiState("No se pudo finalizar la publicación.")
        }
        return finished
    }
}
