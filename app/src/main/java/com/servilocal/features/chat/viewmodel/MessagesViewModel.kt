package com.servilocal.features.chat.viewmodel

import androidx.lifecycle.ViewModel
import com.servilocal.domain.usecase.chat.ObserveMyRequestsUseCase
import com.servilocal.domain.usecase.chat.ObserveReceivedRequestsUseCase
import com.servilocal.domain.usecase.chat.MarkConversationReadUseCase
import com.servilocal.domain.usecase.chat.ObserveChatConversationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject

@HiltViewModel
class MessagesViewModel @Inject constructor(
    observeChatConversations: ObserveChatConversationsUseCase,
    observeMyRequests: ObserveMyRequestsUseCase,
    observeReceivedRequests: ObserveReceivedRequestsUseCase,
    private val markConversationReadUseCase: MarkConversationReadUseCase
) : ViewModel() {
    val conversations = observeChatConversations()
    val myRequests = observeMyRequests()
    val receivedRequests = observeReceivedRequests()
    fun markConversationRead(id: String) = markConversationReadUseCase(id)
}
