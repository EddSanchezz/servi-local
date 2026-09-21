package com.example.domain.repository

import com.example.data.model.ChatConversation
import com.example.data.model.ChatMessageItem
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