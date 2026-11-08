package com.servilocal.features.home.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servilocal.core.util.RequestResult
import com.servilocal.core.util.ResourceProvider
import com.servilocal.R
import com.servilocal.domain.model.ServiceComment
import com.servilocal.domain.model.ServicePost
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.usecase.chat.CreateServiceRequestResult
import com.servilocal.domain.usecase.chat.CreateServiceRequestUseCase
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
    private val createServiceRequestUseCase: CreateServiceRequestUseCase,
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

    fun createServiceRequest(
        postId: String,
        applicantName: String,
        contact: String,
        address: String,
        description: String
    ): CreateServiceRequestResult = createServiceRequestUseCase(
        postId = postId,
        applicantName = applicantName,
        contactMethod = contact,
        address = address,
        description = description
    )

    fun toggleUpvote(postId: String) {
        val result = when (val outcome = togglePostLikeUseCase(postId)) {
            is ToggleLikeResult.Success -> RequestResult.Success(
                resources.getString(if (outcome.liked) R.string.like_success else R.string.like_removed)
            )
            ToggleLikeResult.OwnPost -> RequestResult.Failure(resources.getString(R.string.cannot_like_own_post))
            ToggleLikeResult.PostNotVerified -> RequestResult.Failure(resources.getString(R.string.verified_posts_only_like))
            ToggleLikeResult.PostNotFound -> RequestResult.Failure(resources.getString(R.string.post_unavailable))
        }
        _uiState.update { it.copy(likeResult = result) }
    }

    /** Devuelve true si el comentario se publicó, para que la pantalla limpie el campo. */
    fun addComment(postId: String, text: String): Boolean {
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

    fun reportPost(postId: String, reason: String, description: String): Boolean {
        return reportPostUseCase(postId, reason, description)
    }
}
