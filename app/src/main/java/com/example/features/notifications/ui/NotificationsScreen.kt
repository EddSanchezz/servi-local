package com.example.features.notifications.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.data.model.NotificationItem
import com.example.core.data.model.NotificationType
import com.example.core.data.model.UserRole
import com.example.features.app.viewmodel.AppUiState
import com.example.features.notifications.viewmodel.NotificationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    appUiState: AppUiState,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenServiceDetail: (String) -> Unit,
    onOpenModerationDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val notifications by viewModel.notifications.collectAsState()
    val notificationUiState by viewModel.uiState.collectAsState()
    val notificationFilter = notificationUiState.filter

    val filteredList = remember(notifications, notificationFilter) {
        when (notificationFilter) {
            "unread" -> notifications.filter { !it.read }
            "requests" -> notifications.filter { it.type == NotificationType.REQUEST }
            "comments" -> notifications.filter { it.type == NotificationType.COMMENT }
            else -> notifications
        }
    }

    val unreadCount = notifications.count { !it.read }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.markAllRead() }) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Marcar leídas", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
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
        ) {
            // M3 Filter Chips Carousel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = notificationFilter == "all",
                    onClick = { viewModel.setFilter("all") },
                    label = { Text("Todas (${notifications.size})") },
                    leadingIcon = if (notificationFilter == "all") {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null,
                    shape = RoundedCornerShape(8.dp)
                )

                FilterChip(
                    selected = notificationFilter == "unread",
                    onClick = { viewModel.setFilter("unread") },
                    label = { Text("No leídas ($unreadCount)") },
                    leadingIcon = {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    },
                    shape = RoundedCornerShape(8.dp)
                )

                FilterChip(
                    selected = notificationFilter == "requests",
                    onClick = { viewModel.setFilter("requests") },
                    label = { Text("Solicitudes") },
                    shape = RoundedCornerShape(8.dp)
                )

                FilterChip(
                    selected = notificationFilter == "comments",
                    onClick = { viewModel.setFilter("comments") },
                    label = { Text("Comentarios") },
                    shape = RoundedCornerShape(8.dp)
                )
            }

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.NotificationsOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Bandeja despejada", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(
                            "No tienes notificaciones en esta sección por ahora.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        NotificationCardItem(
                            item = item,
                            onToggleRead = { viewModel.toggleRead(item.id) },
                            onActionClick = {
                                if (!item.read) viewModel.toggleRead(item.id)
                                when {
                                    item.relatedChatId != null -> onOpenChat(item.relatedChatId)
                                    item.relatedPostId != null && appUiState.activeRole == UserRole.MODERATOR ->
                                        onOpenModerationDetail(item.relatedPostId)
                                    item.relatedPostId != null -> onOpenServiceDetail(item.relatedPostId)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCardItem(
    item: NotificationItem,
    onToggleRead: () -> Unit,
    onActionClick: () -> Unit
) {
    val isUnread = !item.read
    val cardBg = if (isUnread) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow

    val (iconBg, iconFg, iconVector) = when (item.type) {
        NotificationType.REQUEST -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, Icons.Default.Handshake)
        NotificationType.SYSTEM -> Triple(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.primary, Icons.Default.CheckCircle)
        NotificationType.COMMENT -> Triple(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Icons.Default.ChatBubble)
        NotificationType.ACHIEVEMENT -> Triple(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, Icons.Default.MilitaryTech)
        NotificationType.INTERACTION -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary, Icons.Default.Favorite)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("notification_item_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconVector, contentDescription = null, tint = iconFg, modifier = Modifier.size(22.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        if (isUnread) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                        }
                    }
                    Text(
                        text = item.time,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.relatedChatId != null || item.relatedPostId != null) {
                        Button(
                            onClick = onActionClick,
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                when {
                                    item.relatedChatId != null -> "Ver solicitud"
                                    item.type == NotificationType.COMMENT -> "Ver comentarios"
                                    else -> "Ver publicación"
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    IconButton(onClick = onToggleRead, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (isUnread) Icons.Default.MarkChatRead else Icons.Default.Markunread,
                            contentDescription = if (isUnread) "Marcar como leída" else "Marcar como no leída",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
