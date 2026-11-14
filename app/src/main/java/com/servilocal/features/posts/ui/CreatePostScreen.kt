package com.servilocal.features.posts.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.servilocal.features.posts.viewmodel.CreatePostViewModel
import com.servilocal.features.posts.viewmodel.EditPostViewModel
import com.servilocal.domain.model.ServicePost
import com.servilocal.domain.model.PostStatus
import com.servilocal.R
import com.servilocal.core.ui.components.ImagePicker
import com.servilocal.core.ui.components.PostPhotoUploader
import com.servilocal.core.ui.components.PostLocationMap
import com.servilocal.core.util.requestCurrentLocation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    viewModel: CreatePostViewModel,
    onBack: () -> Unit,
    onOpenHome: () -> Unit,
    onOpenProfile: () -> Unit,
    editPost: ServicePost? = null,
    editViewModel: EditPostViewModel? = null,
    onEditSaved: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var title by remember(editPost?.id) { mutableStateOf(editPost?.title.orEmpty()) }
    var selectedCategory by remember(editPost?.id) { mutableStateOf(editPost?.category ?: "Hogar") }
    var description by remember(editPost?.id) { mutableStateOf(editPost?.description.orEmpty()) }
    var isPriceToAgree by remember(editPost?.id) { mutableStateOf(editPost?.isPriceToAgree ?: false) }
    var minPrice by remember(editPost?.id) { mutableStateOf(if (editPost == null || editPost.isPriceToAgree) "" else editPost.minPrice.toString()) }
    var maxPrice by remember(editPost?.id) { mutableStateOf(if (editPost == null || editPost.isPriceToAgree) "" else editPost.maxPrice.toString()) }
    var location by remember(editPost?.id) { mutableStateOf(editPost?.location ?: "Armenia, Quindío") }
    var locationLatitude by rememberSaveable(editPost?.id) { mutableStateOf(editPost?.latitude) }
    var locationLongitude by rememberSaveable(editPost?.id) { mutableStateOf(editPost?.longitude) }
    val existingPhotos = remember(editPost?.id) { mutableStateListOf<String>().apply { addAll(editPost?.photos.orEmpty()) } }
    val newPhotoUris = remember(editPost?.id) { mutableStateListOf<String>() }
    var showLocationPicker by remember { mutableStateOf(false) }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    var attemptedSubmit by remember { mutableStateOf(false) }
    var activePhotoIndex by rememberSaveable { mutableIntStateOf(0) }
    var showHelpDialog by remember { mutableStateOf(false) }
    val formUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val editUiState = editViewModel?.uiState?.collectAsStateWithLifecycle()
    val attachedPhotos = if (editPost == null) formUiState.selectedImageUris else existingPhotos + newPhotoUris
    var showEditSavedConfirmation by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val canEditPost = editPost?.status in setOf(PostStatus.APPROVED, PostStatus.PENDING_VERIFICATION)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val categories = listOf(
        "Hogar" to R.string.category_home,
        "Educación" to R.string.category_education,
        "Mascotas" to R.string.category_pets,
        "Tecnología" to R.string.category_technology,
        "Transporte" to R.string.category_transport
    )
    val context = LocalContext.current

    fun requestDeviceLocation() {
        requestCurrentLocation(context) { deviceLocation ->
            if (deviceLocation == null) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.current_location_unavailable))
                }
            } else {
                locationLatitude = deviceLocation.latitude
                locationLongitude = deviceLocation.longitude
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            requestDeviceLocation()
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.location_permission_denied_manual_available))
            }
        }
    }

    LaunchedEffect(formUiState.isCreated) {
        if (formUiState.isCreated && editPost == null) {
            onOpenHome()
        }
    }

    LaunchedEffect(editUiState?.value?.isUpdated) {
        if (editUiState?.value?.isUpdated == true) showEditSavedConfirmation = true
    }
    LaunchedEffect(formUiState.submissionError) {
        formUiState.submissionError?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(editUiState?.value?.submissionError) {
        editUiState?.value?.submissionError?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        stringResource(if (editPost == null) R.string.create_publication_title else R.string.edit_post_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.Default.HelpOutline, contentDescription = stringResource(R.string.help))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Button(
                            onClick = {
                                attemptedSubmit = true
                                val parsedMin = minPrice.toIntOrNull() ?: 0
                                val parsedMax = maxPrice.toIntOrNull() ?: 0
                                val pricesAreValid = isPriceToAgree || (parsedMin > 0 && parsedMax >= parsedMin)
                                val hasPhotos = attachedPhotos.isNotEmpty()
                                if (title.isNotBlank() && description.isNotBlank() && location.isNotBlank() && pricesAreValid && locationLatitude != null && locationLongitude != null && hasPhotos) {
                                    if (editPost != null && editViewModel != null) {
                                        editViewModel.updatePost(
                                            postId = editPost.id,
                                            title = title.trim(), category = selectedCategory, description = description.trim(),
                                            minPrice = parsedMin, maxPrice = parsedMax, isPriceToAgree = isPriceToAgree,
                                            location = location.trim(), latitude = locationLatitude, longitude = locationLongitude,
                                            photos = attachedPhotos
                                        )
                                    } else {
                                    viewModel.createPost(
                                        title = title.trim(),
                                        category = selectedCategory,
                                        description = description.trim(),
                                        minPrice = parsedMin,
                                        maxPrice = parsedMax,
                                        isPriceToAgree = isPriceToAgree,
                                        location = location.trim(),
                                        latitude = locationLatitude,
                                        longitude = locationLongitude
                                    )
                                    }
                                } else {
                                    val message = when {
                                        locationLatitude == null || locationLongitude == null -> context.getString(R.string.post_location_not_selected)
                                        !hasPhotos -> context.getString(R.string.image_required_for_post)
                                        else -> context.getString(R.string.complete_required_fields)
                                    }
                                    coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                                }
                            },
                            enabled = !(editUiState?.value?.isSubmitting ?: formUiState.isSubmitting) && attachedPhotos.isNotEmpty() && (editPost == null || canEditPost),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("submit_publish_button"),
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (formUiState.isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    formUiState.uploadProgress?.let { progress ->
                                        stringResource(R.string.uploading_post_image, (progress * 100).toInt())
                                    } ?: stringResource(R.string.loading)
                                )
                            } else {
                            BadgedBox(badge = {
                                Badge { Text(attachedPhotos.size.toString()) }
                                }) {
                                    Icon(Icons.Default.Save, contentDescription = null)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(if (editPost == null) R.string.send_for_review else R.string.save_changes), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                        }
                        }
                        if (editPost?.status == PostStatus.APPROVED && editViewModel != null) {
                            OutlinedButton(
                                onClick = { showDeleteConfirmation = true },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                enabled = editUiState?.value?.isSubmitting != true
                            ) { Text(stringResource(R.string.finish_post), maxLines = 1) }
                        }
                        Text(
                            text = stringResource(R.string.publication_terms_notice),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
                        )
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            if (editPost != null) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Al guardar los cambios, tu publicación pasará nuevamente a estado Pendiente de revisión para ser verificada por un moderador.",
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Notice Banner: Quality and Trust Review
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.quality_trust_review),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.pending_review_explainer),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            ImagePicker(
                onImageSelected = {
                    if (editPost == null) viewModel.onImageSelected(it.toString())
                    else if (existingPhotos.size + newPhotoUris.size < 5) newPhotoUris.add(it.toString())
                    activePhotoIndex = (attachedPhotos.size).coerceAtMost(4)
                },
                onImagesSelected = { uris ->
                    val additions = uris.map(Uri::toString)
                    if (editPost == null) viewModel.onImagesSelected(additions)
                    else newPhotoUris.addAll(additions.take((5 - existingPhotos.size - newPhotoUris.size).coerceAtLeast(0)))
                    activePhotoIndex = (attachedPhotos.size + uris.size - 1).coerceAtMost(4)
                }
            ) { openCamera, openGallery ->
                PostPhotoUploader(
                    photos = attachedPhotos,
                    activeIndex = activePhotoIndex,
                    isUploading = editUiState?.value?.isSubmitting ?: formUiState.isSubmitting,
                    onActiveIndexChange = { activePhotoIndex = it },
                    onAddFromGallery = openGallery,
                    onTakePhoto = openCamera,
                    onRemovePhoto = {
                        if (editPost == null) viewModel.removeImageAt(it)
                        else if (it < existingPhotos.size) existingPhotos.removeAt(it)
                        else newPhotoUris.removeAt(it - existingPhotos.size)
                        activePhotoIndex = (it - 1).coerceAtLeast(0)
                    }
                )
            }

            // Title Field
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.publication_title_required_label),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(80) },
                    placeholder = { Text(stringResource(R.string.create_post_title_hint)) },
                    trailingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("post_title_input"),
                    isError = attemptedSubmit && title.isBlank()
                )
                if (attemptedSubmit && title.isBlank()) Text(stringResource(R.string.title_required), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                Text(
                    text = stringResource(R.string.title_guidance),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Category Selector
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.service_category_required),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ExposedDropdownMenuBox(
                    expanded = isCategoryDropdownExpanded,
                    onExpandedChange = { isCategoryDropdownExpanded = !isCategoryDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = stringResource(categories.first { it.first == selectedCategory }.second),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded) },
                        leadingIcon = {
                            Icon(Icons.Default.HomeRepairService, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isCategoryDropdownExpanded,
                        onDismissRequest = { isCategoryDropdownExpanded = false }
                    ) {
                        categories.forEach { (cat, labelRes) ->
                            DropdownMenuItem(
                                text = { Text(stringResource(labelRes)) },
                                onClick = {
                                    selectedCategory = cat
                                    isCategoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Description Field with character counter
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.detailed_service_description),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 500) description = it },
                    placeholder = { Text(stringResource(R.string.description_hint)) },
                    maxLines = 5,
                    minLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("post_desc_input"),
                    isError = attemptedSubmit && description.isBlank()
                )
                if (attemptedSubmit && description.isBlank()) Text(stringResource(R.string.description_required), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.safety_and_warranty_hint), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text("${description.length}/500", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }

            // Price Range Section
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.approximate_price_range),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = stringResource(R.string.colombian_pesos),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Price to agree toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(stringResource(R.string.free_or_quote), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                            Text(stringResource(R.string.quote_value_explainer), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(
                            checked = isPriceToAgree,
                            onCheckedChange = { isPriceToAgree = it }
                        )
                    }

                    if (!isPriceToAgree) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = minPrice,
                                onValueChange = { minPrice = it.filter(Char::isDigit).take(9) },
                                label = { Text(stringResource(R.string.price_minimum)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(10.dp),
                                isError = attemptedSubmit && (minPrice.toIntOrNull() ?: 0) <= 0,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = maxPrice,
                                onValueChange = { maxPrice = it.filter(Char::isDigit).take(9) },
                                label = { Text(stringResource(R.string.price_maximum)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(10.dp),
                                isError = attemptedSubmit && ((maxPrice.toIntOrNull() ?: 0) < (minPrice.toIntOrNull() ?: 0)),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (attemptedSubmit && ((minPrice.toIntOrNull() ?: 0) <= 0 || (maxPrice.toIntOrNull() ?: 0) < (minPrice.toIntOrNull() ?: 0))) {
                            Text(stringResource(R.string.valid_price_range), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Location & Coverage
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.location_and_coverage), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    }

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it.take(120) },
                        label = { Text(stringResource(R.string.city_or_service_area)) },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        singleLine = true,
                        isError = attemptedSubmit && location.isBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (attemptedSubmit && location.isBlank()) Text(stringResource(R.string.location_required), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showLocationPicker = true },
                            modifier = Modifier.weight(1f).height(48.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.open_location_map), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                        OutlinedButton(
                            onClick = {
                                val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                if (hasFine || hasCoarse) requestDeviceLocation()
                                else locationPermissionLauncher.launch(
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                )
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.use_current_location), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                    }
                    if (locationLatitude != null && locationLongitude != null) {
                        Text(
                            stringResource(R.string.selected_coordinates, locationLatitude!!, locationLongitude!!),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        PostLocationMap(
                            latitude = locationLatitude,
                            longitude = locationLongitude,
                            locationLabel = location,
                            modifier = Modifier.fillMaxWidth().height(160.dp)
                        )
                    } else if (attemptedSubmit) {
                        Text(stringResource(R.string.post_location_not_selected), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text(stringResource(R.string.create_post)) },
            text = { Text(stringResource(R.string.create_post_help)) },
            confirmButton = { TextButton(onClick = { showHelpDialog = false }) { Text(stringResource(R.string.understood)) } }
        )
    }

    if (showLocationPicker) {
        LocationPickerDialog(
            initialLatitude = locationLatitude,
            initialLongitude = locationLongitude,
            onDismiss = { showLocationPicker = false },
            onConfirm = { latitude, longitude ->
                locationLatitude = latitude
                locationLongitude = longitude
                showLocationPicker = false
            }
        )
    }

    if (showEditSavedConfirmation) {
        AlertDialog(
            onDismissRequest = { showEditSavedConfirmation = false; onEditSaved() },
            title = { Text("Cambios enviados a revisión") },
            text = { Text("Tu publicación pasó a Pendiente de revisión y será verificada por un moderador.") },
            confirmButton = {
                TextButton(onClick = { showEditSavedConfirmation = false; onEditSaved() }) {
                    Text(stringResource(R.string.done))
                }
            }
        )
    }

    if (showDeleteConfirmation && editPost != null && editViewModel != null) {
        AlertDialog(
            onDismissRequest = {
                if (editUiState?.value?.isSubmitting != true) showDeleteConfirmation = false
            },
            title = { Text(stringResource(R.string.delete_post_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.delete_publication_irreversible_message, editPost.title))
                    if (editUiState?.value?.submissionError != null) {
                        Text(editUiState.value!!.submissionError!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { editViewModel.deletePostPermanently(editPost.id) },
                    enabled = editUiState?.value?.isSubmitting != true
                ) {
                    if (editUiState?.value?.isSubmitting == true) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.delete_permanently), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false },
                    enabled = editUiState?.value?.isSubmitting != true
                ) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
