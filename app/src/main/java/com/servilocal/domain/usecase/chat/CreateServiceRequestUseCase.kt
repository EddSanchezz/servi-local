package com.servilocal.domain.usecase.chat

import com.servilocal.domain.repository.ChatRepository
import jakarta.inject.Inject

class CreateServiceRequestUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(
        postId: String,
        applicantName: String,
        contactMethod: String,
        address: String,
        description: String
    ): String {
        return chatRepository.createServiceRequest(postId, applicantName, contactMethod, address, description)
    }
}
