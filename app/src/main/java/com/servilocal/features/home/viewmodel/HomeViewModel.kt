package com.servilocal.features.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException
import com.servilocal.R
import com.servilocal.core.location.LocationProvider
import com.servilocal.core.util.RequestResult
import com.servilocal.core.util.ResourceProvider
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.usecase.notifications.ObserveNotificationsUseCase
import com.servilocal.domain.usecase.posts.FilterPostsByCategoryUseCase
import com.servilocal.domain.usecase.posts.FilterPostsByLocationUseCase
import com.servilocal.domain.usecase.posts.FilterPostsByQueryUseCase
import com.servilocal.domain.usecase.posts.ObservePostsUseCase
import com.servilocal.domain.usecase.posts.ObserveUserLikesUseCase
import com.servilocal.domain.usecase.posts.ToggleLikeResult
import com.servilocal.domain.usecase.posts.TogglePostLikeUseCase
import com.servilocal.domain.usecase.profile.AwardPublicationPointsUseCase
import com.servilocal.domain.model.PostStatus
import com.servilocal.features.posts.coordinator.PublishPostCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

data class HomeUiState(
    val searchQuery: String = "",
    val selectedCategory: String = "Todos",
    val locationQuery: String = "",
    val maxDistanceKm: Double? = null,
    val originLatitude: Double? = null,
    val originLongitude: Double? = null,
    val displayMode: ViewDisplayMode = ViewDisplayMode.LIST,
    val likeResult: RequestResult? = null
)

enum class ViewDisplayMode { LIST, MAP }

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    observeNotifications: ObserveNotificationsUseCase,
    private val togglePostLikeUseCase: TogglePostLikeUseCase,
    observePosts: ObservePostsUseCase,
    observeUserLikes: ObserveUserLikesUseCase,
    private val awardPublicationPoints: AwardPublicationPointsUseCase,
    private val filterPostsByCategory: FilterPostsByCategoryUseCase,
    private val filterPostsByQuery: FilterPostsByQueryUseCase,
    private val filterPostsByLocation: FilterPostsByLocationUseCase,
    private val resources: ResourceProvider,
    private val locationProvider: LocationProvider? = null,
    private val publishPostCoordinator: PublishPostCoordinator? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    val currentUser = authRepository.currentUser
    val posts = observePosts().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )
    private val likedPostIds = observeUserLikes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
    val filteredPosts = combine(posts, _uiState, likedPostIds) { feed, state, likedIds ->
        val withLikeState = feed.map { post -> post.copy(userUpvoted = post.id in likedIds) }
        val byCategory = filterPostsByCategory(withLikeState, state.selectedCategory)
        val byQuery = filterPostsByQuery(byCategory, state.searchQuery)
        filterPostsByLocation(
            byQuery,
            state.locationQuery,
            state.maxDistanceKm,
            state.originLatitude ?: FilterPostsByLocationUseCase.TEST_ORIGIN_LATITUDE,
            state.originLongitude ?: FilterPostsByLocationUseCase.TEST_ORIGIN_LONGITUDE
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )
    val notifications = observeNotifications()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
    val publicationEvents = publishPostCoordinator?.events ?: emptyFlow()
    fun retryPublication() = publishPostCoordinator?.retryLast()

    init {
        viewModelScope.launch {
            combine(posts, currentUser) { feed, user ->
                user.id to feed
                    .filter { it.providerId == user.id && it.status == PostStatus.APPROVED }
                    .map { it.id }
                    .distinct()
                    .sorted()
            }.distinctUntilChanged().collect { (userId, approvedPostIds) ->
                if (userId.isBlank()) return@collect
                approvedPostIds.forEach { postId ->
                    try {
                        awardPublicationPoints(userId, postId)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        Log.w(TAG, "Could not grant points for approved post $postId", error)
                    }
                }
            }
        }
    }

    fun setSearchQuery(value: String) = _uiState.update { it.copy(searchQuery = value) }
    fun setSelectedCategory(value: String) = _uiState.update { it.copy(selectedCategory = value) }
    fun setLocationQuery(value: String) = _uiState.update { it.copy(locationQuery = value) }
    fun setMaxDistanceKm(value: Double?) = _uiState.update { it.copy(maxDistanceKm = value) }
    fun setOriginLocation(latitude: Double, longitude: Double) = _uiState.update {
        it.copy(originLatitude = latitude, originLongitude = longitude)
    }
    fun clearOriginLocation() = _uiState.update { it.copy(originLatitude = null, originLongitude = null) }
    fun setDisplayMode(value: ViewDisplayMode) = _uiState.update { it.copy(displayMode = value) }
    fun refreshLocationInBackground() {
        viewModelScope.launch {
            locationProvider?.getBestAvailableLocation()?.let { location ->
                setOriginLocation(location.latitude, location.longitude)
            }
        }
    }
    fun toggleUpvote(postId: String) {
        if (_uiState.value.likeResult is RequestResult.Loading) return
        _uiState.update { it.copy(likeResult = RequestResult.Loading) }
        viewModelScope.launch {
            val result = try {
                when (val outcome = togglePostLikeUseCase(postId)) {
                    is ToggleLikeResult.Success -> RequestResult.Success(
                        resources.getString(if (outcome.liked) R.string.like_success else R.string.like_removed)
                    )
                    ToggleLikeResult.OwnPost -> RequestResult.Failure(resources.getString(R.string.cannot_like_own_post))
                    ToggleLikeResult.PostNotVerified -> RequestResult.Failure(resources.getString(R.string.verified_posts_only_like))
                    ToggleLikeResult.PostNotFound -> RequestResult.Failure(resources.getString(R.string.post_unavailable))
                    ToggleLikeResult.NetworkError -> RequestResult.Failure(resources.getString(R.string.interaction_network_error))
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

    fun clearLikeFeedback() {
        _uiState.update { it.copy(likeResult = null) }
    }

    private companion object {
        const val TAG = "HomeViewModel"
    }
}
