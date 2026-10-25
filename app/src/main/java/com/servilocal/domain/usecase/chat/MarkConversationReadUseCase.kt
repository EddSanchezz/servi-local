package com.servilocal.domain.usecase.chat

import com.servilocal.domain.repository.ChatRepository
import jakarta.inject.Inject

class MarkConversationReadUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(conversationId: String) {
        chatRepository.markConversationRead(conversationId)
    }
}
