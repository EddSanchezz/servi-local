package com.servilocal.data.repository.firebase

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.Credential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.servilocal.domain.model.User
import com.servilocal.domain.exception.UserBlockedException
import com.servilocal.domain.model.UserLevel
import com.servilocal.domain.model.UserRole
import com.servilocal.domain.model.UserProfile
import com.servilocal.domain.model.UserStatus
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.UserRepository
import com.servilocal.domain.repository.GoogleSignInResult
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

@Singleton
class FirebaseUserRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val credentialManager: CredentialManager
) : UserRepository, AuthRepository {
    private val _users = MutableStateFlow<List<User>>(emptyList())
    override val users: StateFlow<List<User>> = _users.asStateFlow()
    private val usersCollection get() = firestore.collection("users")
    private val _currentUser = MutableStateFlow(signedOutProfile())
    override val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()
    private val _moderatorUser = MutableStateFlow(signedOutModeratorProfile())
    override val moderatorUser: StateFlow<UserProfile> = _moderatorUser.asStateFlow()
    private val _blockedAccountNotice = MutableStateFlow<String?>(null)
    override val blockedAccountNotice: StateFlow<String?> = _blockedAccountNotice.asStateFlow()
    private var profileRegistration: ListenerRegistration? = null
    private var moderatorRegistration: ListenerRegistration? = null

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val firebaseUser = firebaseAuth.currentUser
        profileRegistration?.remove()
        moderatorRegistration?.remove()
        profileRegistration = null
        moderatorRegistration = null
        if (firebaseUser == null) {
            _currentUser.value = signedOutProfile()
            _moderatorUser.value = signedOutModeratorProfile()
        } else {
            profileRegistration = usersCollection.document(firebaseUser.uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    val user = snapshot.toUser(firebaseUser.uid) ?: return@addSnapshotListener
                    if (user.status == UserStatus.BLOCKED) {
                        _blockedAccountNotice.value = user.name.ifBlank { user.email }
                        auth.signOut()
                        return@addSnapshotListener
                    }
                    publish(user)
                    val profile = user.toUserProfile()
                    _currentUser.value = profile
                    if (user.role == UserRole.MODERATOR) {
                        observeModeratorProfile(firebaseUser.uid, profile)
                    } else {
                        moderatorRegistration?.remove()
                        moderatorRegistration = null
                        _moderatorUser.value = signedOutModeratorProfile()
                    }
                }
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
    }

    override suspend fun save(user: User): User {
        val uid = user.id.ifBlank { auth.currentUser?.uid ?: error("No authenticated user") }
        // El estado lo controla el sistema: al guardar se conserva el que ya existe
        // en Firestore (o ACTIVE si el documento todavía no lo tiene).
        val currentStatus = usersCollection.document(uid).get().await()
            .getString("status")
            ?.let { runCatching { UserStatus.valueOf(it) }.getOrNull() }
            ?: UserStatus.ACTIVE
        val saved = user.copy(id = uid, passwordHash = null, status = currentStatus)
        usersCollection.document(uid).set(saved.toMap(), SetOptions.merge()).await()
        publish(saved)
        return saved
    }

    override suspend fun deleteById(id: String): Boolean {
        val uid = id.ifBlank { auth.currentUser?.uid ?: return false }
        return try {
            usersCollection.document(uid).delete().await()
            _users.value = _users.value.filterNot { it.id == uid }
            // Si es la cuenta autenticada, se elimina también de Firebase Auth.
            // Puede requerir un inicio de sesión reciente; si falla, la sesión se cierra
            // igualmente y el perfil ya no existe en Firestore.
            auth.currentUser
                ?.takeIf { it.uid == uid }
                ?.let { current -> runCatching { current.delete().await() } }
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun findById(id: String): User? =
        usersCollection.document(id).get().await().toUser(id)?.also(::publish)

    /** User documents contain PII; clients may only resolve their own email to a profile. */
    override suspend fun findByEmail(email: String): User? {
        val firebaseUser = auth.currentUser ?: return null
        if (!firebaseUser.email.equals(email.trim(), ignoreCase = true)) return null
        return findById(firebaseUser.uid)
    }

    override suspend fun login(email: String, password: String): User? {
        val firebaseUser = try {
            auth.signInWithEmailAndPassword(email.trim(), password).await().user
        } catch (error: FirebaseAuthException) {
            if (error.errorCode in INVALID_LOGIN_CODES) return null
            throw error
        }
        // Keep the credential UID: a concurrent auth-state callback may sign out
        // a blocked account before the profile read below completes.
        val user = firebaseUser?.uid?.let { findById(it) }
        if (user?.status == UserStatus.BLOCKED) {
            _blockedAccountNotice.value = user.name.ifBlank { user.email }
            auth.signOut()
            throw UserBlockedException()
        }
        if (user == null && _blockedAccountNotice.value != null) throw UserBlockedException()
        return user
    }

    override suspend fun clearBlockedAccountNotice() {
        _blockedAccountNotice.value = null
    }

    override suspend fun createAccount(name: String, email: String, password: String): User? {
        val credential = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val firebaseUser = credential.user ?: return null
        val profile = User(
            id = firebaseUser.uid,
            name = name.trim(),
            email = email.trim().lowercase(),
            role = UserRole.USER,
            level = UserLevel.PRINCIPIANTE,
            status = UserStatus.ACTIVE
        )
        return try {
            usersCollection.document(firebaseUser.uid).set(profile.toMap()).await()
            publish(profile)
            profile
        } catch (writeFailure: Exception) {
            runCatching { firebaseUser.delete().await() }
            throw writeFailure
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Boolean {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            true
        } catch (error: FirebaseAuthException) {
            if (error.errorCode == ERROR_USER_NOT_FOUND) true else throw error
        }
    }

    override suspend fun confirmPasswordResetCode(code: String, newPassword: String): Boolean {
        auth.confirmPasswordReset(code, newPassword).await()
        return true
    }

    override suspend fun updateCurrentUser(name: String, email: String) {
        val firebaseUser = auth.currentUser ?: error("No authenticated user")
        val normalizedEmail = email.trim().lowercase()
        if (!firebaseUser.email.equals(normalizedEmail, ignoreCase = true)) {
            firebaseUser.updateEmail(normalizedEmail).await()
        }
        usersCollection.document(firebaseUser.uid).update(
            mapOf("name" to name.trim(), "email" to normalizedEmail)
        ).await()
    }

    override suspend fun updateCurrentUserPhoto(photoUrl: String?) {
        val uid = auth.currentUser?.uid ?: error("No authenticated user")
        usersCollection.document(uid).update("photoUrl", photoUrl).await()
    }

    override suspend fun syncCurrentUser(user: User) {
        publish(user)
        _currentUser.value = user.toUserProfile()
    }

    override suspend fun toggleModeratorShift() {
        val uid = auth.currentUser?.uid ?: error("No authenticated user")
        val moderatorDocument = firestore.collection(MODERATORS_COLLECTION).document(uid)
        val snapshot = moderatorDocument.get().await()
        if (!snapshot.exists()) throw SecurityException("El usuario no está registrado como moderador.")
        moderatorDocument.update("activeShift", !(snapshot.getBoolean("activeShift") ?: true)).await()
    }

    override suspend fun signInWithGoogle(activity: Activity): GoogleSignInResult {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(activity.getString(com.servilocal.R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credential = try {
            credentialManager.getCredential(activity, request).credential
        } catch (_: GetCredentialCancellationException) {
            return GoogleSignInResult.Cancelled
        } catch (_: GetCredentialException) {
            return GoogleSignInResult.ProviderError
        }

        val googleCredential = try {
            if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                return GoogleSignInResult.ProviderError
            }
            GoogleIdTokenCredential.createFrom(credential.data)
        } catch (_: GoogleIdTokenParsingException) {
            return GoogleSignInResult.ProviderError
        }

        val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
        val firebaseUser = try {
            auth.signInWithCredential(firebaseCredential).await().user
        } catch (error: FirebaseAuthException) {
            return when {
                error.errorCode == "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
                    GoogleSignInResult.AccountConflict
                error.errorCode == "ERROR_NETWORK_REQUEST_FAILED" ->
                    GoogleSignInResult.NetworkError
                else -> GoogleSignInResult.ProviderError
            }
        } catch (_: Exception) {
            return GoogleSignInResult.NetworkError
        } ?: return GoogleSignInResult.ProviderError

        return try {
            val existing = usersCollection.document(firebaseUser.uid).get().await().toUser(firebaseUser.uid)
            if (existing != null) {
                if (existing.status == UserStatus.BLOCKED) {
                    _blockedAccountNotice.value = existing.name.ifBlank { existing.email }
                    auth.signOut()
                    return GoogleSignInResult.UserBlocked
                }
                publish(existing)
                GoogleSignInResult.Success(existing)
            } else {
                val profile = User(
                    id = firebaseUser.uid,
                    name = firebaseUser.displayName.orEmpty().ifBlank { "Usuario" },
                    email = firebaseUser.email.orEmpty(),
                    role = UserRole.USER,
                    level = UserLevel.PRINCIPIANTE,
                    status = UserStatus.ACTIVE,
                    photoUrl = null
                )
                usersCollection.document(firebaseUser.uid).set(profile.toMap()).await()
                publish(profile)
                GoogleSignInResult.Success(profile)
            }
        } catch (_: Exception) {
            GoogleSignInResult.NetworkError
        }
    }

    private fun User.toMap(): Map<String, Any?> = mapOf(
        "name" to name,
        "email" to email,
        "role" to role.name,
        "points" to points,
        "level" to level.name,
        "status" to status.name,
        "createdAtMillis" to createdAtMillis,
        "photoUrl" to photoUrl
    )

    private fun publish(user: User) {
        _users.value = (_users.value.filterNot { it.id == user.id } + user)
    }

    private fun observeModeratorProfile(uid: String, profile: UserProfile) {
        moderatorRegistration?.remove()
        moderatorRegistration = firestore.collection(MODERATORS_COLLECTION).document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                _moderatorUser.value = profile.copy(
                    staffId = snapshot.getString("staffId") ?: "MOD-${uid.takeLast(4).uppercase()}",
                    activeShift = snapshot.getBoolean("activeShift") ?: true,
                    workingHours = snapshot.getString("workingHours")
                        ?: "Lunes a viernes · 8:00 a. m. - 4:30 p. m."
                )
            }
    }

    private fun User.toUserProfile(): UserProfile {
        val isModerator = role == UserRole.MODERATOR
        return UserProfile(
            id = id,
            name = name,
            email = email,
            photoUrl = photoUrl,
            role = role,
            level = level.displayName(),
            points = points,
            nextLevelPoints = level.next?.minPoints ?: 1000,
            staffId = if (isModerator) "MOD-${id.takeLast(4).uppercase()}" else null,
            activeShift = true,
            workingHours = if (isModerator) "Lunes a viernes · 8:00 a. m. - 4:30 p. m."
            else "Lunes a sábado · 8:00 a. m. - 6:00 p. m."
        )
    }

    private fun signedOutProfile() = UserProfile(
        id = "",
        name = "",
        email = "",
        role = UserRole.USER
    )

    private fun signedOutModeratorProfile() = UserProfile(
        id = "",
        name = "",
        email = "",
        role = UserRole.MODERATOR,
        activeShift = false
    )

    private companion object {
        const val MODERATORS_COLLECTION = "moderators"
        val INVALID_LOGIN_CODES: Set<String> = setOf(
            "ERROR_INVALID_CREDENTIAL",
            ERROR_USER_NOT_FOUND,
            "ERROR_WRONG_PASSWORD",
            "ERROR_USER_DISABLED",
            "ERROR_INVALID_EMAIL"
        )
        const val ERROR_USER_NOT_FOUND = "ERROR_USER_NOT_FOUND"
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toUser(defaultId: String? = id): User? {
        if (!exists()) return null
        return User(
            id = defaultId ?: return null,
            name = getString("name").orEmpty(),
            email = getString("email").orEmpty(),
            role = runCatching { UserRole.valueOf(getString("role").orEmpty()) }.getOrDefault(UserRole.USER),
            points = getLong("points")?.toInt() ?: 0,
            level = runCatching { UserLevel.valueOf(getString("level").orEmpty()) }.getOrDefault(UserLevel.PRINCIPIANTE),
            status = runCatching { UserStatus.valueOf(getString("status").orEmpty()) }.getOrDefault(UserStatus.ACTIVE),
            createdAtMillis = getLong("createdAtMillis") ?: System.currentTimeMillis(),
            photoUrl = getString("photoUrl")
        )
    }
}
