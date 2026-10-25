package com.servilocal.domain.usecase.chat

import com.servilocal.domain.model.ChatConversation
import com.servilocal.domain.repository.ChatRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.StateFlow

class ObserveChatConversationsUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(): StateFlow<List<ChatConversation>> = chatRepository.conversations
}
