package com.servilocal.features.chat.viewmodel

import androidx.lifecycle.ViewModel
import com.servilocal.domain.model.ChatStatus
import com.servilocal.domain.usecase.chat.MarkConversationReadUseCase
import com.servilocal.domain.usecase.chat.ObserveChatConversationsUseCase
import com.servilocal.domain.usecase.chat.SendMessageUseCase
import com.servilocal.domain.usecase.chat.UpdateConversationStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    observeChatConversations: ObserveChatConversationsUseCase,
    private val markConversationReadUseCase: MarkConversationReadUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val updateConversationStatusUseCase: UpdateConversationStatusUseCase
) : ViewModel() {
    val conversations = observeChatConversations()
    fun markConversationRead(id: String) = markConversationReadUseCase(id)
    fun sendMessage(id: String, text: String) = sendMessageUseCase(id, text)
    fun updateConversationStatus(id: String, status: ChatStatus) = updateConversationStatusUseCase(id, status)
}
