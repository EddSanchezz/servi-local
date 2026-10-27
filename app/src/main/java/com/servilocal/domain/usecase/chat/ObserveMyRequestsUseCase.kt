package com.servilocal.domain.usecase.chat

import com.servilocal.domain.model.ChatConversation
import com.servilocal.domain.repository.ChatRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Service requests sent by the signed-in user in the mock chat repository. */
class ObserveMyRequestsUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(): Flow<List<ChatConversation>> =
        chatRepository.conversations.map { conversations -> conversations.filter { it.isClientRole } }
}
