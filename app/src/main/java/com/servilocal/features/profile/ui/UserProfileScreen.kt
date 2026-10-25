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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.servilocal.core.ui.components.ProfileNavRow
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
    onDarkThemeChange: (Boolean) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val profileUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val badges by viewModel.badges.collectAsStateWithLifecycle()
    val stats = profileUiState.stats
    val darkTheme = appUiState.darkTheme
    val notificationsEnabled = appUiState.notificationsEnabled
    var activeDialog by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var editName by remember(currentUser.id) { mutableStateOf(currentUser.name) }
    var editEmail by remember(currentUser.id) { mutableStateOf(currentUser.email) }
    var editValidationMessage by remember { mutableStateOf(false) }
    val ownedPosts = posts.filter { it.providerId == currentUser.id || it.providerName == currentUser.name }
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
                title = { Text("Perfil", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                actions = {
                    IconButton(onClick = onOpenNotifications) {
                        BadgedBox(badge = { if (notificationsEnabled && notifications.count { !it.isRead } > 0) Badge { Text(notifications.count { !it.isRead }.toString()) } }) {
                            Icon(Icons.Outlined.Notifications, contentDescription = "Notificaciones")
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
                                Icon(Icons.Default.Edit, contentDescription = "Editar foto", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
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
                            Text("$points pts acumulados", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
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
                                Text("$points / $nextLevelPoints pts", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                Text("Nivel actual: $levelLabel", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
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
                                Text("${(progress * 100).roundToInt()}% completado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text("Faltan $remainingPoints pts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
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
                            label = "Publicaciones",
                            modifier = Modifier.weight(1f)
                        )
                        ProfileStatItem(
                            value = "${stats?.pendingPosts ?: ownedPosts.count { it.status == com.servilocal.domain.model.PostStatus.PENDING_VERIFICATION }}",
                            label = "Pendientes",
                            modifier = Modifier.weight(1f)
                        )
                        ProfileStatItem(
                            value = "${stats?.badgeCount ?: displayBadges.count { it.unlocked }}",
                            label = "Insignias",
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
                            Text("Insignias", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("Ver todas", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
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
                                            text = badge.title,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                            color = if (badge.unlocked) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.outline
                                        )
                                        if (!badge.unlocked) {
                                            Icon(Icons.Default.Lock, contentDescription = "Bloqueada", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
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

            // Mi Actividad
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Mi Actividad",
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
                            title = "Editar Perfil",
                            subtitle = "Datos personales y contacto",
                            onClick = { activeDialog = "Editar Perfil" }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.padding(horizontal = 14.dp))
                        ProfileNavRow(
                            icon = Icons.AutoMirrored.Filled.Assignment,
                            title = "Mis Publicaciones",
                            subtitle = "Gestionar servicios activos e inactivos",
                            onClick = { activeDialog = "Mis Publicaciones" }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.padding(horizontal = 14.dp))
                        ProfileNavRow(
                            icon = Icons.Default.ThumbUp,
                            title = "Servicios que me importan / Mis Votos",
                            subtitle = "Profesionales que has apoyado en la comunidad",
                            onClick = { activeDialog = "Servicios que me importan / Mis Votos" }
                        )
                    }
                }
            }

            // Configuración
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Configuración",
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
                            title = "Ajustes de la Aplicación",
                            subtitle = "Notificaciones, tema claro/oscuro e idioma",
                            onClick = { activeDialog = "Ajustes de la Aplicación" }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.padding(horizontal = 14.dp))
                        ProfileNavRow(
                            icon = Icons.Default.Lock,
                            title = "Privacidad y Seguridad",
                            subtitle = "Gestión de datos y visibilidad de contacto",
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
                    Text("Cerrar Sesión", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }

                TextButton(onClick = { showDeleteConfirmation = true }) {
                    Text(
                        "Eliminar Cuenta",
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
            title = { Text(title) },
            text = {
                when (title) {
                    "Editar Perfil" -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Nombre completo") },
                            isError = editValidationMessage && editName.isBlank(),
                            supportingText = { if (editValidationMessage && editName.isBlank()) Text("El nombre es obligatorio.") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editEmail,
                            onValueChange = { editEmail = it },
                            label = { Text("Correo electrónico") },
                            isError = editValidationMessage && !editEmail.trim().matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")),
                            supportingText = { if (editValidationMessage && !editEmail.trim().matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) Text("Ingresa un correo válido.") },
                            singleLine = true
                        )
                    }
                    "Horario de atención" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Disponibilidad para coordinar servicios", style = MaterialTheme.typography.bodyMedium)
                        Text(currentUser.workingHours, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Text("Confirma la disponibilidad directamente con cada profesional antes de coordinar una visita.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    "Mis Publicaciones", "Servicios que me importan / Mis Votos" -> {
                        if (dialogPosts.isEmpty()) {
                            Text(if (title == "Mis Publicaciones") "Aún no tienes publicaciones. Crea una desde la pestaña Publicar." else "Todavía no has apoyado publicaciones.")
                        } else {
                            Column(
                                modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                dialogPosts.forEach { post ->
                                    val status = when (post.status) {
                                        com.servilocal.domain.model.PostStatus.PENDING_VERIFICATION -> "Pendiente de revisión"
                                        com.servilocal.domain.model.PostStatus.APPROVED -> "Publicada"
                                        com.servilocal.domain.model.PostStatus.REJECTED -> "Rechazada"
                                        com.servilocal.domain.model.PostStatus.FINISHED -> "Finalizada"
                                    }
                                    TextButton(
                                        onClick = {
                                            activeDialog = null
                                            onOpenServiceDetail(post.id)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Text(post.title, color = MaterialTheme.colorScheme.onSurface)
                                            Text(status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "Ajustes de la Aplicación" -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tema oscuro", modifier = Modifier.weight(1f))
                            Switch(checked = darkTheme, onCheckedChange = onDarkThemeChange)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Notificaciones dentro de la app", modifier = Modifier.weight(1f))
                            Switch(checked = notificationsEnabled, onCheckedChange = onNotificationsChange)
                        }
                        Text("Idioma: Español", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    else -> Text("Esta versión mantiene los datos solo en memoria durante el uso de la aplicación. No se envían a un servidor desde este prototipo.")
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
                    ) { Text("Guardar") }
                } else {
                    TextButton(onClick = { activeDialog = null }) { Text("Listo") }
                }
            },
            dismissButton = {
                if (title == "Editar Perfil") {
                    TextButton(onClick = { activeDialog = null }) { Text("Cancelar") }
                }
            }
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Eliminar cuenta") },
            text = { Text("Esta es una cuenta de demostración sin almacenamiento ni sesión persistente. Puedes cerrar la sesión para regresar a la pantalla de acceso.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirmation = false; onLogout() }) { Text("Cerrar sesión") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancelar") } }
        )
    }
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

