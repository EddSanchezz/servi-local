package com.servilocal.domain.usecase.chat

import com.servilocal.domain.model.ChatStatus
import com.servilocal.domain.repository.ChatRepository
import jakarta.inject.Inject

class UpdateConversationStatusUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(conversationId: String, status: ChatStatus) {
        chatRepository.updateConversationStatus(conversationId, status)
    }
}
