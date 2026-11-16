package com.servilocal.features.chat.ui

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.ExperimentalLayoutApi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.servilocal.domain.model.ChatMessageItem
import com.servilocal.features.app.viewmodel.AppUiState
import com.servilocal.features.chat.viewmodel.ChatDetailViewModel
import com.servilocal.R
import kotlinx.coroutines.launch
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatDetailScreen(
    chatId: String,
    initialText: String? = null,
    viewModel: ChatDetailViewModel,
    appUiState: AppUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val conversation = conversations.find { it.id == chatId }

    var messageInput by remember(chatId, initialText) { mutableStateOf(initialText ?: "") }
    var messageError by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isImeVisible = WindowInsets.isImeVisible

    BackHandler {
        if (isImeVisible) {
            focusManager.clearFocus()
            keyboardController?.hide()
        } else {
            onBack()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    LaunchedEffect(chatId) {
        viewModel.markConversationRead(chatId)
    }

    if (conversation == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.conversation_not_found))
        }
        return
    }

    val isAvailable = conversation.isPostAvailable

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!conversation.recipientAvatar.isNullOrBlank()) {
                                AsyncImage(
                                    model = conversation.recipientAvatar,
                                    contentDescription = conversation.recipientName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = conversation.recipientName.split(" ")
                                        .mapNotNull { it.firstOrNull()?.toString() }
                                        .take(2)
                                        .joinToString(""),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Column {
                            Text(
                                text = conversation.recipientName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                            if (conversation.isClientRole) {
                                Text(
                                    text = conversation.serviceTitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.more_options))
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                },
                                onClick = {
                                    menuExpanded = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        },
        bottomBar = {
            if (!isAvailable) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Text(
                            text = "Este servicio/post ya no está disponible.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .testTag("chat_message_input"),
                            value = messageInput,
                            onValueChange = { messageInput = it; if (it.isNotBlank()) messageError = false },
                            placeholder = { Text(stringResource(R.string.message_hint), style = MaterialTheme.typography.bodyMedium) },
                            isError = messageError,
                            supportingText = { if (messageError) Text(stringResource(R.string.message_required)) },
                            singleLine = false,
                            maxLines = 4,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                            enabled = isAvailable,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent
                            )
                        )

                        FloatingActionButton(
                            onClick = {
                                if (isAvailable && messageInput.isNotBlank()) {
                                    viewModel.sendMessage(conversation.id, messageInput)
                                    messageError = false
                                    messageInput = ""
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(messages.size)
                                    }
                                } else {
                                    messageError = true
                                }
                            },
                            containerColor = if (isAvailable) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            contentColor = if (isAvailable) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp).testTag("send_chat_button"),
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = stringResource(R.string.send), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Contextual Service Strip
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Handyman, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(stringResource(R.string.active_service), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(conversation.serviceTitle, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                    Text(conversation.servicePrice, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                }
            }

            // Message History
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay mensajes en esta conversación.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatMessageBubble(message = msg)
                    }

                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar chat") },
            text = { Text("La conversación se ocultará de tu lista, pero conservará su historial.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.softDeleteChat(conversation.id)
                        showDeleteDialog = false
                        onBack()
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ChatMessageBubble(message: ChatMessageItem) {
    val isMe = message.isFromMe
    val bubbleColor = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val textColor = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val shape = if (isMe) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMe) MessageAvatar(message.senderName, message.senderAvatar)
        if (isMe) MessageAvatar(message.senderName, message.senderAvatar)
        Surface(
            color = bubbleColor,
            shape = shape,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (!isMe) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.time,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun MessageAvatar(name: String, avatarUrl: String?) {
        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
    }
}
