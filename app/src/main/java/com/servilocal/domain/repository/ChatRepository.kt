package com.servilocal.domain.repository

import com.servilocal.domain.model.ChatConversation
import com.servilocal.domain.model.ChatStatus
import kotlinx.coroutines.flow.StateFlow

interface ChatRepository {
    val conversations: StateFlow<List<ChatConversation>>

    fun sendMessage(conversationId: String, text: String)
    fun startConversation(postId: String): String?
    fun markConversationRead(conversationId: String)
    fun updateConversationStatus(conversationId: String, status: ChatStatus)
    fun createServiceRequest(
        postId: String,
        applicantName: String,
        contactMethod: String,
        address: String,
        description: String
    ): String
    fun resolveReportsForChat(chatId: String)
}
