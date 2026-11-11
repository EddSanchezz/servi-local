package com.servilocal.data.repository.memory

import com.servilocal.core.security.PasswordHasher
import com.servilocal.domain.exception.EmailAlreadyInUseException
import com.servilocal.domain.model.User
import com.servilocal.domain.model.UserLevel
import com.servilocal.domain.model.UserRole
import com.servilocal.domain.model.UserStatus
import com.servilocal.domain.repository.UserRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Singleton
class UserRepositoryImpl @Inject constructor() : UserRepository {

    private val _users = MutableStateFlow(seedUsers())
    override val users: StateFlow<List<User>> = _users.asStateFlow()
    override suspend fun save(user: User): User {
        // El estado lo controla el sistema: no se permite cambiarlo desde un guardado normal.
        val currentStatus = _users.value.firstOrNull { it.id == user.id }?.status
        val userToSave = user.copy(status = currentStatus ?: UserStatus.ACTIVE)
        _users.update { current ->
            current.filterNot { it.id == userToSave.id } + userToSave
        }
        return userToSave
    }

    override suspend fun deleteById(id: String): Boolean {
        val exists = _users.value.any { it.id == id }
        if (!exists) return false
        _users.update { current -> current.filterNot { it.id == id } }
        return true
    }

    override suspend fun findById(id: String): User? =
        _users.value.firstOrNull { it.id == id }

    override suspend fun findByEmail(email: String): User? {
        val normalizedEmail = email.trim().lowercase()
        return _users.value.firstOrNull { it.email.lowercase() == normalizedEmail }
    }

    override suspend fun login(email: String, password: String): User? {
        val user = findByEmail(email) ?: return null
        return user.takeIf { it.passwordHash == PasswordHasher.hash(password) }
    }

    override suspend fun createAccount(name: String, email: String, password: String): User {
        if (findByEmail(email) != null) throw EmailAlreadyInUseException()
        return save(
            User(
                id = "memory-${_users.value.size + 1}",
                name = name,
                email = email,
                passwordHash = PasswordHasher.hash(password),
                role = UserRole.USER,
                status = UserStatus.ACTIVE
            )
        )
    }

    override suspend fun sendPasswordResetEmail(email: String): Boolean = true

    override suspend fun confirmPasswordResetCode(code: String, newPassword: String): Boolean = false

    private fun seedUsers(): List<User> = listOf(
        // Usuario principal (cliente/provider)
        User(
            id = "1",
            name = "Valentina Morales",
            email = "valentina@gmail.com",
            passwordHash = PasswordHasher.hash("password123"),
            role = UserRole.USER,
            points = 220,
            level = UserLevel.PROFESIONAL
        ),
        // Carlos Mendoza - Servicios eléctricos y plomería (pub_1, pub_2)
        User(
            id = "2",
            name = "Carlos Mendoza",
            email = "carlos@gmail.com",
            passwordHash = PasswordHasher.hash("password123"),
            role = UserRole.USER,
            points = 120,
            level = UserLevel.PROFESIONAL
        ),
        // Moderador del sistema
        User(
            id = "3",
            name = "Alejandro Vargas",
            email = "alejandro@gmail.com",
            passwordHash = PasswordHasher.hash("password123"),
            role = UserRole.MODERATOR,
            points = 500,
            level = UserLevel.MAESTRO
        ),
        // Matías S. - Servicios de mascotas (pub_4)
        User(
            id = "4",
            name = "Matías Sepúlveda",
            email = "matias@gmail.com",
            passwordHash = PasswordHasher.hash("password123"),
            role = UserRole.USER,
            points = 80,
            level = UserLevel.PRINCIPIANTE
        ),
        // Rodrigo Tapia - Cerrajería (pub_5)
        User(
            id = "5",
            name = "Rodrigo Tapia",
            email = "rodrigo@gmail.com",
            passwordHash = PasswordHasher.hash("password123"),
            role = UserRole.USER,
            points = 25,
            level = UserLevel.PRINCIPIANTE
        ),
        // Fernando Lucas - Electricidad (pub_6)
        User(
            id = "6",
            name = "Fernando Lucas",
            email = "fernando@gmail.com",
            passwordHash = PasswordHasher.hash("password123"),
            role = UserRole.USER,
            points = 15,
            level = UserLevel.PRINCIPIANTE
        ),
        // Francisca Valenzuela - Cliente que comenta y solicita servicios
        User(
            id = "7",
            name = "Francisca Valenzuela",
            email = "francisca@gmail.com",
            passwordHash = PasswordHasher.hash("password123"),
            role = UserRole.USER,
            points = 45,
            level = UserLevel.PRINCIPIANTE
        ),
        // Andrés Gómez - Cliente de servicios de plomería
        User(
            id = "8",
            name = "Andrés Gómez",
            email = "andres@gmail.com",
            passwordHash = PasswordHasher.hash("password123"),
            role = UserRole.USER,
            points = 30,
            level = UserLevel.PRINCIPIANTE
        ),
        // Mariana Rivas - Cliente que solicita servicios
        User(
            id = "9",
            name = "Mariana Rivas",
            email = "mariana@gmail.com",
            passwordHash = PasswordHasher.hash("password123"),
            role = UserRole.USER,
            points = 20,
            level = UserLevel.PRINCIPIANTE
        ),
        // Jorge Palacios - Cliente de servicios de pintura
        User(
            id = "10",
            name = "Jorge Palacios",
            email = "jorge@gmail.com",
            passwordHash = PasswordHasher.hash("password123"),
            role = UserRole.USER,
            points = 55,
            level = UserLevel.PRINCIPIANTE
        )
    )
}
