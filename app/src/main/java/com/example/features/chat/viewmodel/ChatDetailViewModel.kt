package com.example.features.chat.viewmodel

import androidx.lifecycle.ViewModel
import com.example.core.data.model.ChatStatus
import com.example.core.data.repository.MockDataRepository

class ChatDetailViewModel(private val repository: MockDataRepository) : ViewModel() {
    val conversations = repository.conversations
    fun markConversationRead(id: String) = repository.markConversationRead(id)
    fun sendMessage(id: String, text: String) = repository.sendMessage(id, text)
    fun updateConversationStatus(id: String, status: ChatStatus) = repository.updateConversationStatus(id, status)
    fun reportConversation(id: String, reason: String, description: String) {
        val chat = conversations.value.firstOrNull { it.id == id } ?: return
        repository.createReport(reason, description.ifBlank { "Reporte enviado por la comunidad." }, "Chat", chat.serviceTitle, targetChatId = chat.id)
    }
    fun resolveReportsForChat(id: String) = repository.resolveReportsForChat(id)
}
