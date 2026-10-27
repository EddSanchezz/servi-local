package com.servilocal.features.chat.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servilocal.domain.model.ChatStatus
import com.servilocal.domain.model.ChatMessageItem
import com.servilocal.domain.usecase.chat.MarkConversationReadUseCase
import com.servilocal.domain.usecase.chat.ObserveChatMessagesUseCase
import com.servilocal.domain.usecase.chat.ObserveChatConversationsUseCase
import com.servilocal.domain.usecase.chat.SendMessageUseCase
import com.servilocal.domain.usecase.chat.UpdateRequestStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeChatConversations: ObserveChatConversationsUseCase,
    observeChatMessages: ObserveChatMessagesUseCase,
    private val markConversationReadUseCase: MarkConversationReadUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val updateRequestStatusUseCase: UpdateRequestStatusUseCase
) : ViewModel() {
    private val chatId: String = savedStateHandle.get<String>("chatId").orEmpty()
    val conversations = observeChatConversations()
    val messages: StateFlow<List<ChatMessageItem>> = observeChatMessages(chatId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun markConversationRead(id: String) = markConversationReadUseCase(id)
    fun sendMessage(id: String, text: String) = sendMessageUseCase(id, text)
    fun updateConversationStatus(id: String, status: ChatStatus) = updateRequestStatusUseCase(id, status)
}
