package com.servilocal.domain.repository

import com.servilocal.domain.model.ChatConversation
import com.servilocal.domain.model.ChatStatus
import com.servilocal.domain.model.ServicePost
import com.servilocal.domain.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

interface ChatRepository {
    val conversations: StateFlow<List<ChatConversation>>

    fun sendMessage(conversationId: String, text: String)
    suspend fun deleteConversationsForUser(userId: String): Boolean = false
    fun softDeleteChat(conversationId: String)
    fun startConversation(postId: String): String?
    fun startConversation(post: ServicePost, currentUser: UserProfile): String? =
        startConversation(post.id)
    fun markConversationRead(conversationId: String)
    fun updateConversationStatus(conversationId: String, status: ChatStatus)
    fun createServiceRequest(
        postId: String,
        applicantName: String,
        contactMethod: String,
        address: String,
        description: String
    ): String?
}
