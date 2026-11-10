package com.servilocal.domain.repository

import com.servilocal.domain.model.User
import kotlinx.coroutines.flow.StateFlow
import android.app.Activity

interface UserRepository {
    val users: StateFlow<List<User>>
    suspend fun save(user: User): User
    suspend fun findById(id: String): User?
    suspend fun findByEmail(email: String): User?
    suspend fun login(email: String, password: String): User?
    suspend fun createAccount(name: String, email: String, password: String): User?
    suspend fun sendPasswordResetEmail(email: String): Boolean
    suspend fun confirmPasswordResetCode(code: String, newPassword: String): Boolean
    suspend fun signInWithGoogle(activity: Activity): GoogleSignInResult = GoogleSignInResult.Unavailable
}
