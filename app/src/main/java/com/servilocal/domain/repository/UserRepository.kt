package com.servilocal.domain.repository

import com.servilocal.domain.model.User
import kotlinx.coroutines.flow.StateFlow

interface UserRepository {
    val users: StateFlow<List<User>>
    suspend fun save(user: User): User
    suspend fun findById(id: Long): User?
    suspend fun findByEmail(email: String): User?
    suspend fun login(email: String, password: String): User?
    suspend fun updatePassword(userId: Long, passwordHash: String)
}
