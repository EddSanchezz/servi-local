package com.servilocal.domain.repository

import com.servilocal.domain.model.AiPostAssistantReply
import com.servilocal.domain.model.AiPostAssistantRequest

interface AiPostAssistantRepository {
    suspend fun ask(request: AiPostAssistantRequest): AiPostAssistantReply
}
