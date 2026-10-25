package com.servilocal.domain.usecase.chat

import com.servilocal.domain.repository.ChatRepository
import jakarta.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(conversationId: String, text: String) {
        chatRepository.sendMessage(conversationId, text)
    }
}
