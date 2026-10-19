package com.example.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.core.data.model.UserRole
import com.example.core.data.repository.MockDataRepository
import com.example.features.app.viewmodel.AppViewModel
import com.example.features.app.viewmodel.AppViewModelFactory
import com.example.features.auth.viewmodel.AuthViewModel
import com.example.features.chat.viewmodel.ChatDetailViewModel
import com.example.features.posts.viewmodel.CreatePostViewModel
import com.example.features.explore.viewmodel.ExploreViewModel
import com.example.features.home.viewmodel.HomeViewModel
import com.example.features.chat.viewmodel.MessagesViewModel
import com.example.features.moderation.viewmodel.ModerationViewModel
import com.example.features.notifications.viewmodel.NotificationsViewModel
import com.example.features.profile.viewmodel.ProfileViewModel
import com.example.features.explore.viewmodel.ServiceDetailViewModel
import com.example.features.auth.ui.ForgotPasswordScreen
import com.example.features.auth.ui.LoginScreen
import com.example.features.auth.ui.RegisterScreen
import com.example.features.chat.ui.ChatDetailScreen
import com.example.features.chat.ui.MessagesScreen
import com.example.features.explore.ui.ExploreScreen
import com.example.features.explore.ui.ServiceDetailScreen
import com.example.features.home.ui.HomeScreen
import com.example.features.moderation.ui.ModerationDetailScreen
import com.example.features.moderation.ui.ModerationFeedScreen
import com.example.features.moderation.ui.ModerationReportsScreen
import com.example.features.moderation.ui.ModerationStatsScreen
import com.example.features.moderation.ui.ModeratorProfileScreen
import com.example.features.notifications.ui.NotificationsScreen
import com.example.features.onboarding.ui.OnboardingScreen
import com.example.features.posts.ui.CreatePostScreen
import com.example.features.profile.ui.UserProfileScreen

object AppRoute {
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot-password"
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val MESSAGES = "messages"
    const val CREATE_POST = "create-post"
    const val PROFILE = "profile"
    const val MODERATION_FEED = "moderation-feed"
    const val MODERATION_REPORTS = "moderation-reports"
    const val MODERATION_STATS = "moderation-stats"
    const val MODERATOR_PROFILE = "moderator-profile"
    const val NOTIFICATIONS = "notifications"
    const val SERVICE_DETAIL = "service-detail/{postId}"
    const val CHAT_DETAIL = "chat-detail/{chatId}"
    const val MODERATION_DETAIL = "moderation-detail/{postId}"
    fun serviceDetail(postId: String) = "service-detail/$postId"
    fun chatDetail(chatId: String) = "chat-detail/$chatId"
    fun moderationDetail(postId: String) = "moderation-detail/$postId"
}

@Composable
fun AppNavHost(
    repository: MockDataRepository,
    appViewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val appUiState by appViewModel.uiState.collectAsState()
    val factory = remember(repository) { AppViewModelFactory(repository) }
    val conversations by repository.conversations.collectAsState()
    val reports by repository.reports.collectAsState()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val userRoutes = setOf(AppRoute.HOME, AppRoute.EXPLORE, AppRoute.MESSAGES, AppRoute.CREATE_POST, AppRoute.PROFILE)
    val moderatorRoutes = setOf(AppRoute.MODERATION_FEED, AppRoute.MODERATION_REPORTS, AppRoute.MODERATION_STATS, AppRoute.MODERATOR_PROFILE)
    val showBottomBar = currentRoute in userRoutes || currentRoute in moderatorRoutes

    fun navigateTab(route: String) {
        navController.navigate(route) {
            popUpTo(if (appUiState.activeRole == UserRole.USER) AppRoute.HOME else AppRoute.MODERATION_FEED) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                if (appUiState.activeRole == UserRole.USER) {
                    AppBottomBar(
                        currentRoute = currentRoute,
                        unreadChats = if (appUiState.notificationsEnabled) conversations.sumOf { it.unreadCount } else 0,
                        reportCount = 0,
                        items = listOf(
                            BarItem(AppRoute.HOME, "Inicio", Icons.Filled.Home, Icons.Outlined.Home),
                            BarItem(AppRoute.EXPLORE, "Explorar", Icons.Filled.Explore, Icons.Outlined.Explore),
                            BarItem(AppRoute.MESSAGES, "Mensajes", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline),
                            BarItem(AppRoute.CREATE_POST, "Publicar", Icons.Filled.AddCircle, Icons.Outlined.AddCircleOutline),
                            BarItem(AppRoute.PROFILE, "Perfil", Icons.Filled.Person, Icons.Outlined.Person)
                        ),
                        onSelect = ::navigateTab
                    )
                } else {
                    AppBottomBar(
                        currentRoute = currentRoute,
                        unreadChats = 0,
                        reportCount = reports.size,
                        items = listOf(
                            BarItem(AppRoute.MODERATION_FEED, "Auditoría", Icons.Filled.Shield, Icons.Outlined.Shield),
                            BarItem(AppRoute.MODERATION_REPORTS, "Reportes", Icons.Filled.ReportProblem, Icons.Outlined.ReportProblem),
                            BarItem(AppRoute.MODERATION_STATS, "Métricas", Icons.Filled.BarChart, Icons.Outlined.BarChart),
                            BarItem(AppRoute.MODERATOR_PROFILE, "Staff", Icons.Filled.ManageAccounts, Icons.Outlined.ManageAccounts)
                        ),
                        onSelect = ::navigateTab
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoute.ONBOARDING,
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) {
            composable(AppRoute.ONBOARDING) {
                OnboardingScreen(onGetStarted = {
                    navController.navigate(AppRoute.LOGIN) { popUpTo(AppRoute.ONBOARDING) { inclusive = true } }
                })
            }
            composable(AppRoute.LOGIN) {
                val vm: AuthViewModel = viewModel(factory = factory)
                LoginScreen(
                    viewModel = vm,
                    onRegister = { navController.navigate(AppRoute.REGISTER) },
                    onForgotPassword = { navController.navigate(AppRoute.FORGOT_PASSWORD) },
                    onAuthenticated = { role ->
                        appViewModel.login(role)
                        val destination = if (role == UserRole.MODERATOR) AppRoute.MODERATION_FEED else AppRoute.HOME
                        navController.navigate(destination) { popUpTo(AppRoute.LOGIN) { inclusive = true } }
                    }
                )
            }
            composable(AppRoute.REGISTER) {
                val vm: AuthViewModel = viewModel(factory = factory)
                RegisterScreen(
                    viewModel = vm,
                    onBack = { navController.navigateUp() },
                    onRegistered = {
                        appViewModel.login(UserRole.USER)
                        navController.navigate(AppRoute.HOME) { popUpTo(AppRoute.LOGIN) { inclusive = true } }
                    }
                )
            }
            composable(AppRoute.FORGOT_PASSWORD) { ForgotPasswordScreen(onBack = { navController.navigateUp() }) }
            composable(AppRoute.HOME) {
                val vm: HomeViewModel = viewModel(factory = factory)
                HomeScreen(vm, onExplore = { navigateTab(AppRoute.EXPLORE) }, onOpenDetail = { navController.navigate(AppRoute.serviceDetail(it)) }, onOpenNotifications = { navController.navigate(AppRoute.NOTIFICATIONS) })
            }
            composable(AppRoute.EXPLORE) {
                val vm: ExploreViewModel = viewModel(factory = factory)
                ExploreScreen(vm, appUiState, onOpenNotifications = { navController.navigate(AppRoute.NOTIFICATIONS) }, onOpenServiceDetail = { navController.navigate(AppRoute.serviceDetail(it)) }, onOpenChat = { navController.navigate(AppRoute.chatDetail(it)) })
            }
            composable(AppRoute.MESSAGES) {
                val vm: MessagesViewModel = viewModel(factory = factory)
                MessagesScreen(vm, onOpenChat = { navController.navigate(AppRoute.chatDetail(it)) })
            }
            composable(AppRoute.CREATE_POST) {
                val vm: CreatePostViewModel = viewModel(factory = factory)
                CreatePostScreen(vm, onBack = { navController.navigateUp() }, onOpenExplore = { navigateTab(AppRoute.EXPLORE) }, onOpenProfile = { navigateTab(AppRoute.PROFILE) })
            }
            composable(AppRoute.PROFILE) {
                val vm: ProfileViewModel = viewModel(factory = factory)
                UserProfileScreen(
                    vm, appUiState,
                    onOpenNotifications = { navController.navigate(AppRoute.NOTIFICATIONS) },
                    onSwitchToModerator = {
                        appViewModel.switchRole(UserRole.MODERATOR)
                        navController.navigate(AppRoute.MODERATION_FEED) { popUpTo(navController.graph.id) { inclusive = true } }
                    },
                    onLogout = {
                        appViewModel.logout()
                        navController.navigate(AppRoute.LOGIN) { popUpTo(navController.graph.id) { inclusive = true } }
                    },
                    onOpenServiceDetail = { navController.navigate(AppRoute.serviceDetail(it)) },
                    onDarkThemeChange = appViewModel::setDarkTheme,
                    onNotificationsChange = appViewModel::setNotificationsEnabled
                )
            }
            composable(AppRoute.MODERATION_FEED) {
                val vm: ModerationViewModel = viewModel(factory = factory)
                ModerationFeedScreen(vm, onOpenNotifications = { navController.navigate(AppRoute.NOTIFICATIONS) }, onOpenDetail = { navController.navigate(AppRoute.moderationDetail(it)) })
            }
            composable(AppRoute.MODERATION_REPORTS) {
                val vm: ModerationViewModel = viewModel(factory = factory)
                ModerationReportsScreen(vm, onOpenNotifications = { navController.navigate(AppRoute.NOTIFICATIONS) }, onOpenDetail = { navController.navigate(AppRoute.moderationDetail(it)) }, onOpenChat = { navController.navigate(AppRoute.chatDetail(it)) })
            }
            composable(AppRoute.MODERATION_STATS) {
                val vm: ModerationViewModel = viewModel(factory = factory)
                ModerationStatsScreen(vm, onOpenNotifications = { navController.navigate(AppRoute.NOTIFICATIONS) })
            }
            composable(AppRoute.MODERATOR_PROFILE) {
                val vm: ModerationViewModel = viewModel(factory = factory)
                ModeratorProfileScreen(
                    vm, appUiState,
                    onOpenNotifications = { navController.navigate(AppRoute.NOTIFICATIONS) },
                    onNotificationsChange = appViewModel::setNotificationsEnabled,
                    onSwitchToUser = {
                        appViewModel.switchRole(UserRole.USER)
                        navController.navigate(AppRoute.HOME) { popUpTo(navController.graph.id) { inclusive = true } }
                    },
                    onLogout = {
                        appViewModel.logout()
                        navController.navigate(AppRoute.LOGIN) { popUpTo(navController.graph.id) { inclusive = true } }
                    }
                )
            }
            composable(AppRoute.NOTIFICATIONS) {
                val vm: NotificationsViewModel = viewModel(factory = factory)
                NotificationsScreen(
                    vm, appUiState,
                    onBack = { navController.navigateUp() },
                    onOpenChat = { navController.navigate(AppRoute.chatDetail(it)) },
                    onOpenServiceDetail = { navController.navigate(AppRoute.serviceDetail(it)) },
                    onOpenModerationDetail = { navController.navigate(AppRoute.moderationDetail(it)) }
                )
            }
            composable(AppRoute.SERVICE_DETAIL, arguments = listOf(navArgument("postId") { type = NavType.StringType })) { entry ->
                val vm: ServiceDetailViewModel = viewModel(factory = factory)
                ServiceDetailScreen(
                    postId = entry.arguments?.getString("postId").orEmpty(), viewModel = vm,
                    onBack = { navController.navigateUp() }, onOpenChat = { navController.navigate(AppRoute.chatDetail(it)) }
                )
            }
            composable(AppRoute.CHAT_DETAIL, arguments = listOf(navArgument("chatId") { type = NavType.StringType })) { entry ->
                val vm: ChatDetailViewModel = viewModel(factory = factory)
                ChatDetailScreen(entry.arguments?.getString("chatId").orEmpty(), vm, appUiState, onBack = { navController.navigateUp() })
            }
            composable(AppRoute.MODERATION_DETAIL, arguments = listOf(navArgument("postId") { type = NavType.StringType })) { entry ->
                val vm: ModerationViewModel = viewModel(factory = factory)
                ModerationDetailScreen(entry.arguments?.getString("postId").orEmpty(), vm, onBack = { navController.navigateUp() })
            }
        }
    }
}

private data class BarItem(val route: String, val label: String, val selectedIcon: ImageVector, val icon: ImageVector)

@Composable
private fun AppBottomBar(
    currentRoute: String?,
    unreadChats: Int,
    reportCount: Int,
    items: List<BarItem>,
    onSelect: (String) -> Unit
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(item.route) },
                icon = {
                    val count = when (item.route) {
                        AppRoute.MESSAGES -> unreadChats
                        AppRoute.MODERATION_REPORTS -> reportCount
                        else -> 0
                    }
                    BadgedBox(badge = { if (count > 0) Badge { Text(count.toString()) } }) {
                        Icon(if (selected) item.selectedIcon else item.icon, contentDescription = item.label)
                    }
                },
                label = { Text(item.label) }
            )
        }
    }
}
