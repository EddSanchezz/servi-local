package com.servilocal.features.chat.viewmodel

import androidx.lifecycle.ViewModel
import com.servilocal.domain.model.ChatStatus
import com.servilocal.domain.repository.ChatRepository
import com.servilocal.domain.repository.ModerationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val moderationRepository: ModerationRepository
) : ViewModel() {
    val conversations = chatRepository.conversations
    fun markConversationRead(id: String) = chatRepository.markConversationRead(id)
    fun sendMessage(id: String, text: String) = chatRepository.sendMessage(id, text)
    fun updateConversationStatus(id: String, status: ChatStatus) = chatRepository.updateConversationStatus(id, status)
    fun reportConversation(id: String, reason: String, description: String) {
        val chat = conversations.value.firstOrNull { it.id == id } ?: return
        moderationRepository.createReport(reason, description.ifBlank { "Reporte enviado por la comunidad." }, "Chat", chat.serviceTitle, targetChatId = chat.id)
    }
    fun resolveReportsForChat(id: String) = chatRepository.resolveReportsForChat(id)
}
