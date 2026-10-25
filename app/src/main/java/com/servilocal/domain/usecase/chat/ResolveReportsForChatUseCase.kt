package com.servilocal.domain.usecase.chat

import com.servilocal.domain.repository.ChatRepository
import jakarta.inject.Inject

class ResolveReportsForChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(chatId: String) {
        chatRepository.resolveReportsForChat(chatId)
    }
}
