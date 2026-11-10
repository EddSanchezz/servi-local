package com.servilocal.domain.repository

import com.servilocal.domain.model.User

sealed interface GoogleSignInResult {
    data class Success(val user: User) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
    data object NetworkError : GoogleSignInResult
    data object AccountConflict : GoogleSignInResult
    data object ProviderError : GoogleSignInResult
    data object Unavailable : GoogleSignInResult
}
