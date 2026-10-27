package com.servilocal.features.home.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.servilocal.domain.model.ServicePost
import com.servilocal.features.app.viewmodel.AppUiState
import com.servilocal.features.home.viewmodel.HomeViewModel
import com.servilocal.features.home.viewmodel.ViewDisplayMode
import com.servilocal.domain.usecase.posts.FilterPostsByLocationUseCase
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    appUiState: AppUiState,
    onOpenNotifications: () -> Unit,
    onOpenServiceDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredPosts by viewModel.filteredPosts.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery = uiState.searchQuery
    val selectedCategory = uiState.selectedCategory
    val locationQuery = uiState.locationQuery
    val maxDistanceKm = uiState.maxDistanceKm
    val originLatitude = uiState.originLatitude
    val originLongitude = uiState.originLongitude
    val viewDisplayMode = uiState.displayMode
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val notificationsEnabled = appUiState.notificationsEnabled
    val unreadNotifs = if (notificationsEnabled) notifications.count { !it.isRead } else 0

    val categories = listOf(
        Pair("Todos", Icons.Default.Check),
        Pair("Hogar", Icons.Default.Roofing),
        Pair("Educación", Icons.Default.School),
        Pair("Mascotas", Icons.Default.Pets),
        Pair("Tecnología", Icons.Default.Devices),
        Pair("Electricidad", Icons.Default.Bolt),
        Pair("Transporte", Icons.Default.LocalShipping)
    )

    var distanceMenuExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    fun loadDeviceLocation() {
        requestCurrentLocation(context) { location ->
            if (location == null) {
                coroutineScope.launch { snackbarHostState.showSnackbar("No se encontró una ubicación. Inténtalo en un área abierta.") }
            } else {
                viewModel.setOriginLocation(location.latitude, location.longitude)
                coroutineScope.launch { snackbarHostState.showSnackbar("Ubicación GPS actualizada.") }
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) loadDeviceLocation()
        else coroutineScope.launch { snackbarHostState.showSnackbar("Permite el acceso a la ubicación para buscar cerca de ti.") }
    }

    Box(modifier = modifier.fillMaxSize()) {
      Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
      ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HomeRepairService,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "ServiLocal",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Home",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Notifications button with badge
            IconButton(
                onClick = onOpenNotifications,
                modifier = Modifier.testTag("notification_button")
            ) {
                BadgedBox(
                    badge = {
                        if (unreadNotifs > 0) {
                            Badge(containerColor = MaterialTheme.colorScheme.error) {
                                Text(unreadNotifs.toString())
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Notificaciones",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Search Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Buscar servicios (ej. plomero, mudanzas...)", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_services_input")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = locationQuery,
                onValueChange = viewModel::setLocationQuery,
                placeholder = { Text("Barrio o zona") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                trailingIcon = {
                    if (locationQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setLocationQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar ubicación")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f).testTag("search_location_input")
            )
            IconButton(
                onClick = {
                    val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    if (fineGranted || coarseGranted) loadDeviceLocation()
                    else locationPermissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    )
                },
                modifier = Modifier.testTag("use_device_location_button")
            ) {
                Icon(
                    imageVector = if (originLatitude != null) Icons.Default.MyLocation else Icons.Default.GpsFixed,
                    contentDescription = "Usar ubicación GPS",
                    tint = if (originLatitude != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                FilterChip(
                    selected = maxDistanceKm != null,
                    onClick = { distanceMenuExpanded = true },
                    label = { Text(maxDistanceKm?.let { "${it.toInt()} km" } ?: "Distancia") },
                    leadingIcon = { Icon(Icons.Default.NearMe, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                DropdownMenu(
                    expanded = distanceMenuExpanded,
                    onDismissRequest = { distanceMenuExpanded = false }
                ) {
                    listOf(null, 1.0, 3.0, 5.0, 10.0).forEach { distance ->
                        DropdownMenuItem(
                            text = { Text(distance?.let { "Hasta ${it.toInt()} km" } ?: "Cualquier distancia") },
                            onClick = {
                                viewModel.setMaxDistanceKm(distance)
                                distanceMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Category Filter Chips (Horizontal Scroll)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { (cat, icon) ->
                val isSelected = selectedCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setSelectedCategory(cat) },
                    label = { Text(cat, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    leadingIcon = {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = null
                )
            }
        }

        // Segmented View Toggle: Lista vs Mapa
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(3.dp)
            ) {
                // List Mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (viewDisplayMode == ViewDisplayMode.LIST) MaterialTheme.colorScheme.surfaceContainerLowest else Color.Transparent)
                        .clickable { viewModel.setDisplayMode(ViewDisplayMode.LIST) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            Icons.Default.FormatListBulleted,
                            contentDescription = null,
                            tint = if (viewDisplayMode == ViewDisplayMode.LIST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Lista",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (viewDisplayMode == ViewDisplayMode.LIST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Map Mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (viewDisplayMode == ViewDisplayMode.MAP) MaterialTheme.colorScheme.surfaceContainerLowest else Color.Transparent)
                        .clickable { viewModel.setDisplayMode(ViewDisplayMode.MAP) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            Icons.Default.Map,
                            contentDescription = null,
                            tint = if (viewDisplayMode == ViewDisplayMode.MAP) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Mapa",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (viewDisplayMode == ViewDisplayMode.MAP) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Main Content: List or Map View
        if (viewDisplayMode == ViewDisplayMode.LIST) {
            if (filteredPosts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No se encontraron servicios",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Prueba con otros términos o selecciona otra categoría.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                ) {
                    items(filteredPosts, key = { it.id }) { post ->
                        ServiceCardItem(
                            post = post,
                            onDetailClick = { onOpenServiceDetail(post.id) },
                            onUpvoteClick = { viewModel.toggleUpvote(post.id) },
                            showUpvote = post.providerId != currentUser.id
                        )
                    }
                }
            }
        } else {
            // Interactive Map View of Armenia, Quindío
            InteractiveMapPreview(
                posts = filteredPosts,
                originLatitude = originLatitude,
                originLongitude = originLongitude,
                onPostSelected = { onOpenServiceDetail(it.id) }
            )
        }
      }
      SnackbarHost(
          hostState = snackbarHostState,
          modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
      )
    }
}

@Composable
fun ServiceCardItem(
    post: ServicePost,
    onDetailClick: () -> Unit,
    onUpvoteClick: () -> Unit,
    showUpvote: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDetailClick)
            .testTag("service_card_${post.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Provider Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Provider Avatar
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = post.providerName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = post.providerName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = post.providerLevel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(
                                Icons.Default.Navigation,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${post.distance} · ${post.location}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Favorite / Upvote Heart Icon (hidden on the user's own posts)
                if (showUpvote) {
                    IconButton(onClick = onUpvoteClick) {
                        Icon(
                            imageVector = if (post.userUpvoted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Me interesa",
                            tint = if (post.userUpvoted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Service Image Banner Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
            ) {
                // Background icon aesthetic
                Icon(
                    imageVector = when (post.category) {
                        "Hogar" -> Icons.Default.Roofing
                        "Educación" -> Icons.Default.School
                        "Mascotas" -> Icons.Default.Pets
                        "Electricidad" -> Icons.Default.Bolt
                        else -> Icons.Default.Handyman
                    },
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier
                        .size(120.dp)
                        .align(Alignment.CenterEnd)
                        .offset(x = 20.dp)
                )

                // Overlay details
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(14.dp))
                            Text("Disponible Ahora", style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.SemiBold))
                        }
                    }

                    Text(
                        text = post.description,
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.95f)),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title and Price
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "Desde $ ${post.minPrice} COP / servicio",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Rating and Upvotes
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                Text(
                    text = "${post.rating}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "(${post.reviewCount} reseñas)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text("•", color = MaterialTheme.colorScheme.outline)
                Icon(Icons.Default.ThumbUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                Text(
                    text = "${post.upvotesCount} Me interesa",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Attribute Chips
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = post.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                if (post.warrantyMonths > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Garantía ${post.warrantyMonths} meses",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveMapPreview(
    posts: List<ServicePost>,
    originLatitude: Double?,
    originLongitude: Double?,
    onPostSelected: (ServicePost) -> Unit
) {
    val context = LocalContext.current
    val center = GeoPoint(
        originLatitude ?: FilterPostsByLocationUseCase.TEST_ORIGIN_LATITUDE,
        originLongitude ?: FilterPostsByLocationUseCase.TEST_ORIGIN_LONGITUDE
    )
    val mapView = remember(context) {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            // Carto Voyager requires a provider token in current deployments.
            // Keep OSM as a working fallback until CARTO_API_KEY is configured.
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setBuiltInZoomControls(true)
            zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
            controller.setZoom(13.5)
            controller.setCenter(center)
        }
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp).clip(RoundedCornerShape(20.dp))
    ) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { map ->
                map.overlays.removeAll(map.overlays.filterIsInstance<Marker>())
                if (originLatitude != null && originLongitude != null) {
                    map.overlays.add(Marker(map).apply {
                        position = center
                        title = "Tu ubicación"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    })
                }
                posts.filter { it.latitude != null && it.longitude != null }
                    .groupBy { post ->
                        // A geographic grid clusters nearby services at the current zoom.
                        (post.latitude!! * 100).toInt() to (post.longitude!! * 100).toInt()
                    }
                    .values
                    .forEach { cluster ->
                        val point = GeoPoint(
                            cluster.mapNotNull { it.latitude }.average(),
                            cluster.mapNotNull { it.longitude }.average()
                        )
                        map.overlays.add(Marker(map).apply {
                            position = point
                            title = if (cluster.size == 1) cluster.first().title else "${cluster.size} servicios cercanos"
                            snippet = if (cluster.size == 1) {
                                "${cluster.first().providerName} · ${cluster.first().distance}"
                            } else "Toca para acercar el mapa"
                            icon = createMapMarkerIcon(context, cluster.size)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            setOnMarkerClickListener { clicked, _ ->
                                if (cluster.size == 1) {
                                    onPostSelected(cluster.first())
                                } else {
                                    clicked.closeInfoWindow()
                                    map.controller.animateTo(point)
                                    map.controller.setZoom((map.zoomLevelDouble + 2.0).coerceAtMost(19.0))
                                }
                                true
                            }
                        })
                    }
                map.controller.setCenter(center)
                map.invalidate()
            }
        )
        Surface(
            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                "© OpenStreetMap · CartoDB",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
            )
        }
        Surface(
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(
                if (originLatitude == null) "Servicios cerca de ti" else "Servicios cerca de tu ubicación",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

private fun createMapMarkerIcon(context: Context, count: Int): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val width = (count.toString().length * 24 + 56) * density
    val height = 64 * density
    val bitmap = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(0, 96, 104)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(width / 2f, height / 2f - 8 * density, 24 * density, paint)
    paint.color = android.graphics.Color.WHITE
    paint.textAlign = Paint.Align.CENTER
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 15 * density
    canvas.drawText(count.toString(), width / 2f, height / 2f - 3 * density, paint)
    paint.color = android.graphics.Color.rgb(0, 96, 104)
    val pin = Path().apply {
        moveTo(width / 2f - 9 * density, height / 2f + 10 * density)
        lineTo(width / 2f + 9 * density, height / 2f + 10 * density)
        lineTo(width / 2f, height - 2 * density)
        close()
    }
    canvas.drawPath(pin, paint)
    return BitmapDrawable(context.resources, bitmap)
}

@SuppressLint("MissingPermission", "DEPRECATION")
private fun requestCurrentLocation(context: Context, onResult: (Location?) -> Unit) {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val provider = when {
        manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> null
    }
    if (provider == null) {
        onResult(null)
        return
    }

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            manager.getCurrentLocation(provider, null, context.mainExecutor) { location -> onResult(location) }
        } else {
            val lastKnown = manager.getLastKnownLocation(provider)
            if (lastKnown != null) {
                onResult(lastKnown)
            } else {
                manager.requestSingleUpdate(provider, object : LocationListener {
                    override fun onLocationChanged(location: Location) = onResult(location)
                    override fun onProviderEnabled(provider: String) = Unit
                    override fun onProviderDisabled(provider: String) = onResult(null)
                }, Looper.getMainLooper())
            }
        }
    } catch (_: SecurityException) {
        onResult(null)
    }
}
