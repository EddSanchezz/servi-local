package com.servilocal.features.moderation.ui

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.servilocal.domain.model.ModerationReportItem
import com.servilocal.domain.model.PostCategory
import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.model.ServicePost
import com.servilocal.domain.model.UserRole
import com.servilocal.core.ui.localizedServiceCategory
import com.servilocal.core.ui.components.ProfileNavRow
import com.servilocal.features.app.viewmodel.AppUiState
import com.servilocal.features.moderation.viewmodel.ModerationViewModel
import com.servilocal.R

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
                    ModerationAuditCard(
                        post = post,
                        onAuditDetail = { onOpenDetail(post.id) },
                        onApprove = { viewModel.approvePost(post.id) }
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

@Composable
fun ModerationAuditCard(
    post: ServicePost,
    onAuditDetail: () -> Unit,
    onApprove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("moderation_card_${post.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.HourglassTop, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text(stringResource(R.string.to_verify), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(localizedServiceCategory(post.category), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                Text(stringResource(R.string.queued_time, 15), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(post.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), maxLines = 2)
                    Text("$ ${post.minPrice} - $ ${post.maxPrice} COP", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.by_provider_at_location, post.providerName, post.location), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    Text(stringResource(R.string.identity_verified), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAuditDetail,
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.audit_detail), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }

                FilledTonalButton(
                    onClick = onApprove,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.approve), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
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
    var noteMode by remember { mutableStateOf("internal") } // internal vs author
    var noteInput by remember { mutableStateOf("") }
    val sampleReviewNote = stringResource(R.string.sample_review_note)
    var notesList by remember(sampleReviewNote) { mutableStateOf(listOf(sampleReviewNote)) }
    var showDetailMenu by remember { mutableStateOf(false) }
    var showGuidelines by remember { mutableStateOf(false) }
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
                            onClick = { if (viewModel.resolveReportsForPost(post.id)) onBack() },
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
                                    val reportsResolved = viewModel.resolveReportsForPost(post.id)
                                    if (rejected && reportsResolved) onBack()
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
                        onClick = { if (viewModel.resolveReportsForPost(post.id)) onBack() },
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

            // Image uploads are outside this phase, so the empty state is explicit.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ImageNotSupported, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(40.dp))
                    Text(stringResource(R.string.no_attached_images), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
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
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(post.providerName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                            }
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

            // AI Safety & Moderation Log
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Policy, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text(stringResource(R.string.review_log_title), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)

                    Text(stringResource(R.string.moderator_notes), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))

                    notesList.forEach { note ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(note, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
                        }
                    }

                    // Append note input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = noteInput,
                            onValueChange = { noteInput = it },
                            placeholder = { Text(stringResource(R.string.review_note_hint), style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        IconButton(
                            onClick = {
                                if (noteInput.isNotBlank()) {
                                    notesList = notesList + listOf(noteInput)
                                    noteInput = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Send, contentDescription = stringResource(R.string.save_note), tint = MaterialTheme.colorScheme.primary)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RejectionReasonBottomSheet(
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (reason: String, notes: String) -> Unit
) {
    var selectedReason by remember { mutableStateOf("Precio no concordante") }
    var notes by remember { mutableStateOf("") }

    val reasons = listOf(
        "Precio no concordante" to R.string.price_mismatch,
        "Servicio no autorizado" to R.string.unauthorized_service,
        "Imágenes no pertinentes o de baja calidad" to R.string.irrelevant_images,
        "Información de contacto en descripción" to R.string.contact_in_description,
        "Ubicación o cobertura inconsistente" to R.string.inconsistent_location
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding(),
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
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                shape = RoundedCornerShape(12.dp)
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

        if (filteredReports.isEmpty()) {
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
                items(filteredReports, key = { it.id }) { r ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenReport(r.id) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    Text(
                                        r.reasonTitle,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "Pendiente",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text("${r.targetType}: ${r.targetTitle}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(r.description, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(R.string.reported_by_short, r.reporterName), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text(formatReportDate(r.createdAtMillis), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
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
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val moderatorUser by viewModel.moderatorUser.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val notificationsEnabled = appUiState.notificationsEnabled
    var activeInfoDialog by remember { mutableStateOf<String?>(null) }

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
                        Box(
                            modifier = Modifier.fillMaxSize().clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(moderatorUser.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""), style = MaterialTheme.typography.titleLarge.copy(color = Color.White, fontWeight = FontWeight.Bold))
                        }
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

        // Active Shift Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (moderatorUser.activeShift) Color(0xFF10B981) else MaterialTheme.colorScheme.outline)
                    )
                    Column {
                        Text(stringResource(R.string.queue_availability), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text(
                            text = stringResource(if (moderatorUser.activeShift) R.string.active_shift else R.string.paused_shift, moderatorUser.workingHours),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 2
                        )
                    }
                }
                Switch(
                    checked = moderatorUser.activeShift,
                    onCheckedChange = { viewModel.toggleModeratorShift() }
                )
            }
        }

        // Guidelines & Config
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                ProfileNavRow(
                    icon = Icons.Default.NotificationsActive,
                    title = stringResource(R.string.audit_notification_settings),
                    subtitle = stringResource(R.string.critical_queue_alerts),
                    onClick = { activeInfoDialog = "Ajustes de Notificaciones de Auditoría" }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.padding(horizontal = 14.dp))
                ProfileNavRow(
                    icon = Icons.Default.MenuBook,
                    title = stringResource(R.string.community_policies),
                    subtitle = stringResource(R.string.revocation_criteria),
                    onClick = { activeInfoDialog = "Políticas Comunitarias y Guía v4.2" }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.padding(horizontal = 14.dp))
                ProfileNavRow(
                    icon = Icons.Default.HistoryToggleOff,
                    title = stringResource(R.string.own_decision_history),
                    subtitle = stringResource(R.string.last_90_days),
                    onClick = { activeInfoDialog = "Historial de Decisiones Propias" }
                )
            }
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

    activeInfoDialog?.let { title ->
        AlertDialog(
            onDismissRequest = { activeInfoDialog = null },
            title = { Text(stringResource(when (title) {
                "Ajustes de Notificaciones de Auditoría" -> R.string.audit_notification_settings
                "Políticas Comunitarias y Guía v4.2" -> R.string.community_policies
                else -> R.string.own_decision_history
            })) },
            text = {
                when (title) {
                    "Ajustes de Notificaciones de Auditoría" -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.in_app_alerts), modifier = Modifier.weight(1f))
                        Switch(checked = notificationsEnabled, onCheckedChange = onNotificationsChange)
                    }
                    "Políticas Comunitarias y Guía v4.2" -> Text(stringResource(R.string.community_policies_body))
                    else -> Text(stringResource(R.string.decision_history_memory))
                }
            },
            confirmButton = { TextButton(onClick = { activeInfoDialog = null }) { Text(stringResource(R.string.done)) } }
        )
    }
}

internal fun formatReportDate(millis: Long): String =
    java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.forLanguageTag("es-CO")).format(java.util.Date(millis))
