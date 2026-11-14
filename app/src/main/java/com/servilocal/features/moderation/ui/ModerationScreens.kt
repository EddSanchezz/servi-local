package com.servilocal.features.moderation.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.servilocal.domain.model.ModerationReportItem
import com.servilocal.domain.model.PostCategory
import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.model.ServicePost
import com.servilocal.domain.model.UserRole
import com.servilocal.core.ui.localizedServiceCategory
import com.servilocal.core.ui.components.ProfileNavRow
import com.servilocal.core.ui.components.ProfilePhotoAvatar
import com.servilocal.core.ui.components.PostCardHeaderSlot
import com.servilocal.core.ui.components.UnifiedPostCard
import com.servilocal.core.ui.components.PostPhotoViewer
import com.servilocal.core.ui.components.PostLocationMap
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.servilocal.features.app.viewmodel.AppUiState
import com.servilocal.features.moderation.viewmodel.ModerationViewModel
import com.servilocal.R
import kotlinx.coroutines.launch

@Composable
fun ModerationFeedScreen(
    viewModel: ModerationViewModel,
    onOpenNotifications: () -> Unit,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val pendingPosts by viewModel.pendingPosts.collectAsStateWithLifecycle()
    val moderationUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeFilter = moderationUiState.selectedCategory ?: moderationUiState.filter
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(moderationUiState.error, moderationUiState.actionResult) {
        val feedback = moderationUiState.error ?: moderationUiState.actionResult
        if (feedback != null) {
            snackbarHostState.showSnackbar(feedback)
            viewModel.clearFeedback()
        }
    }

    val categories = listOf(
        "Todos" to R.string.category_all,
        "Hogar" to R.string.category_home,
        "Educación" to R.string.category_education,
        "Mascotas" to R.string.category_pets,
        "Tecnología" to R.string.category_technology,
        "Transporte" to R.string.category_transport
    )

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                Text(
                    text = stringResource(R.string.moderation_feed_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
            IconButton(onClick = onOpenNotifications) {
                Icon(Icons.Outlined.Notifications, contentDescription = stringResource(R.string.in_app_alerts))
            }
        }

        // Live Moderation Queue Metric Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Rule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(stringResource(R.string.live_audit_queue), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(stringResource(R.string.posts_in_review, pendingPosts.size), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
                        Text(stringResource(R.string.priority), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer))
                    }
                }
            }
        }

        // Filter Chips Horizontal Scroller
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { (cat, labelRes) ->
                val isSelected = if (cat == "Todos") {
                    activeFilter.isBlank() || activeFilter.equals("Todos", ignoreCase = true) || activeFilter.equals("FIFO", ignoreCase = true)
                } else {
                    activeFilter.equals(cat, ignoreCase = true)
                }
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (cat == "Todos") {
                            viewModel.setCategoryFilter(null)
                        } else {
                            viewModel.setCategoryFilter(cat)
                        }
                    },
                    label = { Text(stringResource(labelRes)) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Moderation Cards Stream
        if (pendingPosts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(Icons.Default.TaskAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(stringResource(R.string.audit_queue_clear), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(stringResource(R.string.no_pending_posts), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
            ) {
                items(pendingPosts, key = { it.id }) { post ->
                    UnifiedPostCard(
                        post = post,
                        onClick = { onOpenDetail(post.id) },
                        headerSlot = PostCardHeaderSlot.ModeratorAuditDate(formatReportDate(post.createdAtMillis))
                    )
                }
            }
        }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModerationDetailScreen(
    postId: String,
    viewModel: ModerationViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val post = posts.find { it.id == postId }

    var showRejectSheet by remember { mutableStateOf(false) }
    var showDetailMenu by remember { mutableStateOf(false) }
    var showGuidelines by remember { mutableStateOf(false) }
    var viewerIndex by remember { mutableStateOf<Int?>(null) }
    var activePhotoIndex by remember { mutableIntStateOf(0) }
    val moderationUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(moderationUiState.error, moderationUiState.actionResult) {
        val feedback = moderationUiState.error ?: moderationUiState.actionResult
        if (feedback != null) {
            snackbarHostState.showSnackbar(feedback)
            viewModel.clearFeedback()
        }
    }

    if (post == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.post_not_found))
        }
        return
    }

    val reports by viewModel.reports.collectAsStateWithLifecycle()
    val providerPosts = posts.filter { it.providerName == post.providerName }
    val providerApproved = providerPosts.count { it.status == PostStatus.APPROVED }
    val providerRejected = providerPosts.count { it.status == PostStatus.REJECTED }
    val postReportCount = reports.count { it.targetPostId == post.id }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.audit_post_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showDetailMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options))
                        }
                        DropdownMenu(expanded = showDetailMenu, onDismissRequest = { showDetailMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.review_criteria)) },
                                onClick = { showDetailMenu = false; showGuidelines = true }
                            )
                            if (post.status == PostStatus.PENDING_VERIFICATION || post.status == PostStatus.APPROVED) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.mark_resolved)) },
                                    leadingIcon = { Icon(Icons.Default.TaskAlt, contentDescription = null) },
                                    onClick = {
                                        showDetailMenu = false
                                        viewModel.markPostResolved(post.id) { resolved ->
                                            if (resolved) onBack()
                                        }
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            when (post.status) {
                PostStatus.PENDING_VERIFICATION -> Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(
                            onClick = { showRejectSheet = true },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.reject), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        Button(
                            onClick = {
                                viewModel.approvePost(post.id) { approved ->
                                    if (approved) onBack()
                                }
                            },
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.approve), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
                PostStatus.APPROVED -> Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.resolveReportsForPost(post.id) { if (it) onBack() } },
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) { Text(stringResource(R.string.keep_active)) }
                        Button(
                            onClick = {
                                viewModel.rejectPost(
                                    post.id,
                                    "Incumplimiento confirmado",
                                    "Publicación retirada tras revisar las denuncias."
                                ) { rejected ->
                                    if (rejected) viewModel.resolveReportsForPost(post.id) { if (it) onBack() }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) { Text(stringResource(R.string.withdraw_post)) }
                    }
                }
                PostStatus.REJECTED, PostStatus.FINISHED -> Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                ) {
                    Button(
                        onClick = { viewModel.resolveReportsForPost(post.id) { if (it) onBack() } },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).height(48.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) { Text(stringResource(R.string.close_report)) }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Status Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.PendingActions, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text(stringResource(R.string.audit_status), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(
                                when (post.status) {
                                    PostStatus.PENDING_VERIFICATION -> stringResource(R.string.pending_review_status)
                                    PostStatus.APPROVED -> stringResource(R.string.published_status)
                                    PostStatus.REJECTED -> stringResource(R.string.rejected_status)
                                    PostStatus.FINISHED -> stringResource(R.string.finished_status)
                                },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            if (post.status == PostStatus.PENDING_VERIFICATION) stringResource(R.string.to_verify) else stringResource(R.string.own_decision_history),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (post.photos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.post_has_no_photos), color = MaterialTheme.colorScheme.outline)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { viewerIndex = activePhotoIndex }
                ) {
                    AsyncImage(
                        model = post.photos[activePhotoIndex.coerceIn(post.photos.indices)],
                        contentDescription = stringResource(R.string.photo_position, activePhotoIndex + 1, post.photos.size),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
                    ) {
                        Text(
                            "${activePhotoIndex + 1} / ${post.photos.size}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().align(Alignment.Center).padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = {
                                activePhotoIndex = (activePhotoIndex - 1 + post.photos.size) % post.photos.size
                            },
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.7f))
                        ) {
                            Icon(Icons.Default.ChevronLeft, stringResource(R.string.previous_photo), tint = Color.Black)
                        }
                        IconButton(
                            onClick = {
                                activePhotoIndex = (activePhotoIndex + 1) % post.photos.size
                            },
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.7f))
                        ) {
                            Icon(Icons.Default.ChevronRight, stringResource(R.string.next_photo), tint = Color.Black)
                        }
                    }
                    Row(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        post.photos.indices.forEach { index ->
                            Box(
                                Modifier
                                    .size(if (index == activePhotoIndex) 8.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (index == activePhotoIndex) Color.White
                                        else Color.White.copy(alpha = 0.4f)
                                    )
                            )
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(R.string.location_and_coverage),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(post.location, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (post.latitude != null && post.longitude != null) {
                        Text(
                            stringResource(R.string.selected_coordinates, post.latitude!!, post.longitude!!),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        PostLocationMap(
                            latitude = post.latitude,
                            longitude = post.longitude,
                            locationLabel = post.location,
                            modifier = Modifier.fillMaxWidth().height(180.dp)
                        )
                    }
                }
            }

            // Post Details
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(post.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("$ ${post.minPrice} - $ ${post.maxPrice} COP", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    Text(post.description, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Author Dossier & Metrics
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ProfilePhotoAvatar(post.providerName, post.providerAvatar, Modifier.size(40.dp))
                            Column {
                                Text(post.providerName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                Text(stringResource(R.string.registered_date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }

                    }

                    // Metrics row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(providerApproved.toString(), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text(stringResource(R.string.approved_plural), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(providerRejected.toString(), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.error)
                            Text(stringResource(R.string.rejections), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(postReportCount.toString(), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.nav_reports), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Modal Bottom Sheet: Motivo del Rechazo de Publicación (Mandatory Reason)
    if (showRejectSheet) {
        RejectionReasonBottomSheet(
            errorMessage = moderationUiState.error,
            onDismiss = { showRejectSheet = false },
            onConfirm = { reason, notes ->
                viewModel.rejectPost(post.id, reason, notes) { rejected ->
                    if (rejected) {
                        showRejectSheet = false
                        onBack()
                    }
                }
            }
        )
    }

    if (showGuidelines) {
        AlertDialog(
            onDismissRequest = { showGuidelines = false },
            title = { Text(stringResource(R.string.review_criteria)) },
            text = { Text(stringResource(R.string.review_guidelines)) },
            confirmButton = { TextButton(onClick = { showGuidelines = false }) { Text(stringResource(R.string.understood)) } }
        )
    }

    viewerIndex?.let { index ->
        PostPhotoViewer(
            photos = post.photos,
            initialIndex = index,
            onDismiss = { viewerIndex = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RejectionReasonBottomSheet(
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (reason: String, notes: String) -> Unit
) {
    var selectedReason by remember { mutableStateOf("Precio no concordante") }
    var notes by rememberSaveable { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val reasons = listOf(
        "Precio no concordante" to R.string.price_mismatch,
        "Servicio no autorizado" to R.string.unauthorized_service,
        "Imágenes no pertinentes o de baja calidad" to R.string.irrelevant_images,
        "Información de contacto en descripción" to R.string.contact_in_description,
        "Ubicación o cobertura inconsistente" to R.string.inconsistent_location
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(stringResource(R.string.rejection_reason_title), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Text(stringResource(R.string.select_required_reason), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close_dialog))
                }
            }

            Text(stringResource(R.string.predefined_options), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                reasons.forEach { (reason, labelRes) ->
                    val isSelected = selectedReason == reason
                    Surface(
                        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedReason = reason }
                            )
                            Text(stringResource(labelRes), style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal))
                        }
                    }
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.additional_details_author)) },
                placeholder = { Text(stringResource(R.string.additional_details_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) coroutineScope.launch { sheetState.expand() }
                    },
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )

            errorMessage?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = { onConfirm(selectedReason, notes) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.confirm_rejection), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun ModerationReportsScreen(
    viewModel: ModerationViewModel,
    onOpenNotifications: () -> Unit,
    onOpenReport: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val reports by viewModel.reports.collectAsStateWithLifecycle()
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf("Todos") }
    val filterOptions = listOf(
        "Todos" to R.string.category_all,
        "Fraude/Estafa" to R.string.fraud_scam,
        "Contacto no permitido" to R.string.prohibited_contact,
        "Contenido ofensivo" to R.string.offensive_content
    )
    fun matchesReportFilter(report: ModerationReportItem, filter: String): Boolean = when (filter) {
        "Fraude/Estafa" -> report.reasonTitle.contains("fraude", ignoreCase = true) || report.reasonTitle.contains("estafa", ignoreCase = true) || report.reasonTitle.contains("pago", ignoreCase = true) || report.reasonTitle.contains("engañoso", ignoreCase = true)
        "Contacto no permitido" -> report.reasonTitle.contains("contacto", ignoreCase = true)
        "Contenido ofensivo" -> report.reasonTitle.contains("ofensivo", ignoreCase = true)
        else -> true
    }
    val filteredReports = remember(reports, selectedFilter) {
        reports.filter { matchesReportFilter(it, selectedFilter) }
    }
    val cases = remember(filteredReports) {
        filteredReports.groupBy { it.targetPostId }.values
            .map { caseReports -> caseReports.sortedByDescending { it.createdAtMillis } }
            .sortedByDescending { it.first().createdAtMillis }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.reports_title), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            IconButton(onClick = onOpenNotifications) {
                Icon(Icons.Outlined.Notifications, contentDescription = stringResource(R.string.notifications))
            }
        }

        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterOptions.forEach { (filter, labelRes) ->
                val isSelected = selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filter },
                    label = { Text(if (filter == "Todos") stringResource(R.string.all_reports_count, reports.size) else "${stringResource(labelRes)} (${reports.count { matchesReportFilter(it, filter) }})") },
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        if (cases.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(stringResource(R.string.no_pending_reports), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                items(cases, key = { it.first().targetPostId }) { caseReports ->
                    val r = caseReports.first()
                    posts.find { it.id == r.targetPostId }?.let { post ->
                        UnifiedPostCard(
                            post = post,
                            onClick = { onOpenReport(r.id) },
                            headerSlot = PostCardHeaderSlot.ModeratorReportReason(r.reasonTitle)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModerationStatsScreen(
    viewModel: ModerationViewModel,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    val moderationUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val statsRange = moderationUiState.statsTimeRange
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val reports by viewModel.reports.collectAsStateWithLifecycle()
    val rangeStart = remember(statsRange) {
        val now = System.currentTimeMillis()
        when (statsRange) {
            "Hoy" -> java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
            "Esta semana" -> now - 7L * 24 * 60 * 60 * 1000
            else -> now - 30L * 24 * 60 * 60 * 1000
        }
    }
    val rangePosts = posts.filter { it.createdAtMillis >= rangeStart }
    val rangeReports = reports.filter { it.createdAtMillis >= rangeStart }
    val approvedCount = rangePosts.count { it.status == PostStatus.APPROVED }
    val rejectedCount = rangePosts.count { it.status == PostStatus.REJECTED }
    val pendingCount = rangePosts.count { it.status == PostStatus.PENDING_VERIFICATION }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.statistics_title), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            IconButton(onClick = onOpenNotifications) {
                Icon(Icons.Outlined.Notifications, contentDescription = stringResource(R.string.in_app_alerts))
            }
        }

        // Time range filter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(3.dp)
        ) {
            listOf("Hoy", "Esta semana", "Este mes").forEach { range ->
                val isSelected = statsRange == range
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                        .clickable { viewModel.setStatsTimeRange(range) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(when (range) {
                            "Hoy" -> R.string.today_range
                            "Esta semana" -> R.string.this_week_range
                            else -> R.string.this_month_range
                        }),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                        color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Hero Metric Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PostAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(R.string.real_time), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }

                Text(rangePosts.size.toString(), style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                Text(stringResource(R.string.new_posts), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(stringResource(R.string.memory_stats_description, stringResource(when (statsRange) {
                    "Hoy" -> R.string.today_range
                    "Esta semana" -> R.string.this_week_range
                    else -> R.string.this_month_range
                }).lowercase()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }

        // Operational 2x2 Grid
        Text(stringResource(R.string.operational_breakdown), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCardTile(
                icon = Icons.Default.CheckCircle,
                count = approvedCount.toString(),
                label = stringResource(R.string.approved_plural),
                iconColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            StatCardTile(
                icon = Icons.Default.Cancel,
                count = rejectedCount.toString(),
                label = stringResource(R.string.rejected_plural),
                iconColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCardTile(
                icon = Icons.Default.Flag,
                count = rangeReports.size.toString(),
                label = stringResource(R.string.reported_plural),
                iconColor = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
            StatCardTile(
                icon = Icons.Default.PendingActions,
                count = pendingCount.toString(),
                label = stringResource(R.string.pending),
                iconColor = MaterialTheme.colorScheme.outline,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun StatCardTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: String,
    label: String,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Text(count, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ModeratorProfileScreen(
    viewModel: ModerationViewModel,
    appUiState: AppUiState,
    onOpenNotifications: () -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onDarkThemeChange: (Boolean) -> Unit,
    onLanguageChange: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val moderatorUser by viewModel.moderatorUser.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val notificationsEnabled = appUiState.notificationsEnabled
    val context = LocalContext.current
    var showPolicies by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val selectedLanguage = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        .substringBefore(',')
        .substringBefore('-')
        .ifBlank { "es" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.staff_profile), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            IconButton(onClick = onOpenNotifications) {
                BadgedBox(badge = { if (notificationsEnabled && notifications.count { !it.isRead } > 0) Badge { Text(notifications.count { !it.isRead }.toString()) } }) {
                    Icon(Icons.Outlined.Notifications, contentDescription = stringResource(R.string.notifications))
                }
            }
        }

        // Staff Identity Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(modifier = Modifier.size(60.dp)) {
                        ProfilePhotoAvatar(
                            name = moderatorUser.name,
                            photoUrl = moderatorUser.photoUrl,
                            modifier = Modifier.fillMaxSize(),
                            initialsStyle = MaterialTheme.typography.titleLarge
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                        }
                    }

                    Column {
                        Text(moderatorUser.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(stringResource(R.string.staff_identifier, moderatorUser.staffId.orEmpty()), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                        Text(moderatorUser.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        // App settings
        Text(
            text = stringResource(R.string.settings_entry_title),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                ModeratorSettingsRow(
                    icon = Icons.Default.Palette,
                    title = stringResource(R.string.settings_theme_title),
                    subtitle = stringResource(
                        if (appUiState.darkTheme) R.string.settings_theme_dark else R.string.settings_theme_light
                    )
                ) {
                    Switch(
                        checked = appUiState.darkTheme,
                        onCheckedChange = onDarkThemeChange
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.padding(horizontal = 14.dp))
                ModeratorSettingsRow(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.settings_language),
                    subtitle = stringResource(
                        if (selectedLanguage == "en") R.string.language_english else R.string.language_spanish
                    )
                ) {
                    TextButton(onClick = { showLanguageDialog = true }) {
                        Text(stringResource(R.string.settings_change))
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.padding(horizontal = 14.dp))
                ModeratorSettingsRow(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.settings_notification_permissions),
                    subtitle = stringResource(R.string.settings_notification_permissions_summary)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = onNotificationsChange
                        )
                        IconButton(onClick = { context.openNotificationSettings() }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.permission_manage),
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Community guidelines
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ProfileNavRow(
                icon = Icons.Default.MenuBook,
                title = stringResource(R.string.community_policies),
                subtitle = stringResource(R.string.revocation_criteria),
                onClick = { showPolicies = true }
            )
        }

        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Default.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.logout_staff), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showPolicies) {
        AlertDialog(
            onDismissRequest = { showPolicies = false },
            title = { Text(stringResource(R.string.community_policies)) },
            text = { Text(stringResource(R.string.community_policies_body)) },
            confirmButton = { TextButton(onClick = { showPolicies = false }) { Text(stringResource(R.string.done)) } }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.settings_language)) },
            text = {
                Column {
                    ModeratorLanguageOption(
                        label = stringResource(R.string.language_spanish),
                        selected = selectedLanguage == "es",
                        onClick = {
                            showLanguageDialog = false
                            onLanguageChange("es")
                        }
                    )
                    ModeratorLanguageOption(
                        label = stringResource(R.string.language_english),
                        selected = selectedLanguage == "en",
                        onClick = {
                            showLanguageDialog = false
                            onLanguageChange("en")
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.done))
                }
            }
        )
    }
}

@Composable
private fun ModeratorSettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        trailing()
    }
}

@Composable
private fun ModeratorLanguageOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        TextButton(onClick = onClick) { Text(label) }
    }
}

private fun Context.openNotificationSettings() {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", packageName, null))
    }
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

internal fun formatReportDate(millis: Long): String =
    java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.forLanguageTag("es-CO")).format(java.util.Date(millis))
