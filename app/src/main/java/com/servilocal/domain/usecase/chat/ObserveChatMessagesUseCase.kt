package com.servilocal.domain.usecase.chat

import com.servilocal.domain.model.ChatMessageItem
import com.servilocal.domain.repository.ChatRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveChatMessagesUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(conversationId: String): Flow<List<ChatMessageItem>> =
        chatRepository.conversations.map { conversations ->
            conversations.firstOrNull { it.id == conversationId }?.messages.orEmpty()
        }
}
