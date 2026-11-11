package com.servilocal.data.repository.firebase

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.servilocal.domain.model.PostCategory
import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.model.ServicePost
import com.servilocal.domain.model.UserRole
import com.servilocal.domain.repository.PostRepository
import com.servilocal.domain.repository.SessionRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await
import java.util.Locale

/**
 * Firestore-backed publication repository. Location coordinates are embedded in
 * each post document as a `locations` array so the feed needs one document read
 * per publication rather than a second lookup for every marker.
 */
@Singleton
class FirebasePostRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val sessionRepository: SessionRepository
) : PostRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val postsCollection get() = firestore.collection(POSTS_COLLECTION)

    override val posts = sessionRepository.session
        .flatMapLatest { session ->
            if (session == null) {
                flowOf(emptyList())
            } else {
                val approved = observePosts(
                    postsCollection.whereEqualTo(FIELD_STATUS, PostStatus.APPROVED.name)
                )
                val owned = observePosts(
                    postsCollection.whereEqualTo(FIELD_PROVIDER_ID, session.userId)
                )
                if (session.role == UserRole.MODERATOR) {
                    val pending = observePosts(
                        postsCollection.whereEqualTo(FIELD_STATUS, PostStatus.PENDING_VERIFICATION.name)
                    )
                    combine(approved, owned, pending) { publicPosts, ownPosts, pendingPosts ->
                        (publicPosts + ownPosts + pendingPosts).distinctBy(ServicePost::id)
                            .sortedByDescending(ServicePost::createdAtMillis)
                    }
                } else {
                    combine(approved, owned) { publicPosts, ownPosts ->
                        (publicPosts + ownPosts).distinctBy(ServicePost::id)
                            .sortedByDescending(ServicePost::createdAtMillis)
                    }
                }
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override suspend fun createPost(
        title: String,
        category: String,
        description: String,
        minPrice: Int,
        maxPrice: Int,
        isPriceToAgree: Boolean,
        location: String,
        latitude: Double?,
        longitude: Double?,
        photos: List<String>
    ): ServicePost {
        val session = sessionRepository.session.value
            ?: throw IllegalStateException("Inicia sesión antes de crear una publicación.")
        val user = firestore.collection(USERS_COLLECTION).document(session.userId).get().await()
        val providerName = user.getString("name").orEmpty()
        val providerLevel = user.getString("level")
            ?.let { runCatching { com.servilocal.domain.model.UserLevel.valueOf(it).displayName() }.getOrNull() }
            ?: "Nivel 1: Principiante"

        require(title.isNotBlank()) { "El título es obligatorio." }
        require(description.isNotBlank()) { "La descripción es obligatoria." }
        require(location.isNotBlank()) { "La descripción de ubicación es obligatoria." }
        require(latitude.isValidLatitude() && longitude.isValidLongitude()) { "Selecciona un punto válido en el mapa." }
        require(photos.isNotEmpty() && photos.all { it.isSecureUrl() }) { "Agrega al menos una imagen válida." }
        require(isPriceToAgree || (minPrice > 0 && maxPrice >= minPrice)) { "El rango de precios no es válido." }
        val categoryEnum = parseCategory(category)
            ?: throw IllegalArgumentException("Selecciona una categoría válida.")

        val reference = postsCollection.document()
        val createdAt = System.currentTimeMillis()
        val post = ServicePost(
            id = reference.id,
            title = title.trim(),
            category = categoryEnum.toUiCategory(),
            description = description.trim(),
            providerName = providerName,
            providerId = session.userId,
            providerLevel = providerLevel,
            minPrice = minPrice,
            maxPrice = maxPrice,
            isPriceToAgree = isPriceToAgree,
            location = location.trim(),
            latitude = latitude ?: error("La latitud es obligatoria."),
            longitude = longitude ?: error("La longitud es obligatoria."),
            coverageRadiusKm = DEFAULT_COVERAGE_RADIUS_KM,
            distance = distanceFromArmenia(latitude, longitude),
            rating = 5f,
            reviewCount = 0,
            upvotesCount = 0,
            userUpvoted = false,
            photos = photos,
            status = PostStatus.PENDING_VERIFICATION,
            responseTime = "< 20 min",
            comments = emptyList(),
            createdAtMillis = createdAt
        )
        reference.set(post.toFirestoreMap()).await()
        return post
    }

    override suspend fun updatePost(
        postId: String,
        title: String,
        category: String,
        description: String,
        minPrice: Int,
        maxPrice: Int,
        isPriceToAgree: Boolean,
        location: String,
        latitude: Double?,
        longitude: Double?,
        photos: List<String>?
    ): Boolean {
        val current = posts.value.firstOrNull { it.id == postId } ?: return false
        val uid = sessionRepository.session.value?.userId ?: return false
        if (current.providerId != uid || current.status != PostStatus.APPROVED) return false
        if (title.isBlank() || description.isBlank() || location.isBlank()) return false
        if (!isPriceToAgree && (minPrice <= 0 || maxPrice < minPrice)) return false
        if (latitude != null && !latitude.isValidLatitude()) return false
        if (longitude != null && !longitude.isValidLongitude()) return false
        if ((latitude == null) != (longitude == null)) return false
        if (photos != null && (photos.isEmpty() || photos.any { !it.isSecureUrl() })) return false
        val categoryEnum = parseCategory(category) ?: return false
        val updatedLatitude = latitude ?: current.latitude ?: return false
        val updatedLongitude = longitude ?: current.longitude ?: return false

        postsCollection.document(postId).update(
            mapOf(
                "title" to title.trim(),
                "category" to categoryEnum.name,
                "description" to description.trim(),
                "minPrice" to minPrice,
                "maxPrice" to maxPrice,
                "isPriceToAgree" to isPriceToAgree,
                "locationDescription" to location.trim(),
                "locations" to listOf(locationMap(updatedLatitude, updatedLongitude)),
                "photos" to (photos ?: current.photos)
            )
        ).await()
        return true
    }

    override suspend fun deletePost(postId: String): Boolean {
        val current = posts.value.firstOrNull { it.id == postId } ?: return false
        val uid = sessionRepository.session.value?.userId ?: return false
        if (current.providerId != uid) return false
        postsCollection.document(postId).delete().await()
        return true
    }

    override suspend fun finishPost(postId: String): Boolean {
        val current = posts.value.firstOrNull { it.id == postId } ?: return false
        val uid = sessionRepository.session.value?.userId ?: return false
        if (current.providerId != uid || current.status != PostStatus.APPROVED) return false
        postsCollection.document(postId).update(FIELD_STATUS, PostStatus.FINISHED.name).await()
        return true
    }

    override suspend fun approvePost(postId: String) {
        val session = sessionRepository.session.value ?: return
        if (session.role != UserRole.MODERATOR) return
        postsCollection.document(postId).update(
            mapOf(
                FIELD_STATUS to PostStatus.APPROVED.name,
                "moderatorId" to session.userId,
                "moderatedAtMillis" to System.currentTimeMillis()
            )
        ).await()
    }

    override suspend fun rejectPost(postId: String, reason: String, notes: String?) {
        val session = sessionRepository.session.value ?: return
        if (session.role != UserRole.MODERATOR || reason.isBlank()) return
        postsCollection.document(postId).update(
            mapOf(
                FIELD_STATUS to PostStatus.REJECTED.name,
                "moderatorId" to session.userId,
                "rejectionReason" to reason.trim(),
                "rejectionNotes" to notes?.trim(),
                "moderatedAtMillis" to System.currentTimeMillis()
            )
        ).await()
    }

    override suspend fun moderatePost(
        postId: String,
        status: PostStatus,
        reason: String?,
        notes: String?
    ): Boolean {
        val session = sessionRepository.session.value ?: return false
        if (session.role != UserRole.MODERATOR || status !in setOf(PostStatus.REJECTED, PostStatus.FINISHED)) return false
        if (status == PostStatus.REJECTED && reason.isNullOrBlank()) return false
        postsCollection.document(postId).update(
            mapOf(
                FIELD_STATUS to status.name,
                "moderatorId" to session.userId,
                "rejectionReason" to if (status == PostStatus.REJECTED) reason?.trim() else null,
                "rejectionNotes" to if (status == PostStatus.REJECTED) notes?.trim() else null,
                "moderatedAtMillis" to System.currentTimeMillis()
            )
        ).await()
        return true
    }

    private fun observePosts(query: Query): Flow<List<ServicePost>> = callbackFlow {
        val registration: ListenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
            } else {
                trySend(snapshot?.documents.orEmpty().mapNotNull { it.toServicePost() })
            }
        }
        awaitClose { registration.remove() }
    }

    private fun DocumentSnapshot.toServicePost(): ServicePost? {
        if (!exists()) return null
        val location = getString("locationDescription") ?: getString("location").orEmpty()
        val coordinates = (get("locations") as? List<*>)
            ?.firstOrNull() as? Map<*, *>
        val latitude = (coordinates?.get("latitude") as? Number)?.toDouble()
            ?: (get("latitude") as? Number)?.toDouble()
        val longitude = (coordinates?.get("longitude") as? Number)?.toDouble()
            ?: (get("longitude") as? Number)?.toDouble()
        val categoryValue = getString("category").orEmpty()
        val status = runCatching { PostStatus.valueOf(getString(FIELD_STATUS).orEmpty()) }
            .getOrDefault(PostStatus.PENDING_VERIFICATION)
        return ServicePost(
            id = id,
            title = getString("title").orEmpty(),
            category = parseCategory(categoryValue)?.toUiCategory() ?: categoryValue,
            description = getString("description").orEmpty(),
            providerName = getString("providerName").orEmpty(),
            providerId = getString(FIELD_PROVIDER_ID),
            providerAvatar = getString("providerAvatar"),
            providerLevel = getString("providerLevel") ?: "Principiante",
            minPrice = getLong("minPrice")?.toInt() ?: 0,
            maxPrice = getLong("maxPrice")?.toInt() ?: 0,
            isPriceToAgree = getBoolean("isPriceToAgree") ?: false,
            location = location,
            latitude = latitude,
            longitude = longitude,
            coverageRadiusKm = (coordinates?.get("coverageRadiusKm") as? Number)?.toFloat() ?: DEFAULT_COVERAGE_RADIUS_KM,
            distance = if (latitude != null && longitude != null) distanceFromArmenia(latitude, longitude) else "Distancia no calculada",
            rating = getDouble("rating")?.toFloat() ?: 5f,
            reviewCount = getLong("reviewCount")?.toInt() ?: 0,
            upvotesCount = getLong("upvotesCount")?.toInt() ?: 0,
            userUpvoted = false,
            photos = (get("photos") as? List<*>)?.filterIsInstance<String>().orEmpty(),
            status = status,
            warrantyMonths = getLong("warrantyMonths")?.toInt() ?: 0,
            responseTime = getString("responseTime") ?: "",
            comments = emptyList(),
            rejectionReason = getString("rejectionReason"),
            rejectionNotes = getString("rejectionNotes"),
            createdAtMillis = getLong("createdAtMillis") ?: 0L
        )
    }

    private fun ServicePost.toFirestoreMap(): Map<String, Any?> = mapOf(
        FIELD_PROVIDER_ID to providerId,
        "providerName" to providerName,
        "providerLevel" to providerLevel,
        "title" to title,
        "category" to parseCategory(category)?.name,
        "description" to description,
        "minPrice" to minPrice,
        "maxPrice" to maxPrice,
        "isPriceToAgree" to isPriceToAgree,
        "locationDescription" to location,
        "locations" to listOf(locationMap(latitude!!, longitude!!)),
        "photos" to photos,
        FIELD_STATUS to status.name,
        "upvotesCount" to 0,
        "reviewCount" to 0,
        "rating" to rating.toDouble(),
        "warrantyMonths" to warrantyMonths,
        "responseTime" to responseTime,
        "createdAtMillis" to createdAtMillis
    )

    private fun locationMap(latitude: Double, longitude: Double): Map<String, Any> = mapOf(
        "latitude" to latitude,
        "longitude" to longitude,
        "coverageRadiusKm" to DEFAULT_COVERAGE_RADIUS_KM.toDouble()
    )

    private fun parseCategory(value: String): PostCategory? = when (
        value.trim().lowercase(Locale.ROOT)
            .replace("á", "a").replace("é", "e").replace("í", "i")
            .replace("ó", "o").replace("ú", "u")
    ) {
        "hogar" -> PostCategory.HOGAR
        "educacion" -> PostCategory.EDUCACION
        "mascotas" -> PostCategory.MASCOTAS
        "tecnologia", "electricidad" -> PostCategory.TECNOLOGIA
        "transporte" -> PostCategory.TRANSPORTE
        else -> null
    }

    private fun PostCategory.toUiCategory(): String = when (this) {
        PostCategory.HOGAR -> "Hogar"
        PostCategory.EDUCACION -> "Educación"
        PostCategory.MASCOTAS -> "Mascotas"
        PostCategory.TECNOLOGIA -> "Tecnología"
        PostCategory.TRANSPORTE -> "Transporte"
    }

    private fun Double?.isValidLatitude(): Boolean = this != null && isFinite() && this in -90.0..90.0
    private fun Double?.isValidLongitude(): Boolean = this != null && isFinite() && this in -180.0..180.0
    private fun String.isSecureUrl(): Boolean = startsWith("https://", ignoreCase = true)

    private fun distanceFromArmenia(latitude: Double, longitude: Double): String {
        val earthRadiusKm = 6_371.0
        val latDelta = Math.toRadians(latitude - 4.5339)
        val lonDelta = Math.toRadians(longitude - -75.6811)
        val a = kotlin.math.sin(latDelta / 2).let { it * it } +
            kotlin.math.cos(Math.toRadians(4.5339)) * kotlin.math.cos(Math.toRadians(latitude)) *
            kotlin.math.sin(lonDelta / 2).let { it * it }
        val km = 2 * earthRadiusKm * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return String.format(Locale.ROOT, "A %.1f km", km)
    }

    private companion object {
        const val POSTS_COLLECTION = "posts"
        const val USERS_COLLECTION = "users"
        const val FIELD_STATUS = "status"
        const val FIELD_PROVIDER_ID = "providerId"
        const val DEFAULT_COVERAGE_RADIUS_KM = 15f
    }
}
