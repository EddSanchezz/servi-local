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
import com.google.firebase.firestore.SetOptions
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.servilocal.domain.model.User
import com.servilocal.domain.model.UserLevel
import com.servilocal.domain.model.UserRole
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
) : UserRepository {
    private val _users = MutableStateFlow<List<User>>(emptyList())
    override val users: StateFlow<List<User>> = _users.asStateFlow()
    private val usersCollection get() = firestore.collection("users")

    override suspend fun save(user: User): User {
        val uid = user.id.ifBlank { auth.currentUser?.uid ?: error("No authenticated user") }
        val saved = user.copy(id = uid, passwordHash = null)
        usersCollection.document(uid).set(saved.toMap(), SetOptions.merge()).await()
        publish(saved)
        return saved
    }

    override suspend fun findById(id: String): User? =
        usersCollection.document(id).get().await().toUser(id)?.also(::publish)

    override suspend fun findByEmail(email: String): User? {
        val snapshot = usersCollection.whereEqualTo("email", email.trim().lowercase()).limit(1).get().await()
        return snapshot.documents.firstOrNull()?.toUser()?.also(::publish)
    }

    override suspend fun login(email: String, password: String): User? {
        try {
            auth.signInWithEmailAndPassword(email.trim(), password).await()
        } catch (error: FirebaseAuthException) {
            if (error.errorCode in INVALID_LOGIN_CODES) return null
            throw error
        }
        return auth.currentUser?.uid?.let { findById(it) }
    }

    override suspend fun createAccount(name: String, email: String, password: String): User? {
        val credential = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val firebaseUser = credential.user ?: return null
        val profile = User(
            id = firebaseUser.uid,
            name = name.trim(),
            email = email.trim().lowercase(),
            role = UserRole.USER,
            level = UserLevel.PRINCIPIANTE
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
                publish(existing)
                GoogleSignInResult.Success(existing)
            } else {
                val profile = User(
                    id = firebaseUser.uid,
                    name = firebaseUser.displayName.orEmpty().ifBlank { "Usuario" },
                    email = firebaseUser.email.orEmpty(),
                    role = UserRole.USER,
                    level = UserLevel.PRINCIPIANTE,
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
        "createdAtMillis" to createdAtMillis,
        "photoUrl" to photoUrl
    )

    private fun publish(user: User) {
        _users.value = (_users.value.filterNot { it.id == user.id } + user)
    }

    private companion object {
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
            createdAtMillis = getLong("createdAtMillis") ?: System.currentTimeMillis(),
            photoUrl = getString("photoUrl")
        )
    }
}
