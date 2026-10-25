package com.servilocal.data.repository.memory

import com.servilocal.core.security.PasswordHasher
import com.servilocal.domain.model.User
import com.servilocal.domain.model.UserLevel
import com.servilocal.domain.model.UserRole
import com.servilocal.domain.repository.UserRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong

@Singleton
class UserRepositoryImpl @Inject constructor() : UserRepository {

    private val users = MutableStateFlow(seedUsers())
    private val idCounter = AtomicLong(seedUsers().maxOfOrNull { it.id } ?: 0L)

    override suspend fun save(user: User): User {
        val userToSave = if (user.id == 0L) {
            user.copy(id = idCounter.incrementAndGet())
        } else {
            user
        }
        users.update { current ->
            current.filterNot { it.id == userToSave.id } + userToSave
        }
        return userToSave
    }

    override suspend fun findById(id: Long): User? =
        users.value.firstOrNull { it.id == id }

    override suspend fun findByEmail(email: String): User? {
        val normalizedEmail = email.trim().lowercase()
        return users.value.firstOrNull { it.email.lowercase() == normalizedEmail }
    }

    override suspend fun login(email: String, password: String): User? {
        val user = findByEmail(email) ?: return null
        return user.takeIf { it.passwordHash == PasswordHasher.hash(password) }
    }

    override suspend fun updatePassword(userId: Long, passwordHash: String) {
        users.update { current ->
            current.map { user ->
                if (user.id == userId) user.copy(passwordHash = passwordHash) else user
            }
        }
    }

    private fun seedUsers(): List<User> = listOf(
        User(
            id = 1L,
            name = "Valentina Morales",
            email = "valentina.morales@servilocal.co",
            passwordHash = PasswordHasher.hash("servilocal2026"),
            role = UserRole.USER,
            points = 220,
            level = UserLevel.MAESTRO
        ),
        User(
            id = 2L,
            name = "Carlos Mendoza",
            email = "carlos.mendoza@servilocal.co",
            passwordHash = PasswordHasher.hash("servilocal2026"),
            role = UserRole.USER,
            points = 120,
            level = UserLevel.PROFESIONAL
        ),
        User(
            id = 3L,
            name = "Alejandro Vargas",
            email = "alejandro.vargas@staff.servilocal.pe",
            passwordHash = PasswordHasher.hash("servilocal2026"),
            role = UserRole.MODERATOR,
            points = 500,
            level = UserLevel.MAESTRO
        ),
        User(
            id = 4L,
            name = "Jero",
            email = "my@gmail.com",
            passwordHash = PasswordHasher.hash("Jero1234"),
            role = UserRole.USER,
            points = 100,
            level = UserLevel.PRINCIPIANTE
        )
    )
}
