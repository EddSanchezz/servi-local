package com.servilocal.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.servilocal.domain.model.UserRole
import com.servilocal.domain.repository.ChatRepository
import com.servilocal.domain.repository.ModerationRepository
import com.servilocal.features.app.viewmodel.AppViewModel
import com.servilocal.features.auth.ui.ForgotPasswordScreen
import com.servilocal.features.auth.ui.LoginScreen
import com.servilocal.features.auth.ui.RegisterScreen
import com.servilocal.features.auth.viewmodel.AuthViewModel
import com.servilocal.features.chat.ui.ChatDetailScreen
import com.servilocal.features.chat.ui.MessagesScreen
import com.servilocal.features.chat.viewmodel.ChatDetailViewModel
import com.servilocal.features.chat.viewmodel.MessagesViewModel
import com.servilocal.features.explore.ui.ExploreScreen
import com.servilocal.features.explore.ui.ServiceDetailScreen
import com.servilocal.features.explore.viewmodel.ExploreViewModel
import com.servilocal.features.explore.viewmodel.ServiceDetailViewModel
import com.servilocal.features.home.ui.HomeScreen
import com.servilocal.features.home.viewmodel.HomeViewModel
import com.servilocal.features.moderation.ui.ModerationDetailScreen
import com.servilocal.features.moderation.ui.ModerationFeedScreen
import com.servilocal.features.moderation.ui.ModerationReportsScreen
import com.servilocal.features.moderation.ui.ModerationStatsScreen
import com.servilocal.features.moderation.ui.ModeratorProfileScreen
import com.servilocal.features.moderation.viewmodel.ModerationViewModel
import com.servilocal.features.notifications.ui.NotificationsScreen
import com.servilocal.features.notifications.viewmodel.NotificationsViewModel
import com.servilocal.features.onboarding.ui.OnboardingScreen
import com.servilocal.features.posts.ui.CreatePostScreen
import com.servilocal.features.posts.viewmodel.CreatePostViewModel
import com.servilocal.features.profile.ui.UserProfileScreen
import com.servilocal.features.profile.viewmodel.ProfileViewModel

object AppRoute {
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot-password"
    const val DASHBOARD = "dashboard"
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
    chatRepository: ChatRepository,
    moderationRepository: ModerationRepository,
    appViewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val rootNavController = rememberNavController()
    val appUiState by appViewModel.uiState.collectAsStateWithLifecycle()

    NavHost(
        navController = rootNavController,
        startDestination = AppRoute.ONBOARDING,
        modifier = modifier.fillMaxSize()
    ) {
        composable(AppRoute.ONBOARDING) {
            OnboardingScreen(onGetStarted = {
                rootNavController.navigate(AppRoute.LOGIN) {
                    popUpTo(AppRoute.ONBOARDING) { inclusive = true }
                }
            })
        }
        composable(AppRoute.LOGIN) {
            val viewModel: AuthViewModel = hiltViewModel()
            LoginScreen(
                viewModel = viewModel,
                onRegister = { rootNavController.navigate(AppRoute.REGISTER) },
                onForgotPassword = { rootNavController.navigate(AppRoute.FORGOT_PASSWORD) },
                onAuthenticated = { role ->
                    appViewModel.login(role)
                    rootNavController.navigate(AppRoute.DASHBOARD) {
                        popUpTo(AppRoute.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(AppRoute.REGISTER) {
            val viewModel: AuthViewModel = hiltViewModel()
            RegisterScreen(
                viewModel = viewModel,
                onBack = { rootNavController.navigateUp() },
                onRegistered = {
                    appViewModel.login(UserRole.USER)
                    rootNavController.navigate(AppRoute.DASHBOARD) {
                        popUpTo(AppRoute.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(AppRoute.FORGOT_PASSWORD) {
            ForgotPasswordScreen(onBack = { rootNavController.navigateUp() })
        }
        composable(AppRoute.DASHBOARD) {
            // Recreate the nested graph when the demo role changes so its back stack is reset.
            key(appUiState.activeRole) {
                DashboardNavigation(
                    chatRepository = chatRepository,
                    moderationRepository = moderationRepository,
                    appViewModel = appViewModel,
                    onLogout = {
                        appViewModel.logout()
                        rootNavController.navigate(AppRoute.LOGIN) {
                            popUpTo(AppRoute.DASHBOARD) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DashboardNavigation(
    chatRepository: ChatRepository,
    moderationRepository: ModerationRepository,
    appViewModel: AppViewModel,
    onLogout: () -> Unit
) {
    val appUiState by appViewModel.uiState.collectAsStateWithLifecycle()
    val conversations by chatRepository.conversations.collectAsStateWithLifecycle()
    val reports by moderationRepository.reports.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val isModerator = appUiState.activeRole == UserRole.MODERATOR
    val startDestination = if (isModerator) AppRoute.MODERATION_FEED else AppRoute.HOME
    val tabRoutes = if (isModerator) {
        setOf(AppRoute.MODERATION_FEED, AppRoute.MODERATION_REPORTS, AppRoute.MODERATION_STATS, AppRoute.MODERATOR_PROFILE)
    } else {
        setOf(AppRoute.HOME, AppRoute.EXPLORE, AppRoute.MESSAGES, AppRoute.CREATE_POST, AppRoute.PROFILE)
    }

    fun navigateTab(route: String) {
        navController.navigate(route) {
            popUpTo(startDestination) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentRoute in tabRoutes) {
                val items = if (isModerator) {
                    listOf(
                        BarItem(AppRoute.MODERATION_FEED, "Auditoría", Icons.Filled.Shield, Icons.Outlined.Shield),
                        BarItem(AppRoute.MODERATION_REPORTS, "Reportes", Icons.Filled.ReportProblem, Icons.Outlined.ReportProblem),
                        BarItem(AppRoute.MODERATION_STATS, "Métricas", Icons.Filled.BarChart, Icons.Outlined.BarChart),
                        BarItem(AppRoute.MODERATOR_PROFILE, "Staff", Icons.Filled.ManageAccounts, Icons.Outlined.ManageAccounts)
                    )
                } else {
                    listOf(
                        BarItem(AppRoute.HOME, "Inicio", Icons.Filled.Home, Icons.Outlined.Home),
                        BarItem(AppRoute.EXPLORE, "Explorar", Icons.Filled.Explore, Icons.Outlined.Explore),
                        BarItem(AppRoute.MESSAGES, "Mensajes", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline),
                        BarItem(AppRoute.CREATE_POST, "Publicar", Icons.Filled.AddCircle, Icons.Outlined.AddCircleOutline),
                        BarItem(AppRoute.PROFILE, "Perfil", Icons.Filled.Person, Icons.Outlined.Person)
                    )
                }
                AppBottomBar(
                    currentRoute = currentRoute,
                    unreadChats = if (!isModerator && appUiState.notificationsEnabled) conversations.sumOf { it.unreadCount } else 0,
                    reportCount = if (isModerator) reports.size else 0,
                    items = items,
                    onSelect = ::navigateTab
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) {
            composable(AppRoute.HOME) {
                val viewModel: HomeViewModel = hiltViewModel()
                HomeScreen(viewModel, onExplore = { navigateTab(AppRoute.EXPLORE) }, onOpenDetail = { navController.navigate(AppRoute.serviceDetail(it)) }, onOpenNotifications = { navController.navigate(AppRoute.NOTIFICATIONS) })
            }
            composable(AppRoute.EXPLORE) {
                val viewModel: ExploreViewModel = hiltViewModel()
                ExploreScreen(viewModel, appUiState, onOpenNotifications = { navController.navigate(AppRoute.NOTIFICATIONS) }, onOpenServiceDetail = { navController.navigate(AppRoute.serviceDetail(it)) }, onOpenChat = { navController.navigate(AppRoute.chatDetail(it)) })
            }
            composable(AppRoute.MESSAGES) {
                val viewModel: MessagesViewModel = hiltViewModel()
                MessagesScreen(viewModel, onOpenChat = { navController.navigate(AppRoute.chatDetail(it)) })
            }
            composable(AppRoute.CREATE_POST) {
                val viewModel: CreatePostViewModel = hiltViewModel()
                CreatePostScreen(viewModel, onBack = { navController.navigateUp() }, onOpenExplore = { navigateTab(AppRoute.EXPLORE) }, onOpenProfile = { navigateTab(AppRoute.PROFILE) })
            }
            composable(AppRoute.PROFILE) {
                val viewModel: ProfileViewModel = hiltViewModel()
                UserProfileScreen(
                    viewModel, appUiState,
                    onOpenNotifications = { navController.navigate(AppRoute.NOTIFICATIONS) },
                    onSwitchToModerator = { appViewModel.switchRole(UserRole.MODERATOR) },
                    onLogout = onLogout,
                    onOpenServiceDetail = { navController.navigate(AppRoute.serviceDetail(it)) },
                    onDarkThemeChange = appViewModel::setDarkTheme,
                    onNotificationsChange = appViewModel::setNotificationsEnabled
                )
            }
            composable(AppRoute.MODERATION_FEED) {
                val viewModel: ModerationViewModel = hiltViewModel()
                ModerationFeedScreen(viewModel, onOpenNotifications = { navController.navigate(AppRoute.NOTIFICATIONS) }, onOpenDetail = { navController.navigate(AppRoute.moderationDetail(it)) })
            }
            composable(AppRoute.MODERATION_REPORTS) {
                val viewModel: ModerationViewModel = hiltViewModel()
                ModerationReportsScreen(viewModel, onOpenNotifications = { navController.navigate(AppRoute.NOTIFICATIONS) }, onOpenDetail = { navController.navigate(AppRoute.moderationDetail(it)) }, onOpenChat = { navController.navigate(AppRoute.chatDetail(it)) })
            }
            composable(AppRoute.MODERATION_STATS) {
                val viewModel: ModerationViewModel = hiltViewModel()
                ModerationStatsScreen(viewModel, onOpenNotifications = { navController.navigate(AppRoute.NOTIFICATIONS) })
            }
            composable(AppRoute.MODERATOR_PROFILE) {
                val viewModel: ModerationViewModel = hiltViewModel()
                ModeratorProfileScreen(
                    viewModel, appUiState,
                    onOpenNotifications = { navController.navigate(AppRoute.NOTIFICATIONS) },
                    onNotificationsChange = appViewModel::setNotificationsEnabled,
                    onSwitchToUser = { appViewModel.switchRole(UserRole.USER) },
                    onLogout = onLogout
                )
            }
            composable(AppRoute.NOTIFICATIONS) {
                val viewModel: NotificationsViewModel = hiltViewModel()
                NotificationsScreen(
                    viewModel, appUiState,
                    onBack = { navController.navigateUp() },
                    onOpenChat = { navController.navigate(AppRoute.chatDetail(it)) },
                    onOpenServiceDetail = { navController.navigate(AppRoute.serviceDetail(it)) },
                    onOpenModerationDetail = { navController.navigate(AppRoute.moderationDetail(it)) }
                )
            }
            composable(AppRoute.SERVICE_DETAIL, arguments = listOf(navArgument("postId") { type = NavType.StringType })) { entry ->
                val viewModel: ServiceDetailViewModel = hiltViewModel()
                ServiceDetailScreen(entry.arguments?.getString("postId").orEmpty(), viewModel, onBack = { navController.navigateUp() }, onOpenChat = { navController.navigate(AppRoute.chatDetail(it)) })
            }
            composable(AppRoute.CHAT_DETAIL, arguments = listOf(navArgument("chatId") { type = NavType.StringType })) { entry ->
                val viewModel: ChatDetailViewModel = hiltViewModel()
                ChatDetailScreen(entry.arguments?.getString("chatId").orEmpty(), viewModel, appUiState, onBack = { navController.navigateUp() })
            }
            composable(AppRoute.MODERATION_DETAIL, arguments = listOf(navArgument("postId") { type = NavType.StringType })) { entry ->
                val viewModel: ModerationViewModel = hiltViewModel()
                ModerationDetailScreen(entry.arguments?.getString("postId").orEmpty(), viewModel, onBack = { navController.navigateUp() })
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
