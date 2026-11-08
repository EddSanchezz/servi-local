package com.servilocal.features.posts.ui

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.servilocal.domain.model.PostStatus
import com.servilocal.features.posts.viewmodel.EditPostViewModel
import com.servilocal.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPostScreen(
    postId: String,
    viewModel: EditPostViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val observedPost by viewModel.post.collectAsStateWithLifecycle()
    val post = observedPost
    val formUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var showFinishConfirmation by remember { mutableStateOf(false) }

    if (post == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.post_not_found))
        }
        return
    }

    val canEdit = post.status == PostStatus.APPROVED
    val statusLabel = when (post.status) {
        PostStatus.PENDING_VERIFICATION -> stringResource(R.string.pending_review_status)
        PostStatus.APPROVED -> stringResource(R.string.published_status)
        PostStatus.REJECTED -> stringResource(R.string.rejected_status)
        PostStatus.FINISHED -> stringResource(R.string.finished_status)
    }
    val categories = listOf(
        "Hogar" to R.string.category_home,
        "Educación" to R.string.category_education,
        "Mascotas" to R.string.category_pets,
        "Tecnología" to R.string.category_technology,
        "Electricidad" to R.string.category_electricity,
        "Transporte" to R.string.category_transport
    )

    var title by remember(post.id) { mutableStateOf(post.title) }
    var selectedCategory by remember(post.id) { mutableStateOf(post.category) }
    var description by remember(post.id) { mutableStateOf(post.description) }
    var isPriceToAgree by remember(post.id) { mutableStateOf(post.isPriceToAgree) }
    var minPrice by remember(post.id) { mutableStateOf(if (post.isPriceToAgree) "" else post.minPrice.toString()) }
    var maxPrice by remember(post.id) { mutableStateOf(if (post.isPriceToAgree) "" else post.maxPrice.toString()) }
    var location by remember(post.id) { mutableStateOf(post.location) }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    var attemptedSubmit by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.edit_post_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            if (canEdit) {
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
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                attemptedSubmit = true
                                val parsedMin = minPrice.toIntOrNull() ?: 0
                                val parsedMax = maxPrice.toIntOrNull() ?: 0
                                val pricesAreValid = isPriceToAgree || (parsedMin > 0 && parsedMax >= parsedMin)
                                if (title.isNotBlank() && description.isNotBlank() && location.isNotBlank() && pricesAreValid) {
                                    val saved = viewModel.updatePost(
                                        postId = post.id,
                                        title = title.trim(),
                                        category = selectedCategory,
                                        description = description.trim(),
                                        minPrice = parsedMin,
                                        maxPrice = parsedMax,
                                        isPriceToAgree = isPriceToAgree,
                                        location = location.trim()
                                    )
                                    if (saved) onBack()
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(context.getString(R.string.complete_required_fields))
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("save_post_changes_button"),
                            shape = RoundedCornerShape(26.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.save_changes), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        }
                        OutlinedButton(
                            onClick = { showFinishConfirmation = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("finish_post_button"),
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.TaskAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.finish_post), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        }
                        formUiState.submissionError?.let { error ->
                            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
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

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (canEdit) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        if (canEdit) Icons.Default.Edit else Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.post_status_label, statusLabel),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (canEdit) {
                                stringResource(R.string.edit_post_status_help)
                            } else {
                                stringResource(R.string.approved_posts_only_editable)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.publication_title_required_label),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(80) },
                    enabled = canEdit,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("edit_post_title_input"),
                    isError = attemptedSubmit && title.isBlank()
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.service_category_required),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ExposedDropdownMenuBox(
                    expanded = isCategoryDropdownExpanded && canEdit,
                    onExpandedChange = { if (canEdit) isCategoryDropdownExpanded = !isCategoryDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = stringResource(categories.firstOrNull { it.first.equals(selectedCategory, true) }?.second ?: R.string.category_home),
                        onValueChange = {},
                        readOnly = true,
                        enabled = canEdit,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isCategoryDropdownExpanded && canEdit,
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

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.detailed_service_description),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 500) description = it },
                    enabled = canEdit,
                    maxLines = 5,
                    minLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("edit_post_desc_input"),
                    isError = attemptedSubmit && description.isBlank()
                )
                Text("${description.length}/500", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.approximate_price_range),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.free_or_quote), style = MaterialTheme.typography.bodySmall)
                        Switch(
                            checked = isPriceToAgree,
                            onCheckedChange = { isPriceToAgree = it },
                            enabled = canEdit
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
                                enabled = canEdit,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(10.dp),
                                isError = attemptedSubmit && (minPrice.toIntOrNull() ?: 0) <= 0,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = maxPrice,
                                onValueChange = { maxPrice = it.filter(Char::isDigit).take(9) },
                                label = { Text(stringResource(R.string.price_maximum)) },
                                enabled = canEdit,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(10.dp),
                                isError = attemptedSubmit && ((maxPrice.toIntOrNull() ?: 0) < (minPrice.toIntOrNull() ?: 0)),
                                modifier = Modifier.weight(1f)
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
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.location_and_coverage), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it.take(120) },
                        label = { Text(stringResource(R.string.city_or_service_area)) },
                        enabled = canEdit,
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        singleLine = true,
                        isError = attemptedSubmit && location.isBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    if (showFinishConfirmation) {
        AlertDialog(
            onDismissRequest = { showFinishConfirmation = false },
            title = { Text(stringResource(R.string.finish_post)) },
            text = { Text(stringResource(R.string.finish_post_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFinishConfirmation = false
                        if (viewModel.finishPost(post.id)) onBack()
                    }
                ) { Text(stringResource(R.string.finish), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showFinishConfirmation = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
