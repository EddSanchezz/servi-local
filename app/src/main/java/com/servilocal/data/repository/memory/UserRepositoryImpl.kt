package com.servilocal.data.repository.memory

import com.servilocal.core.security.PasswordHasher
import com.servilocal.domain.model.User
import com.servilocal.domain.model.UserLevel
import com.servilocal.domain.model.UserRole
import com.servilocal.domain.repository.UserRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong

@Singleton
class UserRepositoryImpl @Inject constructor() : UserRepository {

    private val _users = MutableStateFlow(seedUsers())
    override val users: StateFlow<List<User>> = _users.asStateFlow()
    private val idCounter = AtomicLong(seedUsers().maxOfOrNull { it.id } ?: 0L)

    override suspend fun save(user: User): User {
        val userToSave = if (user.id == 0L) {
            user.copy(id = idCounter.incrementAndGet())
        } else {
            user
        }
        _users.update { current ->
            current.filterNot { it.id == userToSave.id } + userToSave
        }
        return userToSave
    }

    override suspend fun findById(id: Long): User? =
        _users.value.firstOrNull { it.id == id }

    override suspend fun findByEmail(email: String): User? {
        val normalizedEmail = email.trim().lowercase()
        return _users.value.firstOrNull { it.email.lowercase() == normalizedEmail }
    }

    override suspend fun login(email: String, password: String): User? {
        val user = findByEmail(email) ?: return null
        return user.takeIf { it.passwordHash == PasswordHasher.hash(password) }
    }

    override suspend fun updatePassword(userId: Long, passwordHash: String) {
        _users.update { current ->
            current.map { user ->
                if (user.id == userId) user.copy(passwordHash = passwordHash) else user
            }
        }
    }

    private fun seedUsers(): List<User> = listOf(
        // Usuario principal (cliente/provider)
        User(
            id = 1L,
            name = "Valentina Morales",
            email = "valentina@gmail.com",
            passwordHash = PasswordHasher.hash("password123"),
            role = UserRole.USER,
            points = 220,
            level = UserLevel.PROFESIONAL
        ),
        // Carlos Mendoza - Servicios eléctricos y plomería (pub_1, pub_2)
        User(
            id = 2L,
            name = "Carlos Mendoza",
            email = "carlos@gmail.com",
            passwordHash = PasswordHasher.hash("password123"),
            role = UserRole.USER,
            points = 120,
            level = UserLevel.PROFESIONAL
        ),
        // Moderador del sistema
        User(
            id = 3L,
            name = "Alejandro Vargas",
            email = "alejandro@gmail.com",
            passwordHash = PasswordHasher.hash("password123"),
            role = UserRole.MODERATOR,
            points = 500,
            level = UserLevel.MAESTRO
        ),
        // Matías S. - Servicios de mascotas (pub_4)
        User(
            id = 4L,
            name = "Matías Sepúlveda",
            email = "matias@gmail.com",
            passwordHash = PasswordHasher.hash("password123"),
            role = UserRole.USER,
            points = 80,
            level = UserLevel.PRINCIPIANTE
        ),
        // Rodrigo Tapia - Cerrajería (pub_5)
        User(
            id = 5L,
            name = "Rodrigo Tapia",
            email = "rodrigo@gmail.com",
            passwordHash = PasswordHasher.hash("password123"),
            role = UserRole.USER,
            points = 25,
            level = UserLevel.PRINCIPIANTE
        ),
        // Fernando Lucas - Electricidad (pub_6)
        User(
            id = 6L,
            name = "Fernando Lucas",
            email = "fernando@gmail.com",
            passwordHash = PasswordHasher.hash("password123"),
            role = UserRole.USER,
            points = 15,
            level = UserLevel.PRINCIPIANTE
        ),
        // Francisca Valenzuela - Cliente que comenta y solicita servicios
        User(
            id = 7L,
            name = "Francisca Valenzuela",
            email = "francisca@gmail.com",
            passwordHash = PasswordHasher.hash("password123"),
            role = UserRole.USER,
            points = 45,
            level = UserLevel.PRINCIPIANTE
        ),
        // Andrés Gómez - Cliente de servicios de plomería
        User(
            id = 8L,
            name = "Andrés Gómez",
            email = "andres@gmail.com",
            passwordHash = PasswordHasher.hash("password123"),
            role = UserRole.USER,
            points = 30,
            level = UserLevel.PRINCIPIANTE
        ),
        // Mariana Rivas - Cliente que solicita servicios
        User(
            id = 9L,
            name = "Mariana Rivas",
            email = "mariana@gmail.com",
            passwordHash = PasswordHasher.hash("password123"),
            role = UserRole.USER,
            points = 20,
            level = UserLevel.PRINCIPIANTE
        ),
        // Jorge Palacios - Cliente de servicios de pintura
        User(
            id = 10L,
            name = "Jorge Palacios",
            email = "jorge@gmail.com",
            passwordHash = PasswordHasher.hash("password123"),
            role = UserRole.USER,
            points = 55,
            level = UserLevel.PRINCIPIANTE
        )
    )
}
