package com.servilocal.features.profile.ui

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.servilocal.core.ui.components.ProfileNavRow
import com.servilocal.R
import com.servilocal.features.app.viewmodel.AppUiState
import com.servilocal.features.profile.viewmodel.ProfileViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    viewModel: ProfileViewModel,
    appUiState: AppUiState,
    onOpenNotifications: () -> Unit,
    onLogout: () -> Unit,
    onOpenServiceDetail: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val myPosts by viewModel.myPosts.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val profileUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val badges by viewModel.badges.collectAsStateWithLifecycle()
    val stats = profileUiState.stats
    val notificationsEnabled = appUiState.notificationsEnabled
    var activeDialog by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var postPendingDeletion by remember { mutableStateOf<com.servilocal.domain.model.ServicePost?>(null) }
    var deletePostFailed by remember { mutableStateOf(false) }
    var editName by remember(currentUser.id) { mutableStateOf(currentUser.name) }
    var editEmail by remember(currentUser.id) { mutableStateOf(currentUser.email) }
    var editValidationMessage by remember { mutableStateOf(false) }
    val ownedPosts = myPosts
    val votedPosts = posts.filter { it.userUpvoted }
    val displayBadges = badges
    val points = stats?.user?.points ?: currentUser.points
    val nextLevelPoints = stats?.nextLevelPoints ?: currentUser.nextLevelPoints
    val progress = stats?.progress ?: if (nextLevelPoints > 0) {
        (points.toFloat() / nextLevelPoints).coerceIn(0f, 1f)
    } else {
        0f
    }
    val remainingPoints = stats?.remainingPoints ?: (nextLevelPoints - points).coerceAtLeast(0)
    val levelLabel = stats?.levelLabel ?: currentUser.level
    val dialogPosts = when (activeDialog) {
        "Mis Publicaciones" -> ownedPosts
        "Servicios que me importan / Mis Votos" -> votedPosts
        else -> emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                actions = {
                    IconButton(onClick = onOpenNotifications) {
                        BadgedBox(badge = { if (notificationsEnabled && notifications.count { !it.isRead } > 0) Badge { Text(notifications.count { !it.isRead }.toString()) } }) {
                            Icon(Icons.Outlined.Notifications, contentDescription = stringResource(R.string.notifications))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Identity & Gamification Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Avatar with Edit Badge
                        Box(modifier = Modifier.size(72.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentUser.name.trim().split(Regex("\\s+")).take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString(""),
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_photo), tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                            }
                        }

                        Column {
                            Text(
                                text = currentUser.name,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Text(levelLabel, style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold))
                                }
                            }
                            Text(stringResource(R.string.points_accumulated, points), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }

                    // Progress Bar (RN-GAM-002)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(stringResource(R.string.points_total, points, nextLevelPoints), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                Text(stringResource(R.string.current_level, levelLabel), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            }
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(stringResource(R.string.progress_percent, (progress * 100).roundToInt()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text(stringResource(R.string.points_remaining, remainingPoints), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }

                    // Estadísticas del perfil
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProfileStatItem(
                            value = "${stats?.totalPosts ?: ownedPosts.size}",
                            label = stringResource(R.string.publications),
                            modifier = Modifier.weight(1f)
                        )
                        ProfileStatItem(
                            value = "${stats?.pendingPosts ?: ownedPosts.count { it.status == com.servilocal.domain.model.PostStatus.PENDING_VERIFICATION }}",
                            label = stringResource(R.string.pending),
                            modifier = Modifier.weight(1f)
                        )
                        ProfileStatItem(
                            value = "${stats?.badgeCount ?: displayBadges.count { it.unlocked }}",
                            label = stringResource(R.string.badges),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Insignias (RN-GAM-003)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.badges), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text(stringResource(R.string.view_all), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                        }

                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            displayBadges.forEach { badge ->
                                Surface(
                                    color = if (badge.unlocked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(badge.icon, fontSize = 14.sp)
                                        Text(
                                            text = localizedBadgeTitle(badge.id, badge.title),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                            color = if (badge.unlocked) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.outline
                                        )
                                        if (!badge.unlocked) {
                                            Icon(Icons.Default.Lock, contentDescription = stringResource(R.string.locked_badge), modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            /*
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ProfileNavRow(
                    icon = Icons.Default.Schedule,
                    title = "Horario de atención",
                    subtitle = currentUser.workingHours,
                    onClick = { activeDialog = "Horario de atención" }
                )
            }
             */

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.my_activity),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        ProfileNavRow(
                            icon = Icons.Default.Edit,
                            title = stringResource(R.string.edit_profile),
                            subtitle = stringResource(R.string.personal_contact_info),
                            onClick = { activeDialog = "Editar Perfil" }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.padding(horizontal = 14.dp))
                        ProfileNavRow(
                            icon = Icons.AutoMirrored.Filled.Assignment,
                            title = stringResource(R.string.my_publications),
                            subtitle = stringResource(R.string.manage_my_posts),
                            onClick = { activeDialog = "Mis Publicaciones" }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.padding(horizontal = 14.dp))
                        ProfileNavRow(
                            icon = Icons.Default.ThumbUp,
                            title = stringResource(R.string.my_votes_title),
                            subtitle = stringResource(R.string.my_votes_summary),
                            onClick = { activeDialog = "Servicios que me importan / Mis Votos" }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.settings_section),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        ProfileNavRow(
                            icon = Icons.Default.Settings,
                            title = stringResource(R.string.settings_entry_title),
                            subtitle = stringResource(R.string.settings_entry_summary),
                            onClick = onOpenSettings
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.padding(horizontal = 14.dp))
                        ProfileNavRow(
                            icon = Icons.Default.Lock,
                            title = stringResource(R.string.privacy_security),
                            subtitle = stringResource(R.string.privacy_summary),
                            onClick = { activeDialog = "Privacidad y Seguridad" }
                        )
                    }
                }
            }

            // Logout & Delete account
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onLogout,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("logout_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.logout), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }

                TextButton(onClick = { showDeleteConfirmation = true }) {
                    Text(
                        stringResource(R.string.delete_account_title),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    activeDialog?.let { title ->
        AlertDialog(
            onDismissRequest = { activeDialog = null },
            title = { Text(profileDialogTitle(title)) },
            text = {
                when (title) {
                    "Editar Perfil" -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text(stringResource(R.string.full_name)) },
                            isError = editValidationMessage && editName.isBlank(),
                            supportingText = { if (editValidationMessage && editName.isBlank()) Text(stringResource(R.string.name_required)) },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editEmail,
                            onValueChange = { editEmail = it },
                            label = { Text(stringResource(R.string.email_label)) },
                            isError = editValidationMessage && !editEmail.trim().matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")),
                            supportingText = { if (editValidationMessage && !editEmail.trim().matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) Text(stringResource(R.string.valid_email_required)) },
                            singleLine = true
                        )
                    }
                    "Horario de atención" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.availability_intro), style = MaterialTheme.typography.bodyMedium)
                        Text(currentUser.workingHours, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.availability_notice), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    "Mis Publicaciones", "Servicios que me importan / Mis Votos" -> {
                        if (dialogPosts.isEmpty()) {
                            Text(stringResource(if (title == "Mis Publicaciones") R.string.no_posts_yet else R.string.no_votes_yet))
                        } else {
                            Column(
                                modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                dialogPosts.forEach { post ->
                                    val status = when (post.status) {
                                        com.servilocal.domain.model.PostStatus.PENDING_VERIFICATION -> stringResource(R.string.pending_review_status)
                                        com.servilocal.domain.model.PostStatus.APPROVED -> stringResource(R.string.published_status)
                                        com.servilocal.domain.model.PostStatus.REJECTED -> stringResource(R.string.rejected_status)
                                        com.servilocal.domain.model.PostStatus.FINISHED -> stringResource(R.string.finished_status)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(
                                            onClick = {
                                                activeDialog = null
                                                onOpenServiceDetail(post.id)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                Text(post.title, color = MaterialTheme.colorScheme.onSurface)
                                                Text(status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                            }
                                        }
                                        if (title == "Mis Publicaciones") {
                                            IconButton(onClick = {
                                                deletePostFailed = false
                                                postPendingDeletion = post
                                            }) {
                                                Icon(
                                                    Icons.Default.DeleteOutline,
                                                    contentDescription = stringResource(R.string.delete_post_accessibility, post.title),
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> Text(stringResource(R.string.prototype_memory_notice))
                }
            },
            confirmButton = {
                if (title == "Editar Perfil") {
                    TextButton(
                        onClick = {
                            if (viewModel.updateCurrentUser(editName, editEmail.trim())) {
                                editValidationMessage = false
                                activeDialog = null
                            } else {
                                editValidationMessage = true
                            }
                        }
                    ) { Text(stringResource(R.string.save)) }
                } else {
                    TextButton(onClick = { activeDialog = null }) { Text(stringResource(R.string.done)) }
                }
            },
            dismissButton = {
                if (title == "Editar Perfil") {
                    TextButton(onClick = { activeDialog = null }) { Text(stringResource(R.string.cancel)) }
                }
            }
        )
    }

    postPendingDeletion?.let { post ->
        AlertDialog(
            onDismissRequest = { postPendingDeletion = null; deletePostFailed = false },
            title = { Text(stringResource(R.string.delete_post_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.delete_post_confirmation, post.title))
                    if (deletePostFailed) {
                        Text(
                            stringResource(R.string.delete_post_failed),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (viewModel.deletePost(post.id)) {
                        postPendingDeletion = null
                        deletePostFailed = false
                    } else {
                        deletePostFailed = true
                    }
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { postPendingDeletion = null; deletePostFailed = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.delete_account_title)) },
            text = { Text(stringResource(R.string.demo_account_notice)) },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirmation = false; onLogout() }) { Text(stringResource(R.string.logout_lowercase)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmation = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun profileDialogTitle(title: String): String = when (title) {
    "Editar Perfil" -> stringResource(R.string.edit_profile)
    "Mis Publicaciones" -> stringResource(R.string.my_publications)
    "Servicios que me importan / Mis Votos" -> stringResource(R.string.my_votes_title)
    "Horario de atención" -> stringResource(R.string.working_hours)
    else -> stringResource(R.string.privacy_security)
}

@Composable
private fun localizedBadgeTitle(id: String, fallback: String): String {
    val resourceId = when (id) {
        "badge_1" -> R.string.badge_first_post
        "badge_2" -> R.string.badge_influencer
        "badge_3" -> R.string.badge_super_provider
        "badge_4" -> R.string.badge_community
        else -> return fallback
    }
    return stringResource(resourceId)
}

@Composable
private fun ProfileStatItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

