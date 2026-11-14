package com.servilocal.data.repository.memory

import com.servilocal.domain.model.UserRole
import com.servilocal.domain.model.UserSession
import com.servilocal.domain.model.UserStatus
import com.servilocal.domain.repository.SessionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth?,
    private val firestore: FirebaseFirestore?
) : SessionRepository {
    constructor() : this(null, null)

    private val _session = MutableStateFlow<UserSession?>(null)
    override val session: StateFlow<UserSession?> = _session.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser == null) {
            _session.value = null
        } else {
            _session.value = null
            scope.launch {
                val profile = runCatching {
                    firestore?.collection("users")?.document(firebaseUser.uid)?.get()?.await()
                }.getOrNull()
                // A blocked account can trigger sign-out while this Firestore read is in
                // flight. Never restore its session from a stale read or permission error.
                if (auth?.currentUser?.uid != firebaseUser.uid || profile == null || !profile.exists()) {
                    return@launch
                }
                if (profile.getString("status") == UserStatus.BLOCKED.name) return@launch

                val role = profile.getString("role")
                    ?.let { runCatching { UserRole.valueOf(it) }.getOrNull() }
                    ?: UserRole.USER
                if (auth.currentUser?.uid == firebaseUser.uid) {
                    _session.value = UserSession(firebaseUser.uid, role)
                }
            }
        }
    }

    init {
        auth?.addAuthStateListener(listener)
    }

    override suspend fun saveSession(userId: String, role: UserRole) {
        _session.value = UserSession(userId = userId, role = role)
    }

    override suspend fun clearSession() {
        auth?.signOut()
        _session.value = null
    }
}
