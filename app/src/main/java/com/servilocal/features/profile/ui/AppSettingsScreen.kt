package com.servilocal.features.profile.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.os.LocaleListCompat
import com.servilocal.R

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    darkTheme: Boolean,
    notificationsEnabled: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onLanguageChange: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var locationGranted by remember { mutableStateOf(context.hasLocationPermission()) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val selectedLanguage = remember {
        AppCompatDelegate.getApplicationLocales().toLanguageTags()
            .substringBefore(',')
            .substringBefore('-')
            .ifBlank { "es" }
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        locationGranted = context.hasLocationPermission()
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                locationGranted = context.hasLocationPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SettingsSection(title = stringResource(R.string.settings_appearance_section)) {
                ListItem(
                    leadingContent = { Icon(Icons.Default.Palette, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.settings_dark_theme)) },
                    supportingContent = { Text(stringResource(R.string.settings_dark_theme_summary)) },
                    trailingContent = {
                        Switch(checked = darkTheme, onCheckedChange = onDarkThemeChange)
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    leadingContent = { Icon(Icons.Default.Language, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.settings_language)) },
                    supportingContent = {
                        Text(
                            if (selectedLanguage == "en") stringResource(R.string.language_english)
                            else stringResource(R.string.language_spanish)
                        )
                    },
                    trailingContent = {
                        TextButton(onClick = { showLanguageDialog = true }) {
                            Text(stringResource(R.string.settings_change))
                        }
                    }
                )
            }

            SettingsSection(title = stringResource(R.string.settings_notifications_section)) {
                ListItem(
                    leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.settings_in_app_notifications)) },
                    supportingContent = { Text(stringResource(R.string.settings_notifications_summary)) },
                    trailingContent = {
                        Switch(checked = notificationsEnabled, onCheckedChange = onNotificationsChange)
                    }
                )
            }

            SettingsSection(title = stringResource(R.string.settings_permissions_section)) {
                ListItem(
                    leadingContent = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.permission_location)) },
                    supportingContent = {
                        Text(
                            if (locationGranted) stringResource(R.string.permission_location_granted)
                            else stringResource(R.string.permission_location_summary)
                        )
                    },
                    trailingContent = {
                        TextButton(onClick = {
                            if (locationGranted) context.openApplicationSettings() else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    )
                                )
                            }
                        }) {
                            Text(
                                if (locationGranted) stringResource(R.string.permission_manage)
                                else stringResource(R.string.permission_allow)
                            )
                        }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    leadingContent = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.permission_camera)) },
                    supportingContent = { Text(stringResource(R.string.permission_camera_summary)) },
                    trailingContent = {
                        Text(
                            stringResource(R.string.permission_coming_soon),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                )
            }
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.settings_language)) },
            text = {
                Column {
                    LanguageOption(
                        label = stringResource(R.string.language_spanish),
                        selected = selectedLanguage == "es",
                        onClick = {
                            showLanguageDialog = false
                            onLanguageChange("es")
                        }
                    )
                    LanguageOption(
                        label = stringResource(R.string.language_english),
                        selected = selectedLanguage == "en",
                        onClick = {
                            showLanguageDialog = false
                            onLanguageChange("en")
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.done))
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }
}

@Composable
private fun LanguageOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        TextButton(onClick = onClick) { Text(label) }
    }
}

private fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun Context.openApplicationSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}
