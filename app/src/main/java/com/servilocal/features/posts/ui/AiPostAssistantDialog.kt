package com.servilocal.features.posts.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import coil.compose.AsyncImage
import com.servilocal.R
import com.servilocal.core.constants.AiAssistantAssets
import com.servilocal.features.posts.viewmodel.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiPostAssistantDialog(uiState: AiPostAssistantUiState, onDismiss: () -> Unit, onInputChange: (String) -> Unit, onSend: () -> Unit, onRetry: () -> Unit, onApplySuggestion: (String, String) -> Unit) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = !WindowInsets.isImeVisible)) {
        BackHandler(enabled = WindowInsets.isImeVisible) { focus.clearFocus(); keyboard?.hide() }
        Surface(shape = RoundedCornerShape(24.dp), tonalElevation = 6.dp, modifier = Modifier.fillMaxWidth(.92f).fillMaxHeight(.78f).imePadding()) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(40.dp)) {
                        AsyncImage(AiAssistantAssets.AVATAR_URL, null, contentScale = ContentScale.Fit, modifier = Modifier.padding(3.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.ai_assistant_name), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.ai_assistant_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, stringResource(R.string.ai_assistant_close)) }
                }
                HorizontalDivider()
                LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(uiState.messages, key = { it.id }) { message ->
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = if (message.role == AiChatRole.USER) Alignment.End else Alignment.Start) {
                            Surface(shape = RoundedCornerShape(16.dp), color = if (message.role == AiChatRole.USER) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant) {
                                Text(message.text, Modifier.padding(12.dp), color = if (message.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            message.suggestion?.let { suggestion ->
                                Spacer(Modifier.height(8.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(0.92f),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                                ) {
                                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(stringResource(R.string.ai_assistant_suggested_title), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(suggestion.title, style = MaterialTheme.typography.bodyMedium)
                                        HorizontalDivider()
                                        Text(stringResource(R.string.ai_assistant_suggested_description), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(suggestion.description, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                OutlinedButton(onClick = { onApplySuggestion(suggestion.title, suggestion.description) }) { Text(stringResource(R.string.ai_assistant_apply)) }
                            }
                            if (message.isError && uiState.canRetry) TextButton(onClick = onRetry) { Text(stringResource(R.string.ai_assistant_retry)) }
                        }
                    }
                    if (uiState.isSending) item { Text(stringResource(R.string.ai_assistant_typing), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                LaunchedEffect(uiState.messages.size, uiState.isSending) {
                    val count = uiState.messages.size + if (uiState.isSending) 1 else 0
                    if (count > 0) listState.animateScrollToItem(count - 1)
                }
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = uiState.input, onValueChange = onInputChange, modifier = Modifier.weight(1f), placeholder = { Text(stringResource(R.string.ai_assistant_input_hint)) }, maxLines = 3, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default), shape = RoundedCornerShape(20.dp))
                    FilledIconButton(onClick = onSend, enabled = uiState.input.isNotBlank() && !uiState.isSending, modifier = Modifier.size(48.dp)) { Icon(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.ai_assistant_send)) }
                }
            }
        }
    }
}
