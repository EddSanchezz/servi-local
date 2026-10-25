package com.servilocal.features.chat.viewmodel

import androidx.lifecycle.ViewModel
import com.servilocal.domain.usecase.chat.MarkConversationReadUseCase
import com.servilocal.domain.usecase.chat.ObserveChatConversationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject

@HiltViewModel
class MessagesViewModel @Inject constructor(
    observeChatConversations: ObserveChatConversationsUseCase,
    private val markConversationReadUseCase: MarkConversationReadUseCase
) : ViewModel() {
    val conversations = observeChatConversations()
    fun markConversationRead(id: String) = markConversationReadUseCase(id)
}
