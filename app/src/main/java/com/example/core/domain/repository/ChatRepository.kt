package com.example.core.domain.repository

import com.example.core.data.model.ChatConversation
import com.example.core.data.model.ChatMessageItem
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    val conversations: Flow<List<ChatConversation>>
    suspend fun sendMessage(conversationId: String, text: String)
    suspend fun createServiceRequest(
        postId: String,
        applicantName: String,
        contactMethod: String,
        address: String,
        description: String
    ): String
}