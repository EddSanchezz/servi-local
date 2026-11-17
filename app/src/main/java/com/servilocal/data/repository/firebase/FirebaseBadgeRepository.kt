package com.servilocal.data.repository.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.servilocal.domain.model.Badge
import com.servilocal.domain.model.BadgeCatalog
import com.servilocal.domain.model.UserBadge
import com.servilocal.domain.repository.BadgeRepository
import com.servilocal.domain.repository.SessionRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Keeps the fixed badge catalog in code and observes earned badges from Firestore. */
@Singleton
class FirebaseBadgeRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val sessionRepository: SessionRepository
) : BadgeRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _badges = MutableStateFlow(BadgeCatalog.all)
    override val badges: StateFlow<List<Badge>> = _badges.asStateFlow()

    private val _userBadges = MutableStateFlow<List<UserBadge>>(emptyList())
    override val userBadges: StateFlow<List<UserBadge>> = _userBadges.asStateFlow()

    @Volatile
    private var activeUserId: String? = null
    private var badgeRegistration: ListenerRegistration? = null

    init {
        scope.launch {
            sessionRepository.session.collectLatest { session ->
                badgeRegistration?.remove()
                badgeRegistration = null
                val userId = session?.userId?.takeIf(String::isNotBlank)
                activeUserId = userId
                _userBadges.value = emptyList()

                if (userId != null) {
                    badgeRegistration = firestore.collection(USERS_COLLECTION)
                        .document(userId)
                        .collection(BADGES_COLLECTION)
                        .addSnapshotListener { snapshot, error ->
                            if (activeUserId != userId || error != null || snapshot == null) return@addSnapshotListener
                            _userBadges.value = snapshot.documents
                                .mapNotNull { document -> document.toUserBadge(userId) }
                                .sortedBy(UserBadge::awardedAtMillis)
                        }
                }
            }
        }
    }

    override suspend fun award(userId: String, badgeId: String, sourcePostId: String?): Boolean {
        if (userId.isBlank() || badges.value.none { it.id == badgeId }) return false

        val badgeReference = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(BADGES_COLLECTION)
            .document(badgeId)
        val awardedAtMillis = System.currentTimeMillis()

        return firestore.runTransaction { transaction ->
            if (transaction.get(badgeReference).exists()) {
                false
            } else {
                val data = mutableMapOf<String, Any>(
                    "userId" to userId,
                    "badgeId" to badgeId,
                    "awardedAtMillis" to awardedAtMillis
                )
                sourcePostId?.takeIf(String::isNotBlank)?.let { data["sourcePostId"] = it }
                transaction.set(badgeReference, data)
                true
            }
        }.await()
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toUserBadge(userId: String): UserBadge? {
        val badgeId = getString("badgeId") ?: id
        if (badges.value.none { it.id == badgeId }) return null
        return UserBadge(
            id = id,
            userId = getString("userId") ?: userId,
            badgeId = badgeId,
            awardedAtMillis = getLong("awardedAtMillis") ?: 0L
        )
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val BADGES_COLLECTION = "badges"
    }
}
