package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UserRole
import com.example.ui.viewmodel.*

@Composable
fun MainAppShell(
    viewModel: ServiLocalViewModel,
    modifier: Modifier = Modifier
) {
    val destination by viewModel.currentDestination.collectAsState()
    val role by viewModel.activeRole.collectAsState()
    val selectedUserTab by viewModel.selectedUserTab.collectAsState()
    val selectedModeratorTab by viewModel.selectedModeratorTab.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val totalUnreadChats = remember(conversations) { conversations.sumOf { it.unreadCount } }

    when (destination) {
        AppDestination.AUTH_LOGIN -> {
            LoginScreen(viewModel = viewModel)
        }
        AppDestination.AUTH_REGISTER -> {
            RegisterScreen(viewModel = viewModel)
        }
        AppDestination.AUTH_FORGOT_PASSWORD -> {
            ForgotPasswordScreen(viewModel = viewModel)
        }
        AppDestination.SERVICE_DETAIL -> {
            ServiceDetailScreen(viewModel = viewModel, modifier = modifier)
        }
        AppDestination.CHAT_DETAIL -> {
            ChatDetailScreen(viewModel = viewModel, modifier = modifier)
        }
        AppDestination.MODERATION_DETAIL -> {
            ModerationDetailScreen(viewModel = viewModel, modifier = modifier)
        }
        AppDestination.NOTIFICATIONS -> {
            NotificationsScreen(viewModel = viewModel, modifier = modifier)
        }
        AppDestination.MAIN_SHELL -> {
            if (role == UserRole.USER) {
                // User Shell with NavigationBar
                Scaffold(
                    modifier = modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp,
                            modifier = Modifier.testTag("user_bottom_navigation")
                        ) {
                            NavigationBarItem(
                                selected = selectedUserTab == UserTab.EXPLORE,
                                onClick = { viewModel.selectUserTab(UserTab.EXPLORE) },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedUserTab == UserTab.EXPLORE) Icons.Filled.Explore else Icons.Outlined.Explore,
                                        contentDescription = "Explorar"
                                    )
                                },
                                label = { Text("Explorar", fontWeight = if (selectedUserTab == UserTab.EXPLORE) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.testTag("tab_explore")
                            )

                            NavigationBarItem(
                                selected = selectedUserTab == UserTab.MESSAGES,
                                onClick = { viewModel.selectUserTab(UserTab.MESSAGES) },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (totalUnreadChats > 0) {
                                                Badge { Text(totalUnreadChats.toString()) }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (selectedUserTab == UserTab.MESSAGES) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                                            contentDescription = "Mensajes"
                                        )
                                    }
                                },
                                label = { Text("Mensajes", fontWeight = if (selectedUserTab == UserTab.MESSAGES) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.testTag("tab_messages")
                            )

                            NavigationBarItem(
                                selected = selectedUserTab == UserTab.CREATE,
                                onClick = { viewModel.selectUserTab(UserTab.CREATE) },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedUserTab == UserTab.CREATE) Icons.Filled.AddCircle else Icons.Outlined.AddCircleOutline,
                                        contentDescription = "Publicar"
                                    )
                                },
                                label = { Text("Publicar", fontWeight = if (selectedUserTab == UserTab.CREATE) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.testTag("tab_create")
                            )

                            NavigationBarItem(
                                selected = selectedUserTab == UserTab.PROFILE,
                                onClick = { viewModel.selectUserTab(UserTab.PROFILE) },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedUserTab == UserTab.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                                        contentDescription = "Perfil"
                                    )
                                },
                                label = { Text("Perfil", fontWeight = if (selectedUserTab == UserTab.PROFILE) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.testTag("tab_profile")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        when (selectedUserTab) {
                            UserTab.EXPLORE -> ExploreScreen(viewModel = viewModel)
                            UserTab.MESSAGES -> MessagesScreen(viewModel = viewModel)
                            UserTab.CREATE -> CreatePostScreen(viewModel = viewModel)
                            UserTab.PROFILE -> UserProfileScreen(viewModel = viewModel)
                        }
                    }
                }
            } else {
                // Moderator Shell with Moderator NavigationBar
                Scaffold(
                    modifier = modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp,
                            modifier = Modifier.testTag("moderator_bottom_navigation")
                        ) {
                            NavigationBarItem(
                                selected = selectedModeratorTab == ModeratorTab.FEED,
                                onClick = { viewModel.selectModeratorTab(ModeratorTab.FEED) },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedModeratorTab == ModeratorTab.FEED) Icons.Filled.Shield else Icons.Outlined.Shield,
                                        contentDescription = "Auditoría"
                                    )
                                },
                                label = { Text("Auditoría", fontWeight = if (selectedModeratorTab == ModeratorTab.FEED) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.testTag("tab_mod_feed")
                            )

                            NavigationBarItem(
                                selected = selectedModeratorTab == ModeratorTab.REPORTS,
                                onClick = { viewModel.selectModeratorTab(ModeratorTab.REPORTS) },
                                icon = {
                                    BadgedBox(badge = { Badge { Text("3") } }) {
                                        Icon(
                                            imageVector = if (selectedModeratorTab == ModeratorTab.REPORTS) Icons.Filled.ReportProblem else Icons.Outlined.ReportProblem,
                                            contentDescription = "Reportes"
                                        )
                                    }
                                },
                                label = { Text("Reportes", fontWeight = if (selectedModeratorTab == ModeratorTab.REPORTS) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.testTag("tab_mod_reports")
                            )

                            NavigationBarItem(
                                selected = selectedModeratorTab == ModeratorTab.STATS,
                                onClick = { viewModel.selectModeratorTab(ModeratorTab.STATS) },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedModeratorTab == ModeratorTab.STATS) Icons.Filled.BarChart else Icons.Outlined.BarChart,
                                        contentDescription = "Métricas"
                                    )
                                },
                                label = { Text("Métricas", fontWeight = if (selectedModeratorTab == ModeratorTab.STATS) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.testTag("tab_mod_stats")
                            )

                            NavigationBarItem(
                                selected = selectedModeratorTab == ModeratorTab.PROFILE,
                                onClick = { viewModel.selectModeratorTab(ModeratorTab.PROFILE) },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedModeratorTab == ModeratorTab.PROFILE) Icons.Filled.ManageAccounts else Icons.Outlined.ManageAccounts,
                                        contentDescription = "Staff"
                                    )
                                },
                                label = { Text("Staff", fontWeight = if (selectedModeratorTab == ModeratorTab.PROFILE) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.testTag("tab_mod_profile")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        when (selectedModeratorTab) {
                            ModeratorTab.FEED -> ModerationFeedScreen(viewModel = viewModel)
                            ModeratorTab.REPORTS -> ModerationReportsScreen(viewModel = viewModel)
                            ModeratorTab.STATS -> ModerationStatsScreen(viewModel = viewModel)
                            ModeratorTab.PROFILE -> ModeratorProfileScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
