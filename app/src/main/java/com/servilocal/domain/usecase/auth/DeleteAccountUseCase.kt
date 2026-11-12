package com.servilocal.domain.usecase.auth

import com.servilocal.domain.repository.ChatRepository
import com.servilocal.domain.repository.PostRepository
import com.servilocal.domain.repository.SessionRepository
import com.servilocal.domain.repository.UserRepository
import jakarta.inject.Inject

sealed interface DeleteAccountResult {
    data object Success : DeleteAccountResult
    data object NoActiveSession : DeleteAccountResult
    data object Failed : DeleteAccountResult
}

/**
 * Elimina la cuenta del usuario autenticado en cascada:
 * publicaciones, conversaciones y, por último, el perfil.
 */
class DeleteAccountUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(): DeleteAccountResult {
        val session = sessionRepository.session.value ?: return DeleteAccountResult.NoActiveSession
        val userId = session.userId

        val postsDeleted = postRepository.deletePostsByUser(userId)
        val chatsDeleted = chatRepository.deleteConversationsForUser(userId)
        if (!postsDeleted || !chatsDeleted) return DeleteAccountResult.Failed

        if (!userRepository.deleteById(userId)) return DeleteAccountResult.Failed

        sessionRepository.clearSession()
        return DeleteAccountResult.Success
    }
}
