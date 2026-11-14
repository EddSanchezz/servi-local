package com.servilocal.domain.usecase.chat

import com.servilocal.domain.repository.ChatRepository
import jakarta.inject.Inject

class SoftDeleteChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(conversationId: String) {
        chatRepository.softDeleteChat(conversationId)
    }
}
