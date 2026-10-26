package com.servilocal.core.data.repository

import com.servilocal.domain.model.*
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.ChatRepository
import com.servilocal.domain.repository.CommentRepository
import com.servilocal.domain.repository.LikeRepository
import com.servilocal.domain.repository.ModerationRepository
import com.servilocal.domain.repository.NotificationRepository
import com.servilocal.domain.repository.PostRepository
import com.servilocal.domain.repository.SessionRepository
import com.servilocal.domain.repository.UserRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Singleton
class MockDataRepository @Inject constructor(
    private val userRepository: UserRepository?,
    private val sessionRepository: SessionRepository?
) :
    AuthRepository,
    PostRepository,
    CommentRepository,
    LikeRepository,
    ChatRepository,
    NotificationRepository,
    ModerationRepository {

    /** Constructor para pruebas y previews: sin sesión real asociada. */
    constructor() : this(null, null)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ───────────────────────── TABLAS DE ENTIDADES ERD (PERSISTENCIA) ─────────────────────────

    private val erdPosts = MutableStateFlow<List<Post>>(
        listOf(
            // Carlos Mendoza (userId: 2L) - Servicios eléctricos y plomería
            Post("pub_1", 2L, title = "Instalación Eléctrica Residencial y Reparaciones", category = PostCategory.HOGAR, description = "Ingeniero Eléctrico con certificación técnica oficial.", minPrice = 25000, maxPrice = 45000, status = PostStatus.APPROVED),
            Post("pub_2", 2L, title = "Plomería y Destapes de Urgencia 24/7", category = PostCategory.HOGAR, description = "Atención inmediata para fugas de agua y destapes.", minPrice = 25000, maxPrice = 50000, status = PostStatus.APPROVED),
            // Valentina Morales (userId: 1L) - Clases particulares
            Post("pub_3", 1L, title = "Clases Particulares de Matemáticas y Física", category = PostCategory.EDUCACION, description = "Docente universitaria de matemáticas puras.", minPrice = 15000, maxPrice = 30000, status = PostStatus.APPROVED),
            // Matías Sepúlveda (userId: 4L) - Servicios de mascotas
            Post("pub_4", 4L, title = "Paseo y Cuidado Canino Personalizado", category = PostCategory.MASCOTAS, description = "Paseos recreativos individuales o en grupos reducidos.", minPrice = 8000, maxPrice = 18000, status = PostStatus.APPROVED),
            // Rodrigo Tapia (userId: 5L) - Cerrajería
            Post("pub_5", 5L, title = "Cerrajería Móvil Urgencias 24/7 y Apertura de Puertas", category = PostCategory.HOGAR, description = "Servicio de cerrajería rápido a domicilio.", minPrice = 30000, maxPrice = 60000, status = PostStatus.PENDING_VERIFICATION),
            // Fernando Lucas (userId: 6L) - Electricidad
            Post("pub_6", 6L, title = "Mantenimiento e Instalación de Tableros Eléctricos", category = PostCategory.TECNOLOGIA, description = "Levantamiento de planos y detección de fugas.", minPrice = 35000, maxPrice = 85000, status = PostStatus.PENDING_VERIFICATION)
        )
    )

    private val erdComments = MutableStateFlow<List<Comment>>(emptyList())
    private val erdLikes = MutableStateFlow<List<Like>>(emptyList())
    private val erdRequests = MutableStateFlow<List<ServiceRequest>>(emptyList())
    private val erdMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val erdNotifications = MutableStateFlow<List<AppNotification>>(emptyList())
    private val erdReports = MutableStateFlow<List<Report>>(emptyList())

    override val comments: StateFlow<List<Comment>> = erdComments.asStateFlow()
    override val likes: StateFlow<List<Like>> = erdLikes.asStateFlow()

    // ───────────────────────── FLUJOS DE ESTADO REPACTADOS PARA LA UI ─────────────────────────

    private val _currentUser = MutableStateFlow(defaultCurrentUser())
    override val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    private val _moderatorUser = MutableStateFlow(defaultModeratorUser())
    override val moderatorUser: StateFlow<UserProfile> = _moderatorUser.asStateFlow()

    private val _allConversations = MutableStateFlow<List<ChatConversation>>(getInitialChats())
    private val _conversations = MutableStateFlow<List<ChatConversation>>(emptyList())
    override val conversations: StateFlow<List<ChatConversation>> = _conversations.asStateFlow()

    private fun refreshConversations() {
        val currentUserId = _currentUser.value.id
        val mapped = _allConversations.value.mapNotNull { chat ->
            if (chat.clientId == currentUserId || chat.providerId == currentUserId) {
                val isClient = chat.clientId == currentUserId
                val recipient = if (isClient) chat.providerName else chat.clientName
                val currentUserName = _currentUser.value.name
                val normalizedMessages = chat.messages.map { message ->
                    when (message.senderName) {
                        chat.clientName -> message.copy(isFromMe = chat.clientId == currentUserId)
                        chat.providerName -> message.copy(isFromMe = chat.providerId == currentUserId)
                        currentUserName -> message.copy(isFromMe = true)
                        else -> message
                    }
                }
                chat.copy(
                    isClientRole = isClient,
                    recipientName = recipient,
                    messages = normalizedMessages
                )
            } else {
                null
            }
        }
        _conversations.value = mapped
    }

    private val _posts = MutableStateFlow<List<ServicePost>>(getInitialPosts())
    override val posts: StateFlow<List<ServicePost>> = _posts.asStateFlow()

    private val _notifications = MutableStateFlow<List<AppNotification>>(getInitialNotifications())
    override val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _reports = MutableStateFlow<List<ModerationReportItem>>(getInitialReports())
    override val reports: StateFlow<List<ModerationReportItem>> = _reports.asStateFlow()

    init {
        val users = userRepository
        val sessions = sessionRepository
        if (users != null && sessions != null) {
            scope.launch {
                combine(sessions.session, users.users) { session, allUsers ->
                    session to allUsers
                }.collect { (session, allUsers) ->
                    if (session == null) {
                        _currentUser.value = defaultCurrentUser()
                        _moderatorUser.value = defaultModeratorUser()
                    } else {
                        allUsers.firstOrNull { it.id == session.userId }?.let { user ->
                            val profile = user.toUserProfile()
                            _currentUser.value = profile
                            if (user.role == UserRole.MODERATOR) {
                                _moderatorUser.value = profile
                            }
                        }
                    }
                    refreshConversations()
                    refreshLikeState()
                }
            }
        }
        refreshConversations()
        refreshLikeState()
    }

    // ───────────────────────── MÉTODOS DE REPOSITORIO ─────────────────────────

    private fun refreshLikeState() {
        val userId = _currentUser.value.id
        _posts.update { list ->
            list.map { post ->
                post.copy(
                    userUpvoted = erdLikes.value.any { it.postId == post.id && it.userId == userId }
                )
            }
        }
    }

    override fun toggleLike(postId: String, userId: Long): Boolean {
        if (_posts.value.none { it.id == postId }) return false

        val existing = erdLikes.value.firstOrNull { it.postId == postId && it.userId == userId }
        if (existing == null) {
            erdLikes.update { current ->
                current + Like(
                    id = "like_${postId}_$userId",
                    userId = userId,
                    postId = postId
                )
            }
        } else {
            erdLikes.update { current -> current.filterNot { it.id == existing.id } }
        }

        val liked = existing == null
        _posts.update { list ->
            list.map { post ->
                if (post.id == postId) {
                    post.copy(
                        userUpvoted = liked,
                        upvotesCount = if (liked) post.upvotesCount + 1 else (post.upvotesCount - 1).coerceAtLeast(0)
                    )
                } else post
            }
        }
        return liked
    }

    override fun hasLiked(postId: String, userId: Long): Boolean =
        erdLikes.value.any { it.postId == postId && it.userId == userId }

    override fun addComment(postId: String, userId: Long, authorName: String, content: String): Boolean {
        if (content.isBlank()) return false
        val post = _posts.value.firstOrNull { it.id == postId } ?: return false

        val newComment = Comment(
            id = "c_${UUID.randomUUID()}",
            postId = postId,
            userId = userId,
            content = content
        )
        erdComments.update { listOf(newComment) + it }

        val uiComment = ServiceComment(
            id = newComment.id,
            authorName = authorName,
            date = "Justo ahora",
            text = content,
            isAuthorReply = post.providerId == userId,
            isRecommended = true,
            authorId = userId
        )
        _posts.update { list ->
            list.map {
                if (it.id == postId) it.copy(comments = listOf(uiComment) + it.comments) else it
            }
        }
        return true
    }

    override fun createPost(
        title: String,
        category: String,
        description: String,
        minPrice: Int,
        maxPrice: Int,
        isPriceToAgree: Boolean,
        location: String
    ): ServicePost {
        require(title.isNotBlank()) { "El título es obligatorio." }
        require(description.isNotBlank()) { "La descripción es obligatoria." }
        require(location.isNotBlank()) { "La ubicación es obligatoria." }
        require(isPriceToAgree || (minPrice > 0 && maxPrice >= minPrice)) { "El rango de precios no es válido." }
        val categoryEnum = parsePostCategory(category)
            ?: throw IllegalArgumentException("Selecciona una categoría válida.")
        val coordinates = coordinatesForLocation(location)

        val newErdPost = Post(
            id = "pub_${UUID.randomUUID()}",
            userId = _currentUser.value.id,
            title = title,
            category = categoryEnum,
            description = description,
            minPrice = minPrice,
            maxPrice = maxPrice,
            status = PostStatus.PENDING_VERIFICATION
        )
        erdPosts.update { listOf(newErdPost) + it }

        val newPost = ServicePost(
            id = newErdPost.id,
            title = title,
            category = category,
            description = description,
            providerName = _currentUser.value.name,
            providerId = _currentUser.value.id,
            providerLevel = _currentUser.value.level.substringAfter(": ", _currentUser.value.level),
            minPrice = minPrice,
            maxPrice = maxPrice,
            isPriceToAgree = isPriceToAgree,
            location = location,
            latitude = coordinates?.first,
            longitude = coordinates?.second,
            distance = formatDistance(coordinates),
            rating = 5.0f,
            reviewCount = 0,
            upvotesCount = 0,
            userUpvoted = false,
            status = PostStatus.PENDING_VERIFICATION,
            responseTime = "< 20 min",
            comments = emptyList()
        )
        _posts.update { listOf(newPost) + it }
        return newPost
    }

    override fun updatePost(
        postId: String,
        title: String,
        category: String,
        description: String,
        minPrice: Int,
        maxPrice: Int,
        isPriceToAgree: Boolean,
        location: String
    ): Boolean {
        val post = _posts.value.firstOrNull { it.id == postId } ?: return false
        if (!canManagePost(post)) return false
        if (title.isBlank() || description.isBlank() || location.isBlank()) return false
        if (!isPriceToAgree && (minPrice <= 0 || maxPrice < minPrice)) return false

        val categoryEnum = parsePostCategory(category) ?: return false
        val coordinates = coordinatesForLocation(location)

        _posts.update { list ->
            list.map {
                if (it.id == postId) {
                    it.copy(
                        title = title,
                        category = category,
                        description = description,
                        minPrice = minPrice,
                        maxPrice = maxPrice,
                        isPriceToAgree = isPriceToAgree,
                        location = location,
                        latitude = coordinates?.first,
                        longitude = coordinates?.second,
                        distance = formatDistance(coordinates)
                    )
                } else it
            }
        }
        erdPosts.update { list ->
            list.map {
                if (it.id == postId) {
                    it.copy(
                        title = title,
                        category = categoryEnum,
                        description = description,
                        minPrice = minPrice,
                        maxPrice = maxPrice
                    )
                } else it
            }
        }
        return true
    }

    override fun deletePost(postId: String): Boolean {
        val post = _posts.value.firstOrNull { it.id == postId } ?: return false
        if (post.providerId != _currentUser.value.id) return false

        _posts.update { posts -> posts.filterNot { it.id == postId } }
        erdPosts.update { posts -> posts.filterNot { it.id == postId } }
        erdComments.update { comments -> comments.filterNot { it.postId == postId } }
        erdLikes.update { likes -> likes.filterNot { it.postId == postId } }
        return true
    }

    override fun finishPost(postId: String): Boolean {
        val post = _posts.value.firstOrNull { it.id == postId } ?: return false
        if (!canManagePost(post)) return false

        _posts.update { list ->
            list.map { if (it.id == postId) it.copy(status = PostStatus.FINISHED) else it }
        }
        erdPosts.update { list ->
            list.map { if (it.id == postId) it.copy(status = PostStatus.FINISHED) else it }
        }
        return true
    }

    private fun canManagePost(post: ServicePost): Boolean =
        post.providerId == _currentUser.value.id && post.status == PostStatus.APPROVED

    private fun parsePostCategory(value: String): PostCategory? = when (
        value.trim().lowercase()
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
    ) {
        "hogar" -> PostCategory.HOGAR
        "educacion" -> PostCategory.EDUCACION
        "mascotas" -> PostCategory.MASCOTAS
        "tecnologia", "electricidad" -> PostCategory.TECNOLOGIA
        "transporte" -> PostCategory.TRANSPORTE
        else -> null
    }

    /** Coordinates are intentionally approximate demo locations around Armenia's city center. */
    private fun coordinatesForLocation(location: String): Pair<Double, Double>? {
        val normalized = location.lowercase()
        return when {
            "granada" in normalized -> 4.5339 to -75.6703
            "centro" in normalized -> 4.5369 to -75.6811
            "norte" in normalized -> 4.5591 to -75.6811
            "parque de la vida" in normalized -> 4.5403 to -75.6811
            "los profesionales" in normalized -> 4.5150 to -75.6811
            "la castellana" in normalized -> 4.5510 to -75.6811
            "armenia" in normalized -> 4.5339 to -75.6811
            else -> null
        }
    }

    private fun formatDistance(coordinates: Pair<Double, Double>?): String {
        if (coordinates == null) return "Distancia no calculada"
        val (latitude, longitude) = coordinates
        val earthRadiusKm = 6_371.0
        val latitudeDelta = Math.toRadians(latitude - 4.5339)
        val longitudeDelta = Math.toRadians(longitude - -75.6811)
        val a = kotlin.math.sin(latitudeDelta / 2).let { it * it } +
            kotlin.math.cos(Math.toRadians(4.5339)) * kotlin.math.cos(Math.toRadians(latitude)) *
            kotlin.math.sin(longitudeDelta / 2).let { it * it }
        val distanceKm = 2 * earthRadiusKm * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return String.format(Locale.ROOT, "A %.1f km", distanceKm)
    }

    override fun approvePost(postId: String) {
        erdPosts.update { list -> list.map { if (it.id == postId) it.copy(status = PostStatus.APPROVED) else it } }
        _posts.update { list -> list.map { if (it.id == postId) it.copy(status = PostStatus.APPROVED) else it } }
    }

    override fun rejectPost(postId: String, reason: String, notes: String?) {
        erdPosts.update { list -> list.map { if (it.id == postId) it.copy(status = PostStatus.REJECTED, rejectionReason = reason) else it } }
        _posts.update { list ->
            list.map { post ->
                if (post.id == postId) {
                    post.copy(status = PostStatus.REJECTED, rejectionReason = reason, rejectionNotes = notes)
                } else post
            }
        }
    }

    override fun moderatePost(
        postId: String,
        status: PostStatus,
        reason: String?,
        notes: String?
    ): Boolean {
        if (_moderatorUser.value.role != UserRole.MODERATOR) return false
        if (status != PostStatus.REJECTED && status != PostStatus.FINISHED) return false
        if (_posts.value.none { it.id == postId }) return false

        val moderatorId = _moderatorUser.value.id
        erdPosts.update { list ->
            list.map { post ->
                if (post.id == postId) {
                    post.copy(
                        status = status,
                        moderatorId = moderatorId,
                        rejectionReason = if (status == PostStatus.REJECTED) reason else post.rejectionReason
                    )
                } else post
            }
        }
        _posts.update { list ->
            list.map { post ->
                if (post.id == postId) {
                    post.copy(
                        status = status,
                        rejectionReason = if (status == PostStatus.REJECTED) reason else post.rejectionReason,
                        rejectionNotes = if (status == PostStatus.REJECTED) notes else post.rejectionNotes
                    )
                } else post
            }
        }
        return true
    }

    override fun sendMessage(conversationId: String, text: String) {
        if (text.isBlank()) return
        val newErdMsg = ChatMessage(
            id = "m_${System.currentTimeMillis()}",
            serviceRequestId = conversationId,
            senderId = _currentUser.value.id,
            message = text
        )
        erdMessages.update { listOf(newErdMsg) + it }

        _allConversations.update { list ->
            list.map { chat ->
                if (chat.id == conversationId) {
                    val newMsg = ChatMessageItem(
                        id = newErdMsg.id,
                        senderName = _currentUser.value.name,
                        text = text,
                        time = "Ahora",
                        isFromMe = true,
                        read = false
                    )
                    chat.copy(lastMessage = text, lastTime = "Ahora", messages = chat.messages + newMsg)
                } else chat
            }
        }
        refreshConversations()
    }

    override fun startConversation(postId: String): String? {
        val post = _posts.value.find { it.id == postId } ?: return null
        val currentUserId = _currentUser.value.id
        val currentUserName = _currentUser.value.name
        val providerId = post.providerId ?: 2L
        val providerName = post.providerName

        // Un usuario no puede iniciar una conversación consigo mismo.
        if (providerId == currentUserId) return null

        val existing = _allConversations.value.firstOrNull {
            it.clientId == currentUserId && it.providerId == providerId && it.postId == postId
        }
        if (existing != null) return existing.id

        val conversationId = "chat_${System.currentTimeMillis()}"
        val newConversation = ChatConversation(
            id = conversationId,
            clientId = currentUserId,
            providerId = providerId,
            postId = postId,
            clientName = currentUserName,
            providerName = providerName,
            recipientName = providerName,
            serviceTitle = post.title,
            servicePrice = if (post.isPriceToAgree) "A convenir" else "$ ${post.minPrice} COP",
            status = ServiceRequestStatus.IN_PROGRESS,
            lastMessage = "Inicia la conversación",
            lastTime = "Ahora",
            unreadCount = 1,
            isClientRole = true
        )
        _allConversations.update { listOf(newConversation) + it }
        refreshConversations()
        return conversationId
    }

    override fun markConversationRead(conversationId: String) {
        _allConversations.update { list -> list.map { if (it.id == conversationId) it.copy(unreadCount = 0) else it } }
        refreshConversations()
    }

    override fun updateConversationStatus(conversationId: String, status: ChatStatus) {
        val statusMessage = when (status) {
            ServiceRequestStatus.IN_PROGRESS -> "Solicitud aceptada. El servicio está en proceso."
            ServiceRequestStatus.PENDING -> "Solicitud pendiente de respuesta."
            ServiceRequestStatus.COMPLETED -> "Servicio finalizado."
            ServiceRequestStatus.NEW_REQUEST -> "Nueva solicitud recibida."
            ServiceRequestStatus.DECLINED -> "Solicitud rechazada."
        }
        _allConversations.update { list ->
            list.map { conversation ->
                if (conversation.id != conversationId) conversation
                else conversation.copy(
                    status = status,
                    lastMessage = statusMessage,
                    lastTime = "Ahora",
                    unreadCount = 0,
                    messages = conversation.messages + ChatMessageItem(
                        id = "status_${System.currentTimeMillis()}",
                        senderName = "Sistema",
                        text = statusMessage,
                        time = "Ahora",
                        isFromMe = false
                    )
                )
            }
        }
        refreshConversations()
    }

    override fun updateCurrentUser(name: String, email: String) {
        if (name.isBlank() || !email.contains('@')) return
        _currentUser.update { it.copy(name = name.trim(), email = email.trim()) }

        val updated = _currentUser.value
        val users = userRepository
        if (users != null) {
            scope.launch {
                users.findById(updated.id)?.let { user ->
                    users.save(user.copy(name = updated.name, email = updated.email))
                }
            }
        }
        refreshConversations()
    }

    override fun createReport(
        reason: String,
        description: String,
        targetTitle: String,
        targetPostId: String
    ) {
        val currentUser = _currentUser.value
        val newErdReport = Report(
            id = "rep_${System.currentTimeMillis()}",
            reporterId = currentUser.id,
            targetType = ReportTargetType.POST,
            targetId = targetPostId,
            reason = reason,
            description = description,
            status = ReportStatus.PENDING
        )
        erdReports.update { listOf(newErdReport) + it }
        _reports.update { list ->
            listOf(
                ModerationReportItem(
                    id = newErdReport.id,
                    reasonTitle = reason,
                    targetType = "Publicación",
                    targetTitle = targetTitle,
                    description = description,
                    targetPostId = targetPostId,
                    reporterId = currentUser.id,
                    reporterName = currentUser.name
                )
            ) + list
        }
    }

    override fun createServiceRequest(
        postId: String,
        applicantName: String,
        contactMethod: String,
        address: String,
        description: String
    ): String {
        val post = _posts.value.find { it.id == postId }
        val recipient = post?.providerName ?: "Carlos Mendoza"
        val providerId = post?.providerId ?: 2L
        val currentUserId = _currentUser.value.id
        val convId = "chat_${System.currentTimeMillis()}"

        val newErdReq = ServiceRequest(
            id = convId,
            postId = postId,
            clientId = currentUserId,
            contactPhone = contactMethod,
            exactAddress = address,
            problemDescription = description,
            status = ServiceRequestStatus.PENDING
        )
        erdRequests.update { listOf(newErdReq) + it }

        val newConv = ChatConversation(
            id = convId,
            clientId = currentUserId,
            providerId = providerId,
            postId = postId,
            clientName = applicantName,
            providerName = recipient,
            recipientName = recipient,
            serviceTitle = post?.title ?: "Servicio Solicitado",
            servicePrice = if (post?.isPriceToAgree == true) "A convenir" else "$ ${post?.minPrice ?: 25000} COP",
            status = ServiceRequestStatus.PENDING,
            lastMessage = description,
            lastTime = "Ahora",
            unreadCount = 1,
            isClientRole = true,
            requestAddress = address,
            requestProblem = description,
            requestPhone = contactMethod,
            requesterName = applicantName,
            messages = listOf(
                ChatMessageItem(
                    id = "msg_sys_1",
                    senderName = "Sistema",
                    text = "Has enviado una solicitud formal de servicio.",
                    time = "Ahora",
                    isFromMe = true
                )
            )
        )
        _allConversations.update { listOf(newConv) + it }
        refreshConversations()
        return convId
    }

    override fun resolveReport(
        reportId: String,
        resolution: ReportResolution,
        moderatorComment: String
    ): Boolean {
        if (_moderatorUser.value.role != UserRole.MODERATOR) return false
        val report = _reports.value.firstOrNull { it.id == reportId } ?: return false
        if (report.status != ReportStatus.PENDING) return false

        val moderator = _moderatorUser.value
        val resolvedAt = System.currentTimeMillis()
        erdReports.update { list ->
            list.map { raw ->
                if (raw.id == reportId) {
                    raw.copy(
                        status = ReportStatus.REVIEWED,
                        moderatorId = moderator.id,
                        resolution = resolution,
                        moderatorComment = moderatorComment,
                        resolvedAtMillis = resolvedAt
                    )
                } else raw
            }
        }
        _reports.update { list -> list.filterNot { it.id == reportId } }
        return true
    }

    override fun resolveReportsForPost(postId: String) {
        _reports.update { list -> list.filterNot { it.targetPostId == postId } }
    }

    override fun create(notification: AppNotification) {
        _notifications.update { current -> current + notification }
    }

    override suspend fun markRead(notificationId: String, userId: Long, isRead: Boolean): Boolean {
        var updated = false
        _notifications.update { current ->
            current.map { notification ->
                if (notification.id == notificationId && notification.userId == userId) {
                    updated = true
                    notification.copy(isRead = isRead)
                } else {
                    notification
                }
            }
        }
        return updated
    }

    override suspend fun markAllRead(userId: Long) {
        _notifications.update { current ->
            current.map { notification ->
                if (notification.userId == userId) notification.copy(isRead = true) else notification
            }
        }
    }

    override fun toggleModeratorShift() {
        _moderatorUser.update { it.copy(activeShift = !it.activeShift) }
    }

    // ───────────────────────── DATOS INICIALES MOCK ─────────────────────────

    private fun getInitialPosts(): List<ServicePost> = listOf(
        ServicePost(
            id = "pub_1",
            title = "Instalación Eléctrica Residencial y Reparaciones",
            category = "Hogar",
            description = "Ingeniero Eléctrico con certificación técnica oficial. Realizo diagnóstico, instalación y reparación de cableados domiciliarios.",
            providerName = "Carlos Mendoza",
            providerId = 2L,
            providerLevel = "Profesional",
            minPrice = 25000,
            maxPrice = 45000,
            location = "Armenia, Quindío - Barrio Granada",
            latitude = 4.5339,
            longitude = -75.6703,
            distance = "A 1.2 km - Granada",
            rating = 4.9f,
            reviewCount = 38,
            upvotesCount = 148,
            status = PostStatus.APPROVED,
            warrantyMonths = 6,
            responseTime = "< 30 min",
            createdAtMillis = daysAgo(0),
            comments = listOf(
                ServiceComment("c_1", "Francisca Valenzuela", "Ayer a las 18:40", "Excelente servicio. Don Carlos detectó la fuga de inmediato.", isAuthorReply = false, isRecommended = true),
                ServiceComment("c_2", "Matías Sepúlveda", "Hace 3 días", "Hola Carlos, ¿haces instalación de Wallbox?", isAuthorReply = false, isRecommended = false),
                ServiceComment("c_3", "Carlos Mendoza (Autor)", "Hace 3 días", "¡Hola Matías! Sí, cuento con certificación.", isAuthorReply = true, isRecommended = true)
            )
        ),
        // Carlos Mendoza (userId: 2L) - Plomería
        ServicePost(
            id = "pub_2",
            title = "Plomería y Destapes de Urgencia 24/7",
            category = "Hogar",
            description = "Atención inmediata para fugas de agua, cambio de griferías y destapes.",
            providerName = "Carlos Mendoza",
            providerId = 2L,
            providerLevel = "Profesional",
            minPrice = 25000,
            maxPrice = 50000,
            location = "Armenia, Quindío - Centro",
            latitude = 4.5369,
            longitude = -75.6811,
            distance = "A 0.3 km - Centro",
            rating = 4.9f,
            reviewCount = 42,
            upvotesCount = 114,
            status = PostStatus.APPROVED,
            warrantyMonths = 1,
            responseTime = "< 20 min",
            createdAtMillis = daysAgo(2)
        ),
        // Valentina Morales (userId: 1L) - Clases particulares
        ServicePost(
            id = "pub_3",
            title = "Clases Particulares de Matemáticas y Física",
            category = "Educación",
            description = "Docente universitaria de matemáticas puras. Apoyo escolar y universitario.",
            providerName = "Valentina Morales",
            providerId = 1L,
            providerLevel = "Profesional",
            minPrice = 15000,
            maxPrice = 30000,
            location = "Armenia, Quindío - Norte",
            latitude = 4.5591,
            longitude = -75.6811,
            distance = "A 2.8 km - Norte",
            rating = 5.0f,
            reviewCount = 52,
            upvotesCount = 88,
            status = PostStatus.APPROVED,
            warrantyMonths = 0,
            responseTime = "< 15 min",
            createdAtMillis = daysAgo(9)
        ),
        // Matías Sepúlveda (userId: 4L) - Paseo de mascotas
        ServicePost(
            id = "pub_4",
            title = "Paseo y Cuidado Canino Personalizado",
            category = "Mascotas",
            description = "Paseos recreativos individuales o en grupos reducidos con seguimiento GPS.",
            providerName = "Matías Sepúlveda",
            providerId = 4L,
            providerLevel = "Principiante",
            minPrice = 8000,
            maxPrice = 18000,
            location = "Armenia, Quindío - Parque de la Vida",
            latitude = 4.5403,
            longitude = -75.6811,
            distance = "A 0.7 km",
            rating = 4.8f,
            reviewCount = 19,
            upvotesCount = 49,
            status = PostStatus.APPROVED,
            warrantyMonths = 0,
            responseTime = "< 1 hora",
            createdAtMillis = daysAgo(20)
        ),
        // Rodrigo Tapia (userId: 5L) - Cerrajería
        ServicePost(
            id = "pub_5",
            title = "Cerrajería Móvil Urgencias 24/7 y Apertura de Puertas",
            category = "Hogar",
            description = "Servicio de cerrajería rápido a domicilio para casas y autos.",
            providerName = "Rodrigo Tapia",
            providerId = 5L,
            providerLevel = "Principiante",
            minPrice = 30000,
            maxPrice = 60000,
            location = "Armenia, Quindío - Los Profesionales",
            latitude = 4.5150,
            longitude = -75.6811,
            distance = "A 2.1 km",
            rating = 4.6f,
            reviewCount = 8,
            upvotesCount = 0,
            status = PostStatus.PENDING_VERIFICATION,
            warrantyMonths = 3,
            responseTime = "< 25 min",
            createdAtMillis = daysAgo(5)
        ),
        // Fernando Lucas (userId: 6L) - Electricidad
        ServicePost(
            id = "pub_6",
            title = "Mantenimiento e Instalación de Tableros Eléctricos",
            category = "Electricidad",
            description = "Levantamiento de planos, detección de fugas y reemplazo de protecciones.",
            providerName = "Fernando Lucas",
            providerId = 6L,
            providerLevel = "Principiante",
            minPrice = 35000,
            maxPrice = 85000,
            location = "Armenia, Quindío - La Castellana",
            latitude = 4.5510,
            longitude = -75.6811,
            distance = "A 1.9 km",
            rating = 5.0f,
            reviewCount = 2,
            upvotesCount = 0,
            status = PostStatus.PENDING_VERIFICATION,
            warrantyMonths = 6,
            responseTime = "< 45 min",
            createdAtMillis = daysAgo(1)
        )
    )

    private fun getInitialChats(): List<ChatConversation> = listOf(
        // Chat entre Valentina (cliente, userId: 1L) y Carlos Mendoza (proveedor, userId: 2L)
        ChatConversation(
            id = "chat_1",
            clientId = 1L,
            providerId = 2L,
            postId = "pub_1",
            clientName = "Valentina Morales",
            providerName = "Carlos Mendoza",
            recipientName = "Carlos Mendoza",
            serviceTitle = "Instalación Eléctrica Residencial",
            servicePrice = "$ 45.000 COP",
            status = ServiceRequestStatus.IN_PROGRESS,
            lastMessage = "Perfecto, llevo el multímetro y los repuestos a las 3:00 PM.",
            lastTime = "10:20 AM",
            unreadCount = 2,
            isClientRole = true,
            requestAddress = "Calle 14 # 23-45, Barrio Granada, Armenia, Quindío",
            requestProblem = "El interruptor principal del circuito de la cocina genera chispas al encender el horno eléctrico. Requiere revisión urgente.",
            requestPhone = "+57 312 456 7890",
            requesterName = "Valentina Morales",
            messages = listOf(
                ChatMessageItem("m1", "Carlos Mendoza", "Hola Valentina, gusto en saludarte. Ya revisé la solicitud.", "10:14 AM", isFromMe = false),
                ChatMessageItem("m2", "Valentina Morales", "¡Hola Carlos! Muchas gracias.", "10:16 AM", isFromMe = true, read = true),
                ChatMessageItem("m3", "Carlos Mendoza", "Perfecto, llevo el multímetro a las 3:00 PM.", "10:20 AM", isFromMe = false)
            )
        ),
        // Chat de clases (usando las publicaciones de Valentina como proveedor)
        ChatConversation(
            id = "chat_2",
            clientId = 1L,
            providerId = 2L,
            postId = "pub_3",
            clientName = "Valentina Morales",
            providerName = "Carlos Mendoza",
            recipientName = "Carlos Mendoza",
            serviceTitle = "Clases de Matemáticas",
            servicePrice = "$ 15.000 COP / hr",
            status = ServiceRequestStatus.PENDING,
            lastMessage = "Te adjunto el temario del examen de cálculo para revisarlo.",
            lastTime = "Hace 2 h",
            unreadCount = 0,
            isClientRole = true,
            messages = listOf(
                ChatMessageItem("m4", "Valentina Morales", "Te adjunto el temario del examen de cálculo para revisarlo.", "Hace 2 h", isFromMe = false)
            )
        ),
        // Chat entre Andrés Gómez (userId: 8L) y Carlos Mendoza (userId: 2L) - plomería
        ChatConversation(
            id = "chat_3",
            clientId = 8L,
            providerId = 2L,
            postId = "pub_2",
            clientName = "Andrés Gómez",
            providerName = "Carlos Mendoza",
            recipientName = "Carlos Mendoza",
            serviceTitle = "Plomería y Destapes 24/7",
            servicePrice = "$ 30.000 COP",
            status = ServiceRequestStatus.COMPLETED,
            lastMessage = "Servicio completado exitosamente. ¡Muchas gracias por calificar!",
            lastTime = "Ayer",
            unreadCount = 0,
            isClientRole = true,
            messages = listOf(
                ChatMessageItem("m5", "Andrés Gómez", "Servicio completado exitosamente. ¡Muchas gracias por calificar!", "Ayer", isFromMe = false)
            )
        ),
        // Francisca Valenzuela (userId: 7L) contacting a provider
        ChatConversation(
            id = "chat_4",
            clientId = 7L,
            providerId = 2L,
            postId = "pub_1",
            clientName = "Francisca Valenzuela",
            providerName = "Carlos Mendoza",
            recipientName = "Carlos Mendoza",
            serviceTitle = "Instalación Eléctrica Residencial",
            servicePrice = "$ 40.000 COP",
            status = ServiceRequestStatus.NEW_REQUEST,
            lastMessage = "¡Hola! ¿Tienes disponibilidad este viernes para revisar una cerradura en Calle Mayor?",
            lastTime = "10:45 AM",
            unreadCount = 1,
            isClientRole = false,
            messages = listOf(
                ChatMessageItem("m6", "Francisca Valenzuela", "¡Hola! ¿Tienes disponibilidad este viernes para revisar una cerradura?", "10:45 AM", isFromMe = false)
            )
        ),
        // Jorge Palacios (userId: 10L) contacting a provider
        ChatConversation(
            id = "chat_5",
            clientId = 10L,
            providerId = 2L,
            postId = "pub_2",
            clientName = "Jorge Palacios",
            providerName = "Carlos Mendoza",
            recipientName = "Carlos Mendoza",
            serviceTitle = "Pintura y Remodelaciones Integrales",
            servicePrice = "$ 65.000 COP",
            status = ServiceRequestStatus.IN_PROGRESS,
            lastMessage = "Me interesa cotizar la pintura para dos habitaciones. Te mando medidas.",
            lastTime = "Ayer",
            unreadCount = 0,
            isClientRole = false,
            messages = listOf(
                ChatMessageItem("m7", "Jorge Palacios", "Me interesa cotizar la pintura para dos habitaciones. Te mando medidas.", "Ayer", isFromMe = false)
            )
        )
    )

    private fun getInitialNotifications(): List<AppNotification> {
        val now = System.currentTimeMillis()
        val minute = 60_000L
        return listOf(
            // Notificación para Valentina Morales (userId: 1L)
            AppNotification(
                id = "notif_1",
                userId = 1L,
                title = "Nueva solicitud de servicio recibida",
                body = "Francisca Valenzuela ha solicitado tu servicio.",
                type = NotificationType.REQUEST,
                relatedChatId = "chat_4",
                createdAtMillis = now - 10 * minute
            ),
            AppNotification(
                id = "notif_2",
                userId = 1L,
                title = "¡Publicación aprobada!",
                body = "La publicación 'Instalación Eléctrica' está disponible en el Feed.",
                type = NotificationType.SYSTEM,
                relatedPostId = "pub_1",
                createdAtMillis = now - 120 * minute
            ),
            AppNotification(
                id = "notif_3",
                userId = 1L,
                title = "Nuevo comentario en tu anuncio",
                body = "Matías Sepúlveda preguntó por instalación de cargadores.",
                type = NotificationType.COMMENT,
                relatedPostId = "pub_1",
                createdAtMillis = now - 240 * minute
            ),
            AppNotification(
                id = "notif_4",
                userId = 1L,
                title = "¡Nuevo logro desbloqueado: Influencer!",
                body = "Alcanzaste 100 puntos de reputación.",
                type = NotificationType.ACHIEVEMENT,
                isRead = true,
                createdAtMillis = now - 24 * 60 * minute
            ),
            AppNotification(
                id = "notif_5",
                userId = 1L,
                title = "A 5 vecinos les interesó tu servicio",
                body = "Tu publicación tuvo alta interacción.",
                type = NotificationType.INTERACTION,
                isRead = true,
                createdAtMillis = now - 3 * 24 * 60 * minute
            )
        )
    }

    private fun getInitialReports(): List<ModerationReportItem> = listOf(
        ModerationReportItem(
            id = "rep_1",
            reasonTitle = "Solicitud de pago externo",
            targetTitle = "Instalación Eléctrica Residencial y Reparaciones",
            description = "Pide transferencia previa por WhatsApp antes de agendar la visita.",
            targetPostId = "pub_1",
            reporterName = "Camila Restrepo",
            createdAtMillis = daysAgo(0)
        ),
        ModerationReportItem(
            id = "rep_2",
            reasonTitle = "Datos de contacto en descripción",
            targetTitle = "Cerrajería Móvil Urgencias 24/7 y Apertura de Puertas",
            description = "Incluye un número privado en la descripción del servicio.",
            targetPostId = "pub_5",
            reporterName = "Andrés Gómez",
            createdAtMillis = daysAgo(9)
        ),
        ModerationReportItem(
            id = "rep_3",
            reasonTitle = "Servicio engañoso o no autorizado",
            targetTitle = "Plomería y Destapes de Urgencia 24/7",
            description = "El proveedor no cumple con la visita agendada ni responde los mensajes.",
            targetPostId = "pub_2",
            reporterName = "Laura Díaz",
            createdAtMillis = daysAgo(20)
        )
    )

    private fun daysAgo(days: Int): Long =
        java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, -days)
        }.timeInMillis

    private fun defaultCurrentUser(): UserProfile = UserProfile(
        id = 1L,
        name = "Valentina Morales",
        email = "valentina@gmail.com",
        role = UserRole.USER,
        level = "Nivel 2: Profesional",
        points = 220,
        nextLevelPoints = 250,
        workingHours = "Lunes a sábado · 8:00 a. m. - 6:00 p. m."
    )

    private fun defaultModeratorUser(): UserProfile = UserProfile(
        id = 3L,
        name = "Alejandro Vargas",
        email = "alejandro@gmail.com",
        role = UserRole.MODERATOR,
        level = "Nivel 4: Maestro",
        points = 500,
        nextLevelPoints = 500,
        staffId = "MOD-4892",
        activeShift = true,
        workingHours = "Lunes a viernes · 8:00 a. m. - 4:30 p. m."
    )

    /** Convierte la entidad de sesión en el modelo que consumen las pantallas. */
    private fun User.toUserProfile(): UserProfile {
        val isModerator = role == UserRole.MODERATOR
        return UserProfile(
            id = id,
            name = name,
            email = email,
            role = role,
            level = level.displayName(),
            points = points,
            nextLevelPoints = level.next?.minPoints ?: level.minPoints,
            staffId = if (isModerator) "MOD-4892" else null,
            activeShift = isModerator,
            workingHours = if (isModerator) {
                "Lunes a viernes · 8:00 a. m. - 4:30 p. m."
            } else {
                "Lunes a sábado · 8:00 a. m. - 6:00 p. m."
            }
        )
    }
}
