package com.servilocal.features.home.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servilocal.core.util.RequestResult
import com.servilocal.domain.model.ServiceComment
import com.servilocal.domain.model.ServicePost
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.usecase.chat.StartConversationUseCase
import com.servilocal.domain.usecase.moderation.ReportPostUseCase
import com.servilocal.domain.usecase.posts.AddCommentResult
import com.servilocal.domain.usecase.posts.AddPostCommentUseCase
import com.servilocal.domain.usecase.posts.GetPostDetailUseCase
import com.servilocal.domain.usecase.posts.ObservePostCommentsUseCase
import com.servilocal.domain.usecase.posts.ObservePostLikesUseCase
import com.servilocal.domain.usecase.posts.PostLikes
import com.servilocal.domain.usecase.posts.ToggleLikeResult
import com.servilocal.domain.usecase.posts.TogglePostLikeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class ServiceDetailUiState(
    val likeResult: RequestResult? = null,
    val commentResult: RequestResult? = null
)

@HiltViewModel
class ServiceDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val startConversationUseCase: StartConversationUseCase,
    private val togglePostLikeUseCase: TogglePostLikeUseCase,
    private val addPostCommentUseCase: AddPostCommentUseCase,
    observePostDetail: GetPostDetailUseCase,
    observePostLikes: ObservePostLikesUseCase,
    observePostComments: ObservePostCommentsUseCase,
    private val reportPostUseCase: ReportPostUseCase
) : ViewModel() {
    private val postId: String = savedStateHandle.get<String>("postId").orEmpty()

    val currentUser = authRepository.currentUser

    val post: StateFlow<ServicePost?> = observePostDetail(postId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val likes: StateFlow<PostLikes> = observePostLikes(postId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PostLikes())
    val comments: StateFlow<List<ServiceComment>> = observePostComments(postId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(ServiceDetailUiState())
    val uiState: StateFlow<ServiceDetailUiState> = _uiState.asStateFlow()

    fun startConversation(postId: String): String? = startConversationUseCase(postId)

    fun toggleUpvote(postId: String) {
        val result = when (val outcome = togglePostLikeUseCase(postId)) {
            is ToggleLikeResult.Success -> RequestResult.Success(
                if (outcome.liked) "Apoyaste esta publicación." else "Quitaste tu apoyo."
            )
            ToggleLikeResult.OwnPost -> RequestResult.Failure("No puedes apoyar tu propia publicación.")
            ToggleLikeResult.PostNotVerified -> RequestResult.Failure("Solo puedes apoyar publicaciones verificadas.")
            ToggleLikeResult.PostNotFound -> RequestResult.Failure("La publicación ya no está disponible.")
        }
        _uiState.update { it.copy(likeResult = result) }
    }

    /** Devuelve true si el comentario se publicó, para que la pantalla limpie el campo. */
    fun addComment(postId: String, text: String): Boolean {
        val result = when (addPostCommentUseCase(postId, text)) {
            AddCommentResult.Success -> RequestResult.Success("Comentario publicado.")
            AddCommentResult.EmptyContent -> RequestResult.Failure("El comentario no puede estar vacío.")
            AddCommentResult.TooLong -> RequestResult.Failure(
                "El comentario no puede superar ${AddPostCommentUseCase.MAX_LENGTH} caracteres."
            )
            AddCommentResult.PostNotVerified -> RequestResult.Failure("Solo puedes comentar publicaciones verificadas.")
            AddCommentResult.PostNotFound -> RequestResult.Failure("La publicación ya no está disponible.")
        }
        _uiState.update { it.copy(commentResult = result) }
        return result is RequestResult.Success
    }

    fun clearInteractionFeedback() {
        _uiState.update { ServiceDetailUiState() }
    }

    fun reportPost(postId: String, reason: String, description: String): Boolean {
        return reportPostUseCase(postId, reason, description)
    }
}
