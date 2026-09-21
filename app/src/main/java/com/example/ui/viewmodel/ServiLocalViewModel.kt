package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.MockDataRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppDestination {
    AUTH_LOGIN,
    AUTH_REGISTER,
    AUTH_FORGOT_PASSWORD,
    MAIN_SHELL,
    SERVICE_DETAIL,
    CHAT_DETAIL,
    NOTIFICATIONS,
    MODERATION_DETAIL
}

enum class UserTab {
    EXPLORE,
    MESSAGES,
    CREATE,
    PROFILE
}

enum class ModeratorTab {
    FEED,
    REPORTS,
    STATS,
    PROFILE
}

enum class ViewDisplayMode {
    LIST,
    MAP
}

class ServiLocalViewModel(
    private val repository: MockDataRepository = MockDataRepository()
) : ViewModel() {

    // Current app flow destination
    private val _currentDestination = MutableStateFlow(AppDestination.AUTH_LOGIN)
    val currentDestination: StateFlow<AppDestination> = _currentDestination.asStateFlow()

    // Active role: USER or MODERATOR
    private val _activeRole = MutableStateFlow(UserRole.USER)
    val activeRole: StateFlow<UserRole> = _activeRole.asStateFlow()

    // Active bottom navigation tabs
    private val _selectedUserTab = MutableStateFlow(UserTab.EXPLORE)
    val selectedUserTab: StateFlow<UserTab> = _selectedUserTab.asStateFlow()

    private val _selectedModeratorTab = MutableStateFlow(ModeratorTab.FEED)
    val selectedModeratorTab: StateFlow<ModeratorTab> = _selectedModeratorTab.asStateFlow()

    // Selected items for detail screens
    private val _selectedPostId = MutableStateFlow<String?>("pub_1")
    val selectedPostId: StateFlow<String?> = _selectedPostId.asStateFlow()

    private val _selectedChatId = MutableStateFlow<String?>("chat_1")
    val selectedChatId: StateFlow<String?> = _selectedChatId.asStateFlow()

    // Explore screen state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Todos")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _viewDisplayMode = MutableStateFlow(ViewDisplayMode.LIST)
    val viewDisplayMode: StateFlow<ViewDisplayMode> = _viewDisplayMode.asStateFlow()

    // Messages segment: "client" (Contratados) vs "provider" (Solicitudes recibidas)
    private val _chatSegment = MutableStateFlow("client")
    val chatSegment: StateFlow<String> = _chatSegment.asStateFlow()

    // Notifications filter: "all", "unread", "requests", "comments"
    private val _notificationFilter = MutableStateFlow("all")
    val notificationFilter: StateFlow<String> = _notificationFilter.asStateFlow()

    // Moderation queue filter
    private val _moderationFilter = MutableStateFlow("FIFO")
    val moderationFilter: StateFlow<String> = _moderationFilter.asStateFlow()

    // Moderation stats time range
    private val _statsTimeRange = MutableStateFlow("Hoy")
    val statsTimeRange: StateFlow<String> = _statsTimeRange.asStateFlow()

    // Service request sheet visibility
    private val _showRequestSheet = MutableStateFlow(false)
    val showRequestSheet: StateFlow<Boolean> = _showRequestSheet.asStateFlow()

    // Data from repository
    val currentUser = repository.currentUser
    val moderatorUser = repository.moderatorUser
    val posts = repository.posts
    val conversations = repository.conversations
    val notifications = repository.notifications
    val reports = repository.reports

    // Navigation methods
    fun navigateTo(destination: AppDestination) {
        _currentDestination.value = destination
    }

    fun selectUserTab(tab: UserTab) {
        _selectedUserTab.value = tab
        _currentDestination.value = AppDestination.MAIN_SHELL
    }

    fun selectModeratorTab(tab: ModeratorTab) {
        _selectedModeratorTab.value = tab
        _currentDestination.value = AppDestination.MAIN_SHELL
    }

    fun openServiceDetail(postId: String) {
        _selectedPostId.value = postId
        _currentDestination.value = AppDestination.SERVICE_DETAIL
    }

    fun openChatDetail(chatId: String) {
        _selectedChatId.value = chatId
        _currentDestination.value = AppDestination.CHAT_DETAIL
    }

    fun openModerationDetail(postId: String) {
        _selectedPostId.value = postId
        _currentDestination.value = AppDestination.MODERATION_DETAIL
    }

    fun openNotifications() {
        _currentDestination.value = AppDestination.NOTIFICATIONS
    }

    fun showServiceRequestDialog(show: Boolean) {
        _showRequestSheet.value = show
    }

    // Role switching & Auth
    fun login(asRole: UserRole) {
        _activeRole.value = asRole
        _currentDestination.value = AppDestination.MAIN_SHELL
        if (asRole == UserRole.MODERATOR) {
            _selectedModeratorTab.value = ModeratorTab.FEED
        } else {
            _selectedUserTab.value = UserTab.EXPLORE
        }
    }

    fun logout() {
        _currentDestination.value = AppDestination.AUTH_LOGIN
    }

    fun switchRole(role: UserRole) {
        _activeRole.value = role
        if (role == UserRole.MODERATOR) {
            _selectedModeratorTab.value = ModeratorTab.FEED
        } else {
            _selectedUserTab.value = UserTab.EXPLORE
        }
        _currentDestination.value = AppDestination.MAIN_SHELL
    }

    // Filter controls
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(cat: String) {
        _selectedCategory.value = cat
    }

    fun setViewDisplayMode(mode: ViewDisplayMode) {
        _viewDisplayMode.value = mode
    }

    fun setChatSegment(segment: String) {
        _chatSegment.value = segment
    }

    fun setNotificationFilter(filter: String) {
        _notificationFilter.value = filter
    }

    fun setModerationFilter(filter: String) {
        _moderationFilter.value = filter
    }

    fun setStatsTimeRange(range: String) {
        _statsTimeRange.value = range
    }

    // Repository delegations
    fun toggleUpvote(postId: String) {
        repository.toggleUpvote(postId)
    }

    fun addComment(postId: String, text: String) {
        val author = if (_activeRole.value == UserRole.MODERATOR) "Moderador Staff" else currentUser.value.name
        val isReply = author == "Carlos Mendoza" || author == "Carlos Mendoza (Autor)"
        repository.addComment(postId, text, author, isReply)
    }

    fun createPost(
        title: String,
        category: String,
        description: String,
        minPrice: Int,
        maxPrice: Int,
        isPriceToAgree: Boolean,
        location: String
    ) {
        repository.createPost(title, category, description, minPrice, maxPrice, isPriceToAgree, location)
        selectUserTab(UserTab.EXPLORE)
    }

    fun approvePost(postId: String) {
        repository.approvePost(postId)
    }

    fun rejectPost(postId: String, reason: String, notes: String?) {
        repository.rejectPost(postId, reason, notes)
    }

    fun sendMessage(conversationId: String, text: String) {
        repository.sendMessage(conversationId, text)
    }

    fun submitServiceRequest(
        postId: String,
        applicantName: String,
        contactMethod: String,
        address: String,
        description: String
    ): String {
        val chatId = repository.createServiceRequest(postId, applicantName, contactMethod, address, description)
        _showRequestSheet.value = false
        openChatDetail(chatId)
        return chatId
    }

    fun dismissReport(reportId: String) {
        repository.dismissReport(reportId)
    }

    fun markAllNotificationsRead() {
        repository.markAllNotificationsRead()
    }

    fun toggleNotificationRead(id: String) {
        repository.toggleNotificationRead(id)
    }

    fun toggleModeratorShift() {
        repository.toggleModeratorShift()
    }
}
