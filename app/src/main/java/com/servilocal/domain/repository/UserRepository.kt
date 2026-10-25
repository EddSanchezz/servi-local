package com.servilocal.domain.repository

import com.servilocal.domain.model.User

interface UserRepository {
    suspend fun save(user: User): User
    suspend fun findById(id: Long): User?
    suspend fun findByEmail(email: String): User?
    suspend fun login(email: String, password: String): User?
    suspend fun updatePassword(userId: Long, passwordHash: String)
}
