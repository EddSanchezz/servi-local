package com.servilocal.features.home.ui

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import com.servilocal.core.util.RequestResult
import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.exception.DuplicateReportException
import com.servilocal.domain.usecase.chat.CreateServiceRequestResult
import com.servilocal.features.home.viewmodel.ServiceDetailViewModel
import com.servilocal.core.ui.localizedDistance
import com.servilocal.core.ui.localizedServiceCategory
import com.servilocal.core.ui.components.ProfilePhotoAvatar
import com.servilocal.core.ui.components.PostPhotoViewer
import com.servilocal.core.ui.components.PostLocationMap
import coil.compose.AsyncImage
import com.servilocal.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailScreen(
    postId: String,
    viewModel: ServiceDetailViewModel,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    onEditPost: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val observedPost by viewModel.post.collectAsStateWithLifecycle()
    val post = observedPost
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val likes by viewModel.likes.collectAsStateWithLifecycle()
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var activeSlideIndex by remember { mutableIntStateOf(0) }
    var newCommentText by remember { mutableStateOf("") }
    var isMenuExpanded by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showServiceRequestDialog by remember { mutableStateOf(false) }
    var serviceRequestError by remember { mutableStateOf<String?>(null) }
    var viewerIndex by remember { mutableStateOf<Int?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Las reglas de likes y comentarios que fallan se muestran como Failure en un snackbar.
    val interactionError = (uiState.likeResult as? RequestResult.Failure)?.errorMessage
        ?: (uiState.commentResult as? RequestResult.Failure)?.errorMessage
    LaunchedEffect(uiState) {
        interactionError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearInteractionFeedback()
        }
    }

    if (post == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.post_not_found))
        }
        return
    }

    val isOwner = post.providerId == currentUser.id
    val photoCount = post.photos.size
    LaunchedEffect(photoCount) {
        if (photoCount == 0) activeSlideIndex = 0
        else activeSlideIndex = activeSlideIndex.coerceIn(0, photoCount - 1)
    }
    val statusLabel = when (post.status) {
        PostStatus.PENDING_VERIFICATION -> stringResource(R.string.pending_review_status)
        PostStatus.APPROVED -> stringResource(R.string.published_status)
        PostStatus.REJECTED -> stringResource(R.string.rejected_status)
        PostStatus.FINISHED -> stringResource(R.string.finished_status)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        stringResource(R.string.post_detail_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { isMenuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options))
                        }
                        DropdownMenu(expanded = isMenuExpanded, onDismissRequest = { isMenuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.share_service)) },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = {
                                    isMenuExpanded = false
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "${post.title}\n${post.description}\n${post.location}")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_service)))
                                }
                            )
                            if (!isOwner) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.report_post)) },
                                    leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
                                    onClick = {
                                        isMenuExpanded = false
                                        showReportDialog = true
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            when {
                post.status in setOf(PostStatus.APPROVED, PostStatus.PENDING_VERIFICATION) -> Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column {
                            Text(stringResource(R.string.estimated_rate), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = if (post.isPriceToAgree) stringResource(R.string.price_to_agree) else "$ ${post.minPrice} - $ ${post.maxPrice} ${post.priceUnit}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                        if (isOwner) {
                            Button(
                                onClick = { onEditPost(post.id) },
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("edit_post_button"),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.edit_post), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(
                                    onClick = { viewModel.toggleUpvote(post.id) },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = if (likes.likedByCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = if (likes.likedByCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                    modifier = Modifier.weight(0.85f).height(48.dp)
                                ) {
                                    Icon(imageVector = if (likes.likedByCurrentUser) Icons.Default.ThumbUp else Icons.Outlined.ThumbUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = stringResource(if (likes.likedByCurrentUser) R.string.supported else R.string.like_post), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                                }
                                Button(
                                    onClick = {
                                        serviceRequestError = null
                                        showServiceRequestDialog = true
                                    },
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.weight(1.15f).height(48.dp).testTag("request_service_button"),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.request_service), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                                }
                            }
                        }
                    }
                }
                isOwner -> Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                ) {
                    Text(
                        text = stringResource(R.string.post_status_not_editable, statusLabel),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            ) { data ->
                key(data.visuals.message) {
                val successful = data.visuals.message == context.getString(R.string.report_success_message)
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 }
                ) {
                    CustomFeedbackBanner(
                        message = data.visuals.message,
                        icon = if (successful) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                        isSuccess = successful,
                        onDismiss = data::dismiss
                    )
                }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (isOwner && post.status == PostStatus.REJECTED) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Text(
                                    stringResource(R.string.rejection_reason_title),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Text(
                                post.rejectionReason.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            post.rejectionNotes?.takeIf { it.isNotBlank() }?.let { notes ->
                                Text(
                                    stringResource(R.string.rejection_notes_title),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            }

            // Photo Carousel
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(220.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(enabled = post.photos.isNotEmpty()) { viewerIndex = activeSlideIndex }
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                ) {
                    if (post.photos.isNotEmpty()) {
                        AsyncImage(
                            model = post.photos[activeSlideIndex.coerceIn(post.photos.indices)],
                            contentDescription = stringResource(R.string.photo_position, activeSlideIndex + 1, photoCount),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    // Visual slide content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(14.dp))
                                    Text(stringResource(R.string.certified_work), style = MaterialTheme.typography.labelSmall.copy(color = Color.White))
                                }
                            }

                            Surface(
                                color = Color.Black.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "${activeSlideIndex + 1} / ${photoCount.coerceAtLeast(1)}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color.White),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Slide caption
                        Text(
                            text = when (activeSlideIndex) {
                                0 -> stringResource(R.string.slide_caption_inspection)
                                1 -> stringResource(R.string.slide_caption_tools)
                                else -> stringResource(R.string.slide_caption_repairs)
                            },
                            style = MaterialTheme.typography.titleSmall.copy(color = Color.White, fontWeight = FontWeight.SemiBold)
                        )
                    }

                    // Carousel Controls (Prev & Next buttons)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { if (photoCount > 0) activeSlideIndex = (activeSlideIndex - 1 + photoCount) % photoCount },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.7f))
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = stringResource(R.string.previous_photo), tint = Color.Black)
                        }

                        IconButton(
                            onClick = { if (photoCount > 0) activeSlideIndex = (activeSlideIndex + 1) % photoCount },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.7f))
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = stringResource(R.string.next_photo), tint = Color.Black)
                        }
                    }

                    // Dots indicator
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        repeat(photoCount.coerceAtLeast(1)) { i ->
                            Box(
                                modifier = Modifier
                                    .size(if (i == activeSlideIndex) 8.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(if (i == activeSlideIndex) Color.White else Color.White.copy(alpha = 0.4f))
                            )
                        }
                    }
                }
            }

            // Category & Title
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(localizedServiceCategory(post.category), style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(post.responseTime, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = post.title,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Price Range Card
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
                            Column {
                                Text(stringResource(R.string.estimated_price_range), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text(
                                    text = if (post.isPriceToAgree) stringResource(R.string.price_to_agree) else "$ ${post.minPrice} - $ ${post.maxPrice} ${post.priceUnit}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(stringResource(R.string.diagnostic_visit), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(
                                Icons.Default.Payments,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            // Description & Technical Specifications
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = stringResource(R.string.service_description_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = post.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 20.sp
                            )

                        }
                    }
                }
            }

            // Location section
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = stringResource(R.string.location_and_coverage),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Text(text = post.location, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                            if (post.latitude != null && post.longitude != null) {
                                Text(
                                    text = stringResource(R.string.selected_coordinates, post.latitude!!, post.longitude!!),
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
                }
            }

            // Professional Profile Card
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = stringResource(R.string.professional_profile),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    ProfilePhotoAvatar(post.providerName, post.providerAvatar, Modifier.size(50.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(post.providerName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                            Icon(Icons.Default.CheckCircle, contentDescription = stringResource(R.string.verified), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        }
                                        Text("${post.location} (${localizedDistance(post.distance)})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = post.providerLevel,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.ThumbUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Text(
                                    text = stringResource(R.string.confidence_votes, likes.count),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Comments & Questions Section
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.comments_questions),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Badge(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
                                Text(
                                    text = comments.size.toString(),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Comment input bar
                    if (post.status == PostStatus.APPROVED) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newCommentText,
                                onValueChange = { newCommentText = it },
                                placeholder = {
                                    Text(
                                        text = stringResource(if (isOwner) R.string.owner_reply_hint else R.string.visitor_question_hint),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedBorderColor = Color.Transparent
                                )
                            )
                            IconButton(
                                onClick = {
                                    if (viewModel.addComment(post.id, newCommentText)) {
                                        newCommentText = ""
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Send, contentDescription = stringResource(R.string.send), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // List of comments
            items(comments, key = { it.id }) { comment ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = comment.authorName,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (comment.isAuthorReply) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(comment.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                            if (comment.isAuthorReply) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.author),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = comment.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        ReportContentDialog(
            title = stringResource(R.string.report_post),
            onDismiss = { showReportDialog = false },
            onSubmit = { reason, details ->
                coroutineScope.launch {
                    val outcome = runCatching { viewModel.reportPost(post.id, reason, details) }
                    showReportDialog = false
                    when {
                        outcome.getOrNull() == true -> snackbarHostState.showSnackbar(context.getString(R.string.report_success_message))
                        outcome.exceptionOrNull() is DuplicateReportException -> snackbarHostState.showSnackbar(context.getString(R.string.report_duplicate_message))
                        outcome.exceptionOrNull() != null -> snackbarHostState.showSnackbar(context.getString(R.string.report_failure_message))
                        else -> snackbarHostState.showSnackbar(
                            context.getString(R.string.cannot_report_own_post)
                        )
                    }
                }
            }
        )
    }

    if (showServiceRequestDialog) {
        ServiceRequestDialog(
            applicantName = currentUser.name,
            errorMessage = serviceRequestError,
            onDismiss = { showServiceRequestDialog = false },
            onSubmit = { applicantName, contact, address, description ->
                when (val result = viewModel.createServiceRequest(
                    postId = post.id,
                    applicantName = applicantName,
                    contact = contact,
                    address = address,
                    description = description
                )) {
                    is CreateServiceRequestResult.Success -> {
                        showServiceRequestDialog = false
                        onOpenChat(result.conversationId)
                    }
                    CreateServiceRequestResult.PostNotFound -> serviceRequestError = context.getString(R.string.post_unavailable)
                    CreateServiceRequestResult.PostNotApproved -> serviceRequestError = context.getString(R.string.verified_request_only)
                    CreateServiceRequestResult.OwnPost -> serviceRequestError = context.getString(R.string.own_post_request_error)
                    CreateServiceRequestResult.MissingApplicantName -> serviceRequestError = context.getString(R.string.name_required_short)
                    CreateServiceRequestResult.MissingContactMethod -> serviceRequestError = context.getString(R.string.contact_required)
                    CreateServiceRequestResult.MissingAddress -> serviceRequestError = context.getString(R.string.address_required)
                    CreateServiceRequestResult.MissingDescription -> serviceRequestError = context.getString(R.string.service_description_required)
                    CreateServiceRequestResult.RequestCouldNotBeCreated -> serviceRequestError = context.getString(R.string.request_creation_failed)
                }
            }
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

@Composable
private fun CustomFeedbackBanner(
    message: String,
    icon: ImageVector,
    isSuccess: Boolean,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val container = if (isSuccess) colors.primaryContainer else colors.errorContainer
    val content = if (isSuccess) colors.onPrimaryContainer else colors.onErrorContainer
    val border = if (isSuccess) colors.primary.copy(alpha = 0.3f) else colors.error.copy(alpha = 0.3f)
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = container,
        border = BorderStroke(1.dp, border)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(content.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp)) }
            Text(
                message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = content,
                maxLines = 3
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close_dialog), tint = content, modifier = Modifier.size(18.dp))
            }
        }
    }
}

