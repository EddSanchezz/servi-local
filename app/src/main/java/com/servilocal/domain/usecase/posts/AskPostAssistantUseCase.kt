package com.servilocal.domain.usecase.posts

import com.servilocal.domain.model.AiPostAssistantReply
import com.servilocal.domain.model.AiPostAssistantRequest
import com.servilocal.domain.repository.AiPostAssistantRepository
import jakarta.inject.Inject

class AskPostAssistantUseCase @Inject constructor(private val repository: AiPostAssistantRepository) {
    suspend operator fun invoke(request: AiPostAssistantRequest): AiPostAssistantReply = repository.ask(request)
}
