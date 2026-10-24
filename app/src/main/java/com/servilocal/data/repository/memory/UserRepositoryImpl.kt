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

@Singleton
class UserRepositoryImpl @Inject constructor() : UserRepository {

    private val users = MutableStateFlow(seedUsers())

    override suspend fun save(user: User) {
        users.update { current ->
            current.filterNot { it.id == user.id } + user
        }
    }

    override suspend fun findById(id: String): User? =
        users.value.firstOrNull { it.id == id }

    override suspend fun findByEmail(email: String): User? {
        val normalizedEmail = email.trim().lowercase()
        return users.value.firstOrNull { it.email.lowercase() == normalizedEmail }
    }

    override suspend fun login(email: String, password: String): User? {
        val user = findByEmail(email) ?: return null
        return user.takeIf { it.passwordHash == PasswordHasher.hash(password) }
    }

    override suspend fun updatePassword(userId: String, passwordHash: String) {
        users.update { current ->
            current.map { user ->
                if (user.id == userId) user.copy(passwordHash = passwordHash) else user
            }
        }
    }

    private fun seedUsers(): List<User> = listOf(
        User(
            id = "user_val_1",
            name = "Valentina Morales",
            email = "valentina.morales@servilocal.co",
            passwordHash = PasswordHasher.hash("servilocal2026"),
            role = UserRole.USER,
            points = 220,
            level = UserLevel.MAESTRO
        ),
        User(
            id = "user_car_1",
            name = "Carlos Mendoza",
            email = "carlos.mendoza@servilocal.co",
            passwordHash = PasswordHasher.hash("servilocal2026"),
            role = UserRole.USER,
            points = 120,
            level = UserLevel.PROFESIONAL
        ),
        User(
            id = "mod_ale_1",
            name = "Alejandro Vargas",
            email = "alejandro.vargas@staff.servilocal.pe",
            passwordHash = PasswordHasher.hash("servilocal2026"),
            role = UserRole.MODERATOR,
            points = 500,
            level = UserLevel.MAESTRO
        )
    )
}
