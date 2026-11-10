package com.servilocal.domain.usecase.auth

import android.app.Activity
import com.servilocal.domain.repository.GoogleSignInResult
import com.servilocal.domain.repository.SessionRepository
import com.servilocal.domain.repository.UserRepository
import com.servilocal.domain.repository.AuthRepository
import jakarta.inject.Inject

class SignInWithGoogleUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository? = null
) {
    suspend operator fun invoke(activity: Activity): GoogleSignInResult {
        return when (val result = userRepository.signInWithGoogle(activity)) {
            is GoogleSignInResult.Success -> {
                sessionRepository.saveSession(result.user.id, result.user.role)
                authRepository?.syncCurrentUser(result.user)
                result
            }
            else -> result
        }
    }
}
