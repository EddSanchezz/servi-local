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
import com.servilocal.R
import com.servilocal.core.ui.components.ImagePicker
import com.servilocal.core.util.requestCurrentLocation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    viewModel: CreatePostViewModel,
    onBack: () -> Unit,
    onOpenHome: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Hogar") }
    var description by remember { mutableStateOf("") }
    var isPriceToAgree by remember { mutableStateOf(false) }
    var minPrice by remember { mutableStateOf("") }
    var maxPrice by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("Armenia, Quindío") }
    var locationLatitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var locationLongitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var showLocationPicker by remember { mutableStateOf(false) }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    var attemptedSubmit by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showCreatedDialog by remember { mutableStateOf(false) }
    val formUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val categories = listOf(
        "Hogar" to R.string.category_home,
        "Educación" to R.string.category_education,
        "Mascotas" to R.string.category_pets,
        "Tecnología" to R.string.category_technology,
        "Electricidad" to R.string.category_electricity,
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
        if (formUiState.isCreated) {
            snackbarHostState.showSnackbar(context.getString(R.string.publication_sent_review))
            showCreatedDialog = true
        }
    }

    LaunchedEffect(formUiState.submissionError) {
        formUiState.submissionError?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        stringResource(R.string.create_publication_title),
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
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = {
                            attemptedSubmit = true
                            val parsedMin = minPrice.toIntOrNull() ?: 0
                            val parsedMax = maxPrice.toIntOrNull() ?: 0
                            val pricesAreValid = isPriceToAgree || (parsedMin > 0 && parsedMax >= parsedMin)
                            if (title.isNotBlank() && description.isNotBlank() && location.isNotBlank() && pricesAreValid && locationLatitude != null && locationLongitude != null) {
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
                            } else {
                                val message = when {
                                    locationLatitude == null || locationLongitude == null -> context.getString(R.string.post_location_not_selected)
                                    formUiState.selectedImageUri.isNullOrBlank() -> context.getString(R.string.image_required_for_post)
                                    else -> context.getString(R.string.complete_required_fields)
                                }
                                coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                            }
                        },
                        enabled = !formUiState.isSubmitting,
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
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.send_for_review), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                    Text(
                        text = stringResource(R.string.publication_terms_notice),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp)
                    )
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

            // Gallery Section
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(stringResource(R.string.image_gallery), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text(stringResource(R.string.show_real_work), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Text(stringResource(R.string.required_field), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }

                ImagePicker(
                    onImageSelected = { viewModel.onImageSelected(it.toString()) }
                ) { openPicker ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            formUiState.selectedImageUri?.let { imageUri ->
                                AsyncImage(
                                    model = Uri.parse(imageUri),
                                    contentDescription = stringResource(R.string.image_gallery),
                                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp))
                                )
                            } ?: Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (formUiState.selectedImageUri == null) stringResource(R.string.image_required_for_post) else stringResource(R.string.post_image_selected),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            OutlinedButton(onClick = openPicker, enabled = !formUiState.isSubmitting) {
                                Text(stringResource(R.string.add_post_image))
                            }
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
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.open_location_map))
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
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.use_current_location))
                        }
                    }
                    if (locationLatitude != null && locationLongitude != null) {
                        Text(
                            stringResource(R.string.selected_coordinates, locationLatitude!!, locationLongitude!!),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (attemptedSubmit) {
                        Text(stringResource(R.string.post_location_not_selected), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
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

    if (showCreatedDialog) {
        AlertDialog(
            onDismissRequest = { showCreatedDialog = false; onOpenHome() },
            title = { Text(stringResource(R.string.post_submitted)) },
            text = { Text(stringResource(R.string.post_submitted_message)) },
            confirmButton = { TextButton(onClick = { showCreatedDialog = false; onOpenProfile() }) { Text(stringResource(R.string.view_my_posts)) } },
            dismissButton = { TextButton(onClick = { showCreatedDialog = false; onOpenHome() }) { Text(stringResource(R.string.explore)) } }
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
}
