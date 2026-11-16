package com.servilocal.features.home.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestoreException
import com.servilocal.core.util.RequestResult
import com.servilocal.core.util.ResourceProvider
import com.servilocal.R
import com.servilocal.domain.model.ServiceComment
import com.servilocal.domain.model.ServicePost
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.usecase.chat.StartConversationResult
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

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
    private val reportPostUseCase: ReportPostUseCase,
    private val resources: ResourceProvider
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

    fun startConversation(postId: String): StartConversationResult? = startConversationUseCase(postId)

    fun toggleUpvote(postId: String) {
        if (_uiState.value.likeResult is RequestResult.Loading) return
        _uiState.update { it.copy(likeResult = RequestResult.Loading) }
        viewModelScope.launch {
            val result = try {
                when (val outcome = togglePostLikeUseCase(postId)) {
                    is ToggleLikeResult.Success -> null
                    ToggleLikeResult.OwnPost -> RequestResult.Failure(resources.getString(R.string.cannot_like_own_post))
                    ToggleLikeResult.PostNotVerified -> RequestResult.Failure(resources.getString(R.string.verified_posts_only_like))
                    ToggleLikeResult.PostNotFound -> RequestResult.Failure(resources.getString(R.string.post_unavailable))
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: FirebaseFirestoreException) {
                RequestResult.Failure(
                    resources.getString(
                        if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            R.string.like_permission_denied
                        } else {
                            R.string.like_update_failed
                        }
                    )
                )
            } catch (_: Exception) {
                RequestResult.Failure(resources.getString(R.string.like_update_failed))
            }
            _uiState.update { it.copy(likeResult = result) }
        }
    }

    /** Devuelve true si el comentario se publicó, para que la pantalla limpie el campo. */
    suspend fun addComment(postId: String, text: String): Boolean {
        val result = when (addPostCommentUseCase(postId, text)) {
            AddCommentResult.Success -> RequestResult.Success(resources.getString(R.string.comment_success))
            AddCommentResult.EmptyContent -> RequestResult.Failure(resources.getString(R.string.comment_empty))
            AddCommentResult.TooLong -> RequestResult.Failure(
                resources.getString(R.string.comment_too_long, AddPostCommentUseCase.MAX_LENGTH)
            )
            AddCommentResult.PostNotVerified -> RequestResult.Failure(resources.getString(R.string.verified_posts_only_comment))
            AddCommentResult.PostNotFound -> RequestResult.Failure(resources.getString(R.string.post_unavailable))
        }
        _uiState.update { it.copy(commentResult = result) }
        return result is RequestResult.Success
    }

    fun clearInteractionFeedback() {
        _uiState.update { ServiceDetailUiState() }
    }

    suspend fun reportPost(postId: String, reason: String, description: String): Boolean {
        return reportPostUseCase(postId, reason, description)
    }
}
