package com.servilocal.domain.usecase.chat

import com.servilocal.domain.repository.ChatRepository
import jakarta.inject.Inject

class StartConversationUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(postId: String): String? {
        return chatRepository.startConversation(postId)
    }
}
