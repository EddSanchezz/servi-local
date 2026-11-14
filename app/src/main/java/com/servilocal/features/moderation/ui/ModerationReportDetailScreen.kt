package com.servilocal.features.moderation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servilocal.domain.model.ModerationReportItem
import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.model.ReportResolution
import com.servilocal.domain.model.UserRole
import com.servilocal.domain.model.ServicePost
import com.servilocal.features.moderation.viewmodel.ModerationViewModel
import com.servilocal.core.ui.localizedDistance
import com.servilocal.core.ui.localizedServiceCategory
import com.servilocal.core.ui.components.PostLocationMap
import com.servilocal.core.ui.components.PostPhotoViewer
import com.servilocal.core.ui.components.ProfilePhotoAvatar
import com.servilocal.R
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModerationReportDetailScreen(
    reportId: String,
    viewModel: ModerationViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reports by viewModel.reportHistory.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val report = reports.firstOrNull { it.id == reportId }
    val post = report?.let { current -> posts.firstOrNull { it.id == current.targetPostId } }
    var showResolveSheet by remember { mutableStateOf(false) }
    var photoIndex by remember(reportId) { mutableIntStateOf(0) }
    var photoViewerIndex by remember { mutableStateOf<Int?>(null) }

    if (report == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(stringResource(R.string.report_unavailable), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(
                    stringResource(R.string.report_already_resolved),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onBack) { Text(stringResource(R.string.back_to_reports)) }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.report_detail_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (currentUser.role == UserRole.MODERATOR) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding()
            ) {
                Button(
                    onClick = { showResolveSheet = true },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .height(50.dp)
                        .testTag("resolve_case_button")
                ) {
                    Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.resolve_case), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
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
            // Primero se presenta la publicación para dar contexto al caso.
            if (post == null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.full_post), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text(stringResource(R.string.reported_post_unavailable), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                ReportedPostCard(
                    post = post,
                    photoIndex = photoIndex,
                    onPhotoIndexChange = { photoIndex = it },
                    onOpenPhoto = { photoViewerIndex = it }
                )
            }

            val caseReports = reports.filter { it.targetPostId == report.targetPostId }
                .sortedByDescending { it.createdAtMillis }
            ReportHistoryExpandableCard(caseReports)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showResolveSheet) {
        ResolveReportSheet(
            postStatus = post?.status,
            onDismiss = { showResolveSheet = false },
            onConfirm = { resolution, comment, newStatus, blockUser, onComplete ->
                viewModel.resolveReport(report.id, resolution, comment, newStatus, blockUser) { success ->
                    if (success) {
                        showResolveSheet = false
                        onBack()
                    }
                    onComplete(success)
                }
            }
        )
    }

    photoViewerIndex?.let { index ->
        PostPhotoViewer(
            photos = post?.photos.orEmpty(),
            initialIndex = index,
            onDismiss = { photoViewerIndex = null }
        )
    }
}

@Composable
private fun ReportSummaryCard(report: ModerationReportItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        report.reasonTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        stringResource(if (report.status == com.servilocal.domain.model.ReportStatus.PENDING) R.string.status_pending else R.string.status_reviewed),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(report.targetTitle, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
            Text(
                report.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.reported_by, report.reporterName),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    formatReportDate(report.createdAtMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun ReportedPostCard(
    post: ServicePost,
    photoIndex: Int,
    onPhotoIndexChange: (Int) -> Unit,
    onOpenPhoto: (Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Carrusel de fotos con el mismo patrón de navegación que el detalle de publicación.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)))
                    .clickable(enabled = post.photos.isNotEmpty()) { onOpenPhoto(photoIndex) }
            ) {
                if (post.photos.isNotEmpty()) {
                    AsyncImage(
                        model = post.photos[photoIndex.coerceIn(post.photos.indices)],
                        contentDescription = stringResource(R.string.photo_position, photoIndex + 1, post.photos.size),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                        color = Color.Black.copy(alpha = 0.58f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            "${photoIndex + 1} / ${post.photos.size}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                    if (post.photos.size > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth().align(Alignment.Center).padding(horizontal = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = { onPhotoIndexChange((photoIndex - 1 + post.photos.size) % post.photos.size) }) {
                                Icon(Icons.Default.ChevronLeft, stringResource(R.string.previous_photo), tint = Color.White)
                            }
                            IconButton(onClick = { onPhotoIndexChange((photoIndex + 1) % post.photos.size) }) {
                                Icon(Icons.Default.ChevronRight, stringResource(R.string.next_photo), tint = Color.White)
                            }
                        }
                    }
                } else {
                    Text(
                        stringResource(R.string.post_has_no_photos),
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            localizedServiceCategory(post.category),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                    Text(post.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                }
                Spacer(modifier = Modifier.width(8.dp))
                PostStatusChip(post.status)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.service_description_title), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text(post.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
                Text(
                    if (post.isPriceToAgree) stringResource(R.string.price_to_agree) else "$ ${post.minPrice} - $ ${post.maxPrice} COP",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.location_and_coverage), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text(post.location, style = MaterialTheme.typography.bodyMedium)
                }
                PostLocationMap(
                    latitude = post.latitude,
                    longitude = post.longitude,
                    locationLabel = post.location,
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.professional_profile), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfilePhotoAvatar(post.providerName, post.providerAvatar, Modifier.size(54.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(post.providerName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text(post.providerLevel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        Text(localizedDistance(post.distance), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            Text(
                stringResource(R.string.rating_summary, post.rating, post.reviewCount, post.upvotesCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun ReportHistoryExpandableCard(reports: List<ModerationReportItem>) {
    var expanded by remember(reports.firstOrNull()?.targetPostId) { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().animateContentSize()
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(stringResource(R.string.report_case_total, reports.size), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text(
                        if (expanded) stringResource(R.string.report_history_collapse_hint) else stringResource(R.string.report_history_expand_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = stringResource(if (expanded) R.string.collapse_reports else R.string.expand_reports)
                    )
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
                    reports.forEachIndexed { index, item ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                stringResource(R.string.report_history_item, index + 1),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                            ReportSummaryCard(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PostStatusChip(status: PostStatus) {
    val (label, container, content) = when (status) {
        PostStatus.PENDING_VERIFICATION -> Triple(
            stringResource(R.string.status_pending),
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        PostStatus.APPROVED -> Triple(
            stringResource(R.string.status_verified),
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        PostStatus.REJECTED -> Triple(
            stringResource(R.string.rejected_status),
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        PostStatus.FINISHED -> Triple(
            stringResource(R.string.finished_status),
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Surface(color = container, shape = RoundedCornerShape(8.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = content,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResolveReportSheet(
    postStatus: PostStatus?,
    onDismiss: () -> Unit,
    onConfirm: (ReportResolution, String, PostStatus?, Boolean, (Boolean) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var resolution by remember { mutableStateOf(ReportResolution.DISMISSED) }
    var comment by remember { mutableStateOf("") }
    var newStatus by remember { mutableStateOf<PostStatus?>(null) }
    var blockUser by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.resolve_case), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Text(
                        stringResource(R.string.decide_report_resolution),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close_dialog))
                }
            }

            Text(stringResource(R.string.decision), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))

            DecisionOption(
                selected = resolution == ReportResolution.DISMISSED,
                title = stringResource(R.string.discard_report),
                subtitle = stringResource(R.string.discard_report_summary),
                onClick = {
                    resolution = ReportResolution.DISMISSED
                    newStatus = null
                    blockUser = false
                    error = null
                }
            )
            DecisionOption(
                selected = resolution == ReportResolution.UPHELD,
                title = stringResource(R.string.valid_report),
                subtitle = stringResource(R.string.valid_report_summary),
                onClick = {
                    resolution = ReportResolution.UPHELD
                    newStatus = if (postStatus == PostStatus.REJECTED) null else PostStatus.REJECTED
                    blockUser = false
                    error = null
                }
            )

            if (resolution == ReportResolution.UPHELD) {
                Text(
                    stringResource(R.string.report_case_action_title),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                DecisionOption(
                    selected = newStatus == PostStatus.REJECTED && !blockUser,
                    title = stringResource(R.string.reject_post_action_title),
                    subtitle = stringResource(R.string.reject_post_action_summary),
                    onClick = { newStatus = PostStatus.REJECTED; blockUser = false },
                    enabled = postStatus != PostStatus.REJECTED
                )
                DecisionOption(
                    selected = newStatus == null && blockUser,
                    title = stringResource(R.string.block_provider_action_title),
                    subtitle = stringResource(R.string.block_provider_action_summary),
                    onClick = { newStatus = null; blockUser = true }
                )
                DecisionOption(
                    selected = newStatus == PostStatus.REJECTED && blockUser,
                    title = stringResource(R.string.reject_and_block_action),
                    subtitle = stringResource(R.string.reject_and_block_action_summary),
                    onClick = { newStatus = PostStatus.REJECTED; blockUser = true },
                    enabled = postStatus != PostStatus.REJECTED
                )
            }

            OutlinedTextField(
                value = comment,
                onValueChange = {
                    comment = it
                    error = null
                },
                label = { Text(stringResource(R.string.moderator_comments)) },
                placeholder = {
                    Text(
                        if (resolution == ReportResolution.UPHELD) {
                            stringResource(R.string.upheld_comment_hint)
                        } else {
                            stringResource(R.string.dismissed_comment_hint)
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp)
            )

            error?.let { message ->
                Text(
                    message,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    run {
                        onConfirm(
                            resolution,
                            comment.trim(),
                            if (resolution == ReportResolution.UPHELD) newStatus else null,
                            resolution == ReportResolution.UPHELD && blockUser
                        ) { success ->
                            if (!success) error = context.getString(R.string.resolution_failed)
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (resolution == ReportResolution.DISMISSED) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    contentColor = if (resolution == ReportResolution.DISMISSED) {
                        MaterialTheme.colorScheme.onSecondary
                    } else {
                        MaterialTheme.colorScheme.onError
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("confirm_resolution_button")
            ) {
                Text(
                    stringResource(if (resolution == ReportResolution.DISMISSED) R.string.dismiss_report_action else R.string.confirm_resolution),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DecisionOption(
    selected: Boolean,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RadioButton(selected = selected, enabled = enabled, onClick = onClick)
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
