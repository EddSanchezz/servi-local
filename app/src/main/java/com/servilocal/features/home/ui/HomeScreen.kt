package com.servilocal.features.home.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings

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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material3.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.servilocal.domain.model.ServicePost
import com.servilocal.R
import com.servilocal.features.app.viewmodel.AppUiState
import com.servilocal.core.ui.localizedDistance
import com.servilocal.core.ui.localizedServiceCategory
import com.servilocal.core.ui.components.ProfilePhotoAvatar
import com.servilocal.core.ui.components.PostCardHeaderSlot
import com.servilocal.core.ui.components.UnifiedPostCard
import coil.compose.AsyncImage
import com.servilocal.features.home.viewmodel.HomeViewModel
import com.servilocal.features.home.viewmodel.ViewDisplayMode
import com.servilocal.features.posts.coordinator.PublishPostEvent
import com.servilocal.domain.usecase.posts.FilterPostsByLocationUseCase
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

private data class CategoryOption(val key: String, val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector)

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
    val maxDistanceKm = uiState.maxDistanceKm
    val originLatitude = uiState.originLatitude
    val originLongitude = uiState.originLongitude
    val viewDisplayMode = uiState.displayMode
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val notificationsEnabled = appUiState.notificationsEnabled
    val unreadNotifs = if (notificationsEnabled) notifications.count { !it.isRead } else 0

    val categories = listOf(
        CategoryOption("Todos", R.string.category_all, Icons.Default.Check),
        CategoryOption("Hogar", R.string.category_home, Icons.Default.Roofing),
        CategoryOption("Educación", R.string.category_education, Icons.Default.School),
        CategoryOption("Mascotas", R.string.category_pets, Icons.Default.Pets),
        CategoryOption("Tecnología", R.string.category_technology, Icons.Default.Devices),
        CategoryOption("Electricidad", R.string.category_electricity, Icons.Default.Bolt),
        CategoryOption("Transporte", R.string.category_transport, Icons.Default.LocalShipping)
    )

    var distanceMenuExpanded by remember { mutableStateOf(false) }
    var locationDialogVisible by remember { mutableStateOf(false) }
    var permissionRequested by remember { mutableStateOf(false) }
    var pendingMapActivation by remember { mutableStateOf(false) }
    var pendingDistanceSelection by remember { mutableStateOf(false) }
    var mapRecenterRequest by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.setLocationQuery("") }

    LaunchedEffect(Unit) {
        viewModel.publicationEvents.collect { event ->
            when (event) {
                is PublishPostEvent.Success -> snackbarHostState.showSnackbar(context.getString(R.string.publication_sent_review))
                is PublishPostEvent.Failure -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = context.getString(R.string.retry)
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.retryPublication()
                    }
                }
                is PublishPostEvent.Progress -> Unit
            }
        }
    }

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun isLocationEnabled(): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return runCatching {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) manager.isLocationEnabled
            else manager.isProviderEnabled(LocationManager.GPS_PROVIDER) || manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }.getOrDefault(false)
    }

    fun refreshLocationInBackground(recenterMap: Boolean = false) {
        if (recenterMap) mapRecenterRequest++
        viewModel.refreshLocationInBackground()
    }

    fun activateMapIfReady() {
        if (!hasLocationPermission() || !isLocationEnabled()) {
            pendingMapActivation = true
            locationDialogVisible = true
        } else {
            viewModel.setDisplayMode(ViewDisplayMode.MAP)
            pendingMapActivation = false
            locationDialogVisible = false
            refreshLocationInBackground()
        }
    }

    fun openDistanceFilter() {
        if (!hasLocationPermission() || !isLocationEnabled()) {
            pendingDistanceSelection = true
            locationDialogVisible = true
            return
        }
        distanceMenuExpanded = true
        pendingDistanceSelection = false
        refreshLocationInBackground()
    }

    val locationSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (pendingMapActivation && hasLocationPermission() && isLocationEnabled()) activateMapIfReady()
        else if (pendingDistanceSelection && hasLocationPermission() && isLocationEnabled()) openDistanceFilter()
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it } && isLocationEnabled()) {
            if (pendingMapActivation) activateMapIfReady() else openDistanceFilter()
        } else {
            locationDialogVisible = true
        }
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
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.home_title),
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
                        contentDescription = stringResource(R.string.notifications),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Search bar and distance control
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = {
                    Text(
                        stringResource(R.string.search_services_short),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
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
                    .weight(1f)
                    .height(48.dp)
                    .testTag("search_services_input")
            )
            Box {
                FilledTonalIconButton(
                    onClick = ::openDistanceFilter,
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (maxDistanceKm != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (maxDistanceKm != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Rounded.NearMe, contentDescription = stringResource(R.string.distance))
                }
                DropdownMenu(expanded = distanceMenuExpanded, onDismissRequest = { distanceMenuExpanded = false }) {
                    listOf(null, 1.0, 5.0, 10.0).forEach { distance ->
                        DropdownMenuItem(
                            text = { Text(distance?.let { stringResource(R.string.distance_up_to, it.toInt()) } ?: stringResource(R.string.any_distance), maxLines = 1) },
                            onClick = { viewModel.setMaxDistanceKm(distance); distanceMenuExpanded = false }
                        )
                    }
                }
            }
        }

        // Category Filter Chips (Horizontal Scroll)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories, key = { it.key }) { cat ->
                val isSelected = selectedCategory == cat.key
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setSelectedCategory(cat.key) },
                    label = { Text(stringResource(cat.labelRes), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    leadingIcon = {
                        Icon(cat.icon, contentDescription = null, modifier = Modifier.size(16.dp))
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

        // Main view selector
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = viewDisplayMode == ViewDisplayMode.LIST,
                    onClick = { viewModel.setDisplayMode(ViewDisplayMode.LIST) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = { Icon(Icons.Default.FormatListBulleted, contentDescription = null) }
                ) { Text(stringResource(R.string.list_view), maxLines = 1) }
                SegmentedButton(
                    selected = viewDisplayMode == ViewDisplayMode.MAP,
                    onClick = ::activateMapIfReady,
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = { Icon(Icons.Default.Map, contentDescription = null) }
                ) { Text(stringResource(R.string.map_view), maxLines = 1) }
            }
        }

        AnimatedContent(
            targetState = viewDisplayMode,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "home_contextual_filters"
        ) { mode ->
            if (mode == ViewDisplayMode.MAP) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.NearMe, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Mostrando servicios cercanos", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else Spacer(Modifier.height(0.dp))
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
                            text = stringResource(R.string.no_services_found),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.try_other_search),
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
                            isOwner = post.providerId == currentUser.id
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
                onPostSelected = { onOpenServiceDetail(it.id) },
                recenterRequest = mapRecenterRequest,
                onRecenter = {
                    if (hasLocationPermission() && isLocationEnabled()) refreshLocationInBackground(recenterMap = true)
                    else { pendingMapActivation = true; locationDialogVisible = true }
                }
            )
        }
      }
      SnackbarHost(
          hostState = snackbarHostState,
          modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
      )
      if (locationDialogVisible) {
          AlertDialog(
              onDismissRequest = { locationDialogVisible = false; pendingMapActivation = false; pendingDistanceSelection = false },
              title = { Text("Ubicación necesaria") },
              text = { Text("Esta funcionalidad no está disponible sin ubicación. Activa la ubicación de tu dispositivo para explorar servicios cercanos en el mapa.") },
              confirmButton = {
                  TextButton(onClick = {
                      if (!hasLocationPermission()) {
                          val activity = context.findActivity()
                          val fineRationale = activity?.let { androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_FINE_LOCATION) } ?: true
                          val coarseRationale = activity?.let { androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_COARSE_LOCATION) } ?: true
                          if (permissionRequested && !fineRationale && !coarseRationale) {
                              val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                              locationSettingsLauncher.launch(intent)
                          } else {
                              permissionRequested = true
                              locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                          }
                      } else {
                          locationSettingsLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                      }
                  }) { Text("Reintentar") }
              },
              dismissButton = {
                  TextButton(onClick = { locationDialogVisible = false; pendingMapActivation = false; pendingDistanceSelection = false }) { Text("Cancelar") }
              }
          )
      }
    }
}

@Composable
fun ServiceCardItem(
    post: ServicePost,
    onDetailClick: () -> Unit,
    onUpvoteClick: () -> Unit,
    isOwner: Boolean
) {
    UnifiedPostCard(
        post = post,
        onClick = onDetailClick,
        headerSlot = if (isOwner) {
            PostCardHeaderSlot.HomeOwner(post.upvotesCount)
        } else {
            PostCardHeaderSlot.HomeOtherUser(
                likesCount = post.upvotesCount,
                isLiked = post.userUpvoted,
                onLikeClick = onUpvoteClick
            )
        }
    )
}

@Composable
fun InteractiveMapPreview(
    posts: List<ServicePost>,
    originLatitude: Double?,
    originLongitude: Double?,
    onPostSelected: (ServicePost) -> Unit,
    recenterRequest: Int,
    onRecenter: () -> Unit
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
                        title = context.getString(R.string.your_location)
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
                            title = if (cluster.size == 1) cluster.first().title else context.getString(R.string.nearby_services_count, cluster.size)
                            snippet = if (cluster.size == 1) {
                                "${cluster.first().providerName} · ${cluster.first().distance}"
                            } else context.getString(R.string.tap_to_zoom_map)
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
                if (recenterRequest > 0) map.controller.animateTo(center) else map.controller.setCenter(center)
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
                if (originLatitude == null) stringResource(R.string.services_near_you) else stringResource(R.string.services_near_your_location),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        SmallFloatingActionButton(
            onClick = onRecenter,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Centrar en mi ubicación")
        }
    }
}

private tailrec fun Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
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
