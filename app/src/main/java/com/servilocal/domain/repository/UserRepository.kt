package com.servilocal.domain.repository

import com.servilocal.domain.model.User

interface UserRepository {
    suspend fun save(user: User)
    suspend fun findById(id: String): User?
    suspend fun findByEmail(email: String): User?
    suspend fun login(email: String, password: String): User?
}
