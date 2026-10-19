package com.servilocal.domain.model

/*
 * Entidades de dominio alineadas 1:1 con el ERD "Marketplace de Servicios Locales".
 *
 * Convenciones:
 *  - UUID            -> String
 *  - TIMESTAMP       -> Long (milisegundos, campo *AtMillis). La UI lo formatea.
 *  - DECIMAL (COP)   -> Int (el peso colombiano no usa centavos)
 *  - FK nullable     -> String?
 *  - Los nombres de los enums conservan los que ya usa la UI; el comentario
 *    indica el valor equivalente en el ERD.
 *
 * Si alguna clase ya existe en el proyecto (por ejemplo UserRole), no la dupliquen.
 */

// ───────────────────────── ENUMS ─────────────────────────

// enum class UserRole se encuentra en UserRole.kt

/** users.level — PRINCIPIANTE, PROFESIONAL, EXPERTO, MAESTRO. Los umbrales son ajustables. */
enum class UserLevel(val minPoints: Int) {
    PRINCIPIANTE(0),
    PROFESIONAL(100),
    EXPERTO(250),
    MAESTRO(500);

    val next: UserLevel? get() = values().getOrNull(ordinal + 1)

    companion object {
        fun fromPoints(points: Int): UserLevel =
            values().last { points >= it.minPoints }
    }
}

/** posts.category */
enum class PostCategory { HOGAR, EDUCACION, MASCOTAS, TECNOLOGIA, TRANSPORTE }

/** posts.status — PENDIENTE_VERIFICACION, VERIFICADA, RECHAZADA, FINALIZADA */
enum class PostStatus {
    PENDING_VERIFICATION,   // PENDIENTE_VERIFICACION
    APPROVED,               // VERIFICADA
    REJECTED,               // RECHAZADA
    FINISHED                // FINALIZADA
}

/**
 * service_requests.status (el ERD solo dice ENUM; estos son los valores que ya usa la UI).
 * Antes se llamaba ChatStatus.
 */
enum class ServiceRequestStatus { NEW_REQUEST, PENDING, IN_PROGRESS, COMPLETED, DECLINED }

/** reports.target_type — POST, COMMENT, PROFILE, CHAT_MESSAGE */
enum class ReportTargetType { POST, COMMENT, PROFILE, CHAT_MESSAGE }

/** reports.status — PENDIENTE, REVISADO */
enum class ReportStatus { PENDING, REVIEWED }

// ───────────────────────── ENTIDADES ─────────────────────────

/** Tabla users */
data class User(
    val id: String,
    val name: String,
    val email: String,
    val passwordHash: String,
    val role: UserRole,
    val points: Int = 0,
    val level: UserLevel = UserLevel.PRINCIPIANTE,
    val createdAtMillis: Long = System.currentTimeMillis()
)

/** Tabla posts (la oferta de servicio) */
data class Post(
    val id: String,
    val userId: String,                     // autor / proveedor
    val moderatorId: String? = null,        // quien verificó o rechazó
    val title: String,
    val category: PostCategory,
    val description: String,
    val minPrice: Int,
    val maxPrice: Int,
    val status: PostStatus = PostStatus.PENDING_VERIFICATION,
    val rejectionReason: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

/** Tabla post_images (1 post -> N imágenes) */
data class PostImage(
    val id: String,
    val postId: String,
    val imageUrl: String
)

/** Tabla post_locations (1 post -> N zonas de cobertura) */
data class PostLocation(
    val id: String,
    val postId: String,
    val latitude: Double,
    val longitude: Double,
    val coverageRadiusKm: Float
)

/** Tabla comments */
data class Comment(
    val id: String,
    val postId: String,
    val userId: String,
    val content: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

/** Tabla likes ("Es importante" / "Me interesa") */
data class Like(
    val id: String,
    val userId: String,
    val postId: String
)

/**
 * Tabla service_requests.
 * El proveedor no se guarda aquí: es el userId del post al que pertenece la solicitud.
 */
data class ServiceRequest(
    val id: String,
    val postId: String,
    val clientId: String,
    val contactPhone: String,
    val exactAddress: String,
    val problemDescription: String,
    val status: ServiceRequestStatus = ServiceRequestStatus.NEW_REQUEST,
    val createdAtMillis: Long = System.currentTimeMillis()
)

/** Tabla chat_messages (cada mensaje pertenece a una solicitud) */
data class ChatMessage(
    val id: String,
    val serviceRequestId: String,
    val senderId: String,
    val message: String,
    val sentAtMillis: Long = System.currentTimeMillis()
)

/** Tabla notifications (se llama AppNotification para no chocar con android.app.Notification) */
data class AppNotification(
    val id: String,
    val userId: String,                     // destinatario
    val title: String,
    val body: String,
    val isRead: Boolean = false,
    val relatedPostId: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

/** Tabla badges (catálogo de insignias) */
data class Badge(
    val id: String,
    val name: String,
    val description: String,
    val iconUrl: String,                    // por ahora puede contener un emoji
    val pointsRequired: Int
)

/** Tabla user_badges (insignias ganadas por cada usuario) */
data class UserBadge(
    val id: String,
    val userId: String,
    val badgeId: String,
    val awardedAtMillis: Long = System.currentTimeMillis()
)

/** Tabla reports (referencia polimórfica: targetType + targetId) */
data class Report(
    val id: String,
    val reporterId: String,
    val targetType: ReportTargetType,
    val targetId: String,
    val reason: String,
    val status: ReportStatus = ReportStatus.PENDING,
    val createdAtMillis: Long = System.currentTimeMillis()
)

// ───────────────────────── NO ES TABLA ─────────────────────────

/** Sesión en memoria (fase 2). No existe en el ERD. */
data class UserSession(
    val userId: String,
    val role: UserRole
)