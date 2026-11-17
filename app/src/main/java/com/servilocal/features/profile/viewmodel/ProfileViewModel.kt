package com.servilocal.features.profile.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servilocal.domain.model.BadgeDisplay
import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.ImageStorageRepository
import com.servilocal.domain.repository.PostRepository
import com.servilocal.domain.usecase.auth.DeleteAccountResult
import com.servilocal.domain.usecase.auth.DeleteAccountUseCase
import com.servilocal.domain.usecase.notifications.ObserveNotificationsUseCase
import com.servilocal.domain.usecase.profile.CheckAndAwardBadgesUseCase
import com.servilocal.domain.usecase.profile.GetProfileStatsUseCase
import com.servilocal.domain.usecase.profile.ObserveUserBadgesUseCase
import com.servilocal.domain.usecase.profile.ProfileStats
import com.servilocal.domain.usecase.profile.AwardPublicationPointsUseCase
import com.servilocal.domain.usecase.posts.DeleteServicePostUseCase
import com.servilocal.domain.usecase.posts.ObserveMyPostsUseCase
import com.servilocal.domain.usecase.posts.ObserveUserLikesUseCase
import com.servilocal.domain.usecase.moderation.ObserveReportHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

data class ProfileUiState(
    val stats: ProfileStats? = null,
    val isLoading: Boolean = true,
    val isUploadingPhoto: Boolean = false,
    val uploadProgress: Float? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val imageStorageRepository: ImageStorageRepository,
    postRepository: PostRepository,
    observeNotifications: ObserveNotificationsUseCase,
    observeUserBadges: ObserveUserBadgesUseCase,
    observeUserLikes: ObserveUserLikesUseCase,
    observeMyPosts: ObserveMyPostsUseCase,
    observeReportHistory: ObserveReportHistoryUseCase,
    private val deleteServicePost: DeleteServicePostUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val getProfileStats: GetProfileStatsUseCase,
    private val checkAndAwardBadges: CheckAndAwardBadgesUseCase,
    private val awardPublicationPoints: AwardPublicationPointsUseCase
) : ViewModel() {

    val currentUser = authRepository.currentUser
    val likedPosts = combine(postRepository.posts, observeUserLikes()) { posts, likedPostIds ->
        posts.filter { it.id in likedPostIds }
            .map { it.copy(userUpvoted = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val myPosts = observeMyPosts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val reportHistory = observeReportHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val notifications = observeNotifications()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val badges: StateFlow<List<BadgeDisplay>> = observeUserBadges()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getProfileStats().collect { stats ->
                _uiState.update { it.copy(stats = stats, isLoading = false) }
            }
        }
        viewModelScope.launch {
            myPosts
                .map { posts ->
                    posts.asSequence()
                        .filter { it.status == PostStatus.APPROVED }
                        .map { it.id }
                        .sorted()
                        .toList()
                }
                .distinctUntilChanged()
                .collect { approvedPostIds -> syncApprovedPublicationPoints(approvedPostIds) }
        }
        refresh()
    }

    private suspend fun syncApprovedPublicationPoints(postIds: List<String>) {
        val userId = currentUser.value.id
        if (userId.isBlank()) return
        postIds.forEach { postId ->
            try {
                awardPublicationPoints(userId, postId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w(TAG, "Could not grant points for approved post $postId", error)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val approvedPostIds = myPosts.value
                    .filter { it.status == PostStatus.APPROVED }
                    .map { it.id }
                syncApprovedPublicationPoints(approvedPostIds)
                checkAndAwardBadges()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                // A denied/offline badge write must not crash profile composition.
                Log.w(TAG, "Could not synchronize user badges", error)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    suspend fun updateCurrentUserProfile(name: String, email: String, photoUrl: String?): Boolean {
        if (name.isBlank() || !email.trim().matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) return false

        val currentProfile = currentUser.value
        val persistentPhotoUrl = when {
            photoUrl.isNullOrBlank() -> null
            photoUrl.startsWith("https://") || photoUrl.startsWith("http://") -> photoUrl
            else -> {
                _uiState.update { it.copy(isUploadingPhoto = true, uploadProgress = 0f) }
                try {
                    imageStorageRepository.uploadImage(
                        localUri = photoUrl,
                        folder = "servilocal/avatars/user_${currentProfile.id}",
                        onProgress = { progress ->
                            _uiState.update { it.copy(uploadProgress = progress) }
                        }
                    )
                } finally {
                    _uiState.update { it.copy(isUploadingPhoto = false, uploadProgress = null) }
                }
            }
        }

        authRepository.updateCurrentUserProfile(name.trim(), email.trim(), persistentPhotoUrl)
        refresh()
        return true
    }

    suspend fun deletePost(postId: String): Boolean = deleteServicePost(postId)

    suspend fun deleteAccount(): Boolean =
        deleteAccountUseCase() == DeleteAccountResult.Success

    private companion object {
        const val TAG = "ProfileViewModel"
    }
}
