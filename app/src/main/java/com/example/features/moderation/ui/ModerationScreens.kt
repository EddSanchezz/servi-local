package com.example.features.moderation.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ModerationReportItem
import com.example.domain.model.PostStatus
import com.example.domain.model.ServicePost
import com.example.domain.model.UserRole
import com.example.core.ui.components.ProfileNavRow
import com.example.features.app.viewmodel.AppUiState
import com.example.features.moderation.viewmodel.ModerationViewModel

@Composable
fun ModerationFeedScreen(
    viewModel: ModerationViewModel,
    onOpenNotifications: () -> Unit,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val reports by viewModel.reports.collectAsStateWithLifecycle()
    val moderationUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val moderationFilter = moderationUiState.filter
    val pendingPosts = remember(posts, reports, moderationFilter) {
        val pending = posts.filter { it.status == PostStatus.PENDING_VERIFICATION }
        when {
            moderationFilter.startsWith("Plomería", ignoreCase = true) -> pending.filter { "plomer" in it.title.lowercase() || "plomer" in it.category.lowercase() }
            moderationFilter.startsWith("Electricidad", ignoreCase = true) -> pending.filter { "electric" in it.title.lowercase() || "electric" in it.category.lowercase() }
            moderationFilter.startsWith("Cerrajería", ignoreCase = true) -> pending.filter { "cerrajer" in it.title.lowercase() || "cerrajer" in it.category.lowercase() }
            moderationFilter.startsWith("Urgentes", ignoreCase = true) -> pending.sortedByDescending { post -> reports.filter { it.targetPostId == post.id }.sumOf { it.reportCount } }
            else -> pending.sortedBy { it.id.substringAfterLast('_').toLongOrNull() ?: Long.MAX_VALUE }
        }
    }

    Column(
        modifier = modifier
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
                    text = "Feed de Auditoría",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
            IconButton(onClick = onOpenNotifications) {
                Icon(Icons.Outlined.Notifications, contentDescription = "Alertas")
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
                        Text("Cola de Auditoría en Vivo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text("${pendingPosts.size} publicaciones en revisión", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
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
                        Text("Prioritario", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer))
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
            listOf("FIFO (Por defecto)", "Urgentes", "Plomería", "Electricidad", "Cerrajería").forEach { f ->
                val isSelected = moderationFilter.startsWith(f.substringBefore(" ("), ignoreCase = true)
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setFilter(f) },
                    label = { Text(f) },
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
                    Text("¡Cola de auditoría al día!", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("No hay publicaciones pendientes de revisión por ahora.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
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
                            Text("Por verificar", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(post.category, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                Text("En cola: 15 min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
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
                    Text("Por ${post.providerName} · ${post.location}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
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
                    Text("DNI validado · Sin antecedentes · Matrícula técnica adjunta", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text("Auditar Detalle", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }

                FilledTonalButton(
                    onClick = onApprove,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Aprobar", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
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
    var notesList by remember { mutableStateOf(listOf("El número de carnet de instalador fue cotejado con el registro SEC. Pendiente confirmar si el rango tarifario incluye IVA.")) }
    var showDetailMenu by remember { mutableStateOf(false) }
    var showGuidelines by remember { mutableStateOf(false) }

    if (post == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Publicación no encontrada")
        }
        return
    }

    val reports by viewModel.reports.collectAsStateWithLifecycle()
    val providerPosts = posts.filter { it.providerName == post.providerName }
    val providerApproved = providerPosts.count { it.status == PostStatus.APPROVED }
    val providerRejected = providerPosts.count { it.status == PostStatus.REJECTED }
    val postReportCount = reports.filter { it.targetPostId == post.id }.sumOf { it.reportCount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Auditoría de Publicación", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showDetailMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
                        }
                        DropdownMenu(expanded = showDetailMenu, onDismissRequest = { showDetailMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Criterios de revisión") },
                                onClick = { showDetailMenu = false; showGuidelines = true }
                            )
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
                            Text("Rechazar", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        Button(
                            onClick = { viewModel.approvePost(post.id); onBack() },
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Aprobar", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
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
                            onClick = { viewModel.resolveReportsForPost(post.id); onBack() },
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) { Text("Mantener activa") }
                        Button(
                            onClick = {
                                viewModel.rejectPost(post.id, "Incumplimiento confirmado", "Publicación retirada tras revisar las denuncias.")
                                viewModel.resolveReportsForPost(post.id)
                                onBack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) { Text("Retirar publicación") }
                    }
                }
                PostStatus.REJECTED, PostStatus.FINISHED -> Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                ) {
                    Button(
                        onClick = { viewModel.resolveReportsForPost(post.id); onBack() },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).height(48.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) { Text("Cerrar reporte") }
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
                            Text("Estado de Auditoría", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(
                                when (post.status) {
                                    PostStatus.PENDING_VERIFICATION -> "Pendiente de revisión"
                                    PostStatus.APPROVED -> "Aprobada"
                                    PostStatus.REJECTED -> "Rechazada"
                                    PostStatus.FINISHED -> "Finalizada"
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
                            if (post.status == PostStatus.PENDING_VERIFICATION) "En cola" else "Historial",
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
                    Text("Sin imágenes adjuntas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
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
                                Text("RT", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                            }
                            Column {
                                Text(post.providerName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                Text("Registrado: 14 Feb 2023", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
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
                            Text("Aprobadas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(providerRejected.toString(), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.error)
                            Text("Rechazos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(postReportCount.toString(), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                            Text("Reportes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
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
                        Text("Criterios y registro de revisión", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)

                    Text("Bitácora y Notas del Moderador", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))

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
                            placeholder = { Text("Escribe apunte de revisión...", style = MaterialTheme.typography.bodySmall) },
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
                            Icon(Icons.Default.Send, contentDescription = "Guardar nota", tint = MaterialTheme.colorScheme.primary)
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
            onDismiss = { showRejectSheet = false },
            onConfirm = { reason, notes ->
                viewModel.rejectPost(post.id, reason, notes)
                showRejectSheet = false
                onBack()
            }
        )
    }

    if (showGuidelines) {
        AlertDialog(
            onDismissRequest = { showGuidelines = false },
            title = { Text("Criterios de revisión") },
            text = { Text("Revisa que la descripción sea clara, que el precio no solicite pagos previos fuera de la plataforma y que el contenido respete las políticas comunitarias. Las imágenes y verificaciones externas no están incluidas en esta fase.") },
            confirmButton = { TextButton(onClick = { showGuidelines = false }) { Text("Entendido") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RejectionReasonBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (reason: String, notes: String) -> Unit
) {
    var selectedReason by remember { mutableStateOf("Precio no concordante") }
    var notes by remember { mutableStateOf("") }

    val reasons = listOf(
        "Precio no concordante",
        "Servicio no autorizado",
        "Imágenes no pertinentes o de baja calidad",
        "Información de contacto en descripción",
        "Ubicación o cobertura inconsistente"
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
                    Text("Motivo del Rechazo", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Text("Selecciona una justificación obligatoria para notificar al autor", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar")
                }
            }

            Text("Opciones Predefinidas", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                reasons.forEach { r ->
                    val isSelected = selectedReason == r
                    Surface(
                        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = r }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedReason = r }
                            )
                            Text(r, style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal))
                        }
                    }
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Detalle adicional para el autor (opcional)") },
                placeholder = { Text("Ej. Por favor especifica si el costo incluye materiales...") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

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
                Text("Confirmar Rechazo", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun ModerationReportsScreen(
    viewModel: ModerationViewModel,
    onOpenNotifications: () -> Unit,
    onOpenDetail: (String) -> Unit,
    onOpenChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val reports by viewModel.reports.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf("Todos") }
    val filterOptions = listOf("Todos", "Fraude/Estafa", "Contacto no permitido", "Contenido ofensivo")
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
            Text("Reportes de Denuncias", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            IconButton(onClick = onOpenNotifications) {
                Icon(Icons.Outlined.Notifications, contentDescription = "Notificaciones")
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
            filterOptions.forEach { filter ->
                val isSelected = selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filter },
                    label = { Text(if (filter == "Todos") "Todos (${reports.size})" else "$filter (${reports.count { matchesReportFilter(it, filter) }})") },
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        if (filteredReports.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Sin reportes pendientes", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
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
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    Text(r.reasonTitle, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("${r.reportCount} denuncias", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
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
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { viewModel.dismissReport(r.id) }) {
                                    Text("Descartar")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        when {
                                            r.targetPostId != null -> onOpenDetail(r.targetPostId)
                                            r.targetChatId != null -> onOpenChat(r.targetChatId)
                                            else -> viewModel.dismissReport(r.id)
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(if (r.targetPostId == null && r.targetChatId == null) "Cerrar" else "Revisar incidencia", style = MaterialTheme.typography.labelSmall)
                                }
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
            Text("Estadísticas", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            IconButton(onClick = onOpenNotifications) {
                Icon(Icons.Outlined.Notifications, contentDescription = "Alertas")
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
                        text = range,
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
                        Text("En tiempo real", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }

                Text(rangePosts.size.toString(), style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                Text("Publicaciones Nuevas", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text("Registros en memoria ingresados ${statsRange.lowercase()} en Armenia y Quindío", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }

        // Operational 2x2 Grid
        Text("Desglose Operativo", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCardTile(
                icon = Icons.Default.CheckCircle,
                count = approvedCount.toString(),
                label = "Aprobadas",
                iconColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            StatCardTile(
                icon = Icons.Default.Cancel,
                count = rejectedCount.toString(),
                label = "Rechazadas",
                iconColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCardTile(
                icon = Icons.Default.Flag,
                count = rangeReports.size.toString(),
                label = "Reportadas",
                iconColor = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
            StatCardTile(
                icon = Icons.Default.PendingActions,
                count = pendingCount.toString(),
                label = "Pendientes",
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
    onSwitchToUser: () -> Unit,
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
            Text("Perfil de Staff", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            IconButton(onClick = onOpenNotifications) {
                BadgedBox(badge = { if (notificationsEnabled && notifications.count { !it.read } > 0) Badge { Text(notifications.count { !it.read }.toString()) } }) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "Notificaciones")
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
                            Text("AV", style = MaterialTheme.typography.titleLarge.copy(color = Color.White, fontWeight = FontWeight.Bold))
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
                            Text("Staff ServiLocal · ID: ${moderatorUser.staffId}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
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
                        Text("Disponibilidad en Cola", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text(
                            text = if (moderatorUser.activeShift) "Turno activo · ${moderatorUser.workingHours}" else "Turno pausado · ${moderatorUser.workingHours}",
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

        // Return to User Mode Button (for seamless evaluation of both user and moderator flows)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("Modo Usuario", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text("Volver a la vista de cliente/proveedor", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Button(
                    onClick = onSwitchToUser,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cambiar")
                }
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
                    title = "Ajustes de Notificaciones de Auditoría",
                    subtitle = "Alertas por infracciones críticas en cola",
                    onClick = { activeInfoDialog = "Ajustes de Notificaciones de Auditoría" }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.padding(horizontal = 14.dp))
                ProfileNavRow(
                    icon = Icons.Default.MenuBook,
                    title = "Políticas Comunitarias y Guía v4.2",
                    subtitle = "Criterios de revocación y sanciones",
                    onClick = { activeInfoDialog = "Políticas Comunitarias y Guía v4.2" }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.padding(horizontal = 14.dp))
                ProfileNavRow(
                    icon = Icons.Default.HistoryToggleOff,
                    title = "Historial de Decisiones Propias",
                    subtitle = "Registro de auditoría de los últimos 90 días",
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
            Text("Cerrar Sesión de Staff", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    activeInfoDialog?.let { title ->
        AlertDialog(
            onDismissRequest = { activeInfoDialog = null },
            title = { Text(title) },
            text = {
                when (title) {
                    "Ajustes de Notificaciones de Auditoría" -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Alertas dentro de la aplicación", modifier = Modifier.weight(1f))
                        Switch(checked = notificationsEnabled, onCheckedChange = onNotificationsChange)
                    }
                    "Políticas Comunitarias y Guía v4.2" -> Text("No se permiten pagos previos fuera de ServiLocal, datos de contacto en publicaciones ni contenido ofensivo. Las decisiones se registran en memoria durante esta demostración.")
                    else -> Text("El historial de decisiones se conserva en memoria mientras la aplicación está abierta. En esta demostración aún no hay un registro persistente.")
                }
            },
            confirmButton = { TextButton(onClick = { activeInfoDialog = null }) { Text("Listo") } }
        )
    }
}
