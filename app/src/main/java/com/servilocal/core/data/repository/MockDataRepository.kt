package com.servilocal.core.data.repository

import com.servilocal.domain.model.*
import com.servilocal.domain.repository.AuthRepository
import com.servilocal.domain.repository.ChatRepository
import com.servilocal.domain.repository.ModerationRepository
import com.servilocal.domain.repository.NotificationRepository
import com.servilocal.domain.repository.PostRepository
import com.servilocal.domain.repository.SessionRepository
import com.servilocal.domain.repository.UserRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Singleton
class MockDataRepository @Inject constructor(
    private val userRepository: UserRepository?,
    private val sessionRepository: SessionRepository?
) :
    AuthRepository,
    PostRepository,
    ChatRepository,
    NotificationRepository,
    ModerationRepository {

    /** Constructor para pruebas y previews: sin sesión real asociada. */
    constructor() : this(null, null)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ───────────────────────── TABLAS DE ENTIDADES ERD (PERSISTENCIA) ─────────────────────────

    private val erdPosts = MutableStateFlow<List<Post>>(
        listOf(
            Post("pub_1", "user_val_1", title = "Instalación Eléctrica Residencial y Reparaciones", category = PostCategory.HOGAR, description = "Ingeniero Eléctrico con certificación técnica oficial.", minPrice = 25000, maxPrice = 45000, status = PostStatus.APPROVED),
            Post("pub_2", "user_val_1", title = "Plomería y Destapes de Urgencia 24/7", category = PostCategory.HOGAR, description = "Atención inmediata para fugas de agua y destapes.", minPrice = 25000, maxPrice = 50000, status = PostStatus.APPROVED),
            Post("pub_3", "user_val_1", title = "Clases Particulares de Matemáticas y Física", category = PostCategory.EDUCACION, description = "Docente universitaria de matemáticas puras.", minPrice = 15000, maxPrice = 30000, status = PostStatus.APPROVED),
            Post("pub_4", "user_val_1", title = "Paseo y Cuidado Canino Personalizado", category = PostCategory.MASCOTAS, description = "Paseos recreativos individuales o en grupos reducidos.", minPrice = 8000, maxPrice = 18000, status = PostStatus.APPROVED),
            Post("pub_5", "user_val_1", title = "Cerrajería Móvil Urgencias 24/7 y Apertura de Puertas", category = PostCategory.HOGAR, description = "Servicio de cerrajería rápido a domicilio.", minPrice = 30000, maxPrice = 60000, status = PostStatus.PENDING_VERIFICATION),
            Post("pub_6", "user_val_1", title = "Mantenimiento e Instalación de Tableros Eléctricos", category = PostCategory.TECNOLOGIA, description = "Levantamiento de planos y detección de fugas.", minPrice = 35000, maxPrice = 85000, status = PostStatus.PENDING_VERIFICATION)
        )
    )

    private val erdRequests = MutableStateFlow<List<ServiceRequest>>(emptyList())
    private val erdMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val erdNotifications = MutableStateFlow<List<AppNotification>>(emptyList())
    private val erdReports = MutableStateFlow<List<Report>>(emptyList())

    // ───────────────────────── FLUJOS DE ESTADO REPACTADOS PARA LA UI ─────────────────────────

    private val _currentUser = MutableStateFlow(defaultCurrentUser())
    override val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    private val _moderatorUser = MutableStateFlow(defaultModeratorUser())
    override val moderatorUser: StateFlow<UserProfile> = _moderatorUser.asStateFlow()

    init {
        val users = userRepository
        val sessions = sessionRepository
        if (users != null && sessions != null) {
            scope.launch {
                sessions.session.collect { session ->
                    if (session == null) {
                        _currentUser.value = defaultCurrentUser()
                        _moderatorUser.value = defaultModeratorUser()
                    } else {
                        users.findById(session.userId)?.let { user ->
                            val profile = user.toUserProfile()
                            _currentUser.value = profile
                            if (user.role == UserRole.MODERATOR) {
                                _moderatorUser.value = profile
                            }
                        }
                    }
                }
            }
        }
    }

    private val _posts = MutableStateFlow<List<ServicePost>>(getInitialPosts())
    override val posts: StateFlow<List<ServicePost>> = _posts.asStateFlow()

    private val _conversations = MutableStateFlow<List<ChatConversation>>(getInitialChats())
    override val conversations: StateFlow<List<ChatConversation>> = _conversations.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationItem>>(getInitialNotifications())
    override val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _reports = MutableStateFlow<List<ModerationReportItem>>(getInitialReports())
    override val reports: StateFlow<List<ModerationReportItem>> = _reports.asStateFlow()

    // ───────────────────────── MÉTODOS DE REPOSITORIO ─────────────────────────

    override fun toggleUpvote(postId: String) {
        _posts.update { list ->
            list.map { post ->
                if (post.id == postId) {
                    val newUpvoted = !post.userUpvoted
                    val newCount = if (newUpvoted) post.upvotesCount + 1 else (post.upvotesCount - 1).coerceAtLeast(0)
                    post.copy(userUpvoted = newUpvoted, upvotesCount = newCount)
                } else post
            }
        }
    }

    override fun addComment(postId: String, text: String, authorName: String, isAuthorReply: Boolean) {
        if (text.isBlank()) return
        _posts.update { list ->
            list.map { post ->
                if (post.id == postId) {
                    val newComment = ServiceComment(
                        id = "c_${System.currentTimeMillis()}",
                        authorName = authorName,
                        date = "Justo ahora",
                        text = text,
                        isAuthorReply = isAuthorReply,
                        isRecommended = true
                    )
                    post.copy(comments = listOf(newComment) + post.comments)
                } else post
            }
        }
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

        val newErdPost = Post(
            id = "pub_${System.currentTimeMillis()}",
            userId = _currentUser.value.id,
            title = title,
            category = try { PostCategory.valueOf(category.uppercase()) } catch (_: Exception) { PostCategory.HOGAR },
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
            providerLevel = "Profesional",
            minPrice = minPrice,
            maxPrice = maxPrice,
            isPriceToAgree = isPriceToAgree,
            location = location,
            distance = "A 0.5 km - Armenia",
            rating = 5.0f,
            reviewCount = 0,
            upvotesCount = 1,
            userUpvoted = true,
            status = PostStatus.PENDING_VERIFICATION,
            responseTime = "< 20 min",
            comments = emptyList()
        )
        _posts.update { listOf(newPost) + it }
        return newPost
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

    override fun sendMessage(conversationId: String, text: String) {
        if (text.isBlank()) return
        val newErdMsg = ChatMessage(
            id = "m_${System.currentTimeMillis()}",
            serviceRequestId = conversationId,
            senderId = _currentUser.value.id,
            message = text
        )
        erdMessages.update { listOf(newErdMsg) + it }

        _conversations.update { list ->
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
    }

    override fun startConversation(postId: String): String? {
        val post = _posts.value.find { it.id == postId } ?: return null
        val existing = _conversations.value.firstOrNull {
            it.isClientRole && it.serviceTitle == post.title && it.recipientName == post.providerName
        }
        if (existing != null) return existing.id

        val conversationId = "chat_${System.currentTimeMillis()}"
        val newConversation = ChatConversation(
            id = conversationId,
            recipientName = post.providerName,
            serviceTitle = post.title,
            servicePrice = if (post.isPriceToAgree) "A convenir" else "$ ${post.minPrice} COP",
            status = ServiceRequestStatus.IN_PROGRESS,
            lastMessage = "Inicia la conversación",
            lastTime = "Ahora",
            isClientRole = true
        )
        _conversations.update { listOf(newConversation) + it }
        return conversationId
    }

    override fun markConversationRead(conversationId: String) {
        _conversations.update { list -> list.map { if (it.id == conversationId) it.copy(unreadCount = 0) else it } }
    }

    override fun updateConversationStatus(conversationId: String, status: ChatStatus) {
        val statusMessage = when (status) {
            ServiceRequestStatus.IN_PROGRESS -> "Solicitud aceptada. El servicio está en proceso."
            ServiceRequestStatus.PENDING -> "Solicitud pendiente de respuesta."
            ServiceRequestStatus.COMPLETED -> "Servicio finalizado."
            ServiceRequestStatus.NEW_REQUEST -> "Nueva solicitud recibida."
            ServiceRequestStatus.DECLINED -> "Solicitud rechazada."
        }
        _conversations.update { list ->
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
    }

    override fun createReport(
        reason: String,
        description: String,
        targetType: String,
        targetTitle: String,
        targetPostId: String?,
        targetChatId: String?
    ) {
        val newErdReport = Report(
            id = "rep_${System.currentTimeMillis()}",
            reporterId = _currentUser.value.id,
            targetType = if (targetPostId != null) ReportTargetType.POST else ReportTargetType.CHAT_MESSAGE,
            targetId = targetPostId ?: targetChatId ?: "unknown",
            reason = reason,
            status = ReportStatus.PENDING
        )
        erdReports.update { listOf(newErdReport) + it }

        val existing = _reports.value.firstOrNull {
            it.targetPostId == targetPostId && it.targetChatId == targetChatId && it.reasonTitle == reason
        }
        if (existing != null) {
            _reports.update { list -> list.map { if (it.id == existing.id) it.copy(reportCount = it.reportCount + 1) else it } }
        } else {
            _reports.update { list ->
                listOf(
                    ModerationReportItem(
                        id = newErdReport.id,
                        reasonTitle = reason,
                        reportCount = 1,
                        targetType = targetType,
                        targetTitle = targetTitle,
                        description = description,
                        targetPostId = targetPostId,
                        targetChatId = targetChatId
                    )
                ) + list
            }
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
        val convId = "chat_${System.currentTimeMillis()}"

        val newErdReq = ServiceRequest(
            id = convId,
            postId = postId,
            clientId = _currentUser.value.id,
            contactPhone = contactMethod,
            exactAddress = address,
            problemDescription = description,
            status = ServiceRequestStatus.PENDING
        )
        erdRequests.update { listOf(newErdReq) + it }

        val newConv = ChatConversation(
            id = convId,
            recipientName = recipient,
            serviceTitle = post?.title ?: "Servicio Solicitado",
            servicePrice = if (post?.isPriceToAgree == true) "A convenir" else "$ ${post?.minPrice ?: 25000} COP",
            status = ServiceRequestStatus.PENDING,
            lastMessage = description,
            lastTime = "Ahora",
            unreadCount = 0,
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
        _conversations.update { listOf(newConv) + it }
        return convId
    }

    override fun dismissReport(reportId: String) {
        _reports.update { list -> list.filterNot { it.id == reportId } }
    }

    override fun resolveReportsForPost(postId: String) {
        _reports.update { list -> list.filterNot { it.targetPostId == postId } }
    }

    override fun resolveReportsForChat(chatId: String) {
        _reports.update { list -> list.filterNot { it.targetChatId == chatId } }
    }

    override fun markAllNotificationsRead() {
        _notifications.update { list -> list.map { it.copy(read = true) } }
    }

    override fun toggleNotificationRead(notificationId: String) {
        _notifications.update { list -> list.map { if (it.id == notificationId) it.copy(read = !it.read) else it } }
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
            providerLevel = "Maestro 🏆",
            minPrice = 25000,
            maxPrice = 45000,
            location = "Armenia, Quindío - Barrio Granada",
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
        ServicePost(
            id = "pub_2",
            title = "Plomería y Destapes de Urgencia 24/7",
            category = "Hogar",
            description = "Atención inmediata para fugas de agua, cambio de griferías y destapes.",
            providerName = "Carlos R.",
            providerLevel = "Experto ★",
            minPrice = 25000,
            maxPrice = 50000,
            location = "Armenia, Quindío - Centro",
            distance = "A 1.2 km - Centro",
            rating = 4.9f,
            reviewCount = 42,
            upvotesCount = 114,
            status = PostStatus.APPROVED,
            warrantyMonths = 1,
            responseTime = "< 20 min",
            createdAtMillis = daysAgo(2)
        ),
        ServicePost(
            id = "pub_3",
            title = "Clases Particulares de Matemáticas y Física",
            category = "Educación",
            description = "Docente universitaria de matemáticas puras. Apoyo escolar y universitario.",
            providerName = "Valentina M.",
            providerLevel = "Maestro 🏆",
            minPrice = 15000,
            maxPrice = 30000,
            location = "Armenia, Quindío - Norte",
            distance = "A 3.5 km - Norte",
            rating = 5.0f,
            reviewCount = 52,
            upvotesCount = 88,
            status = PostStatus.APPROVED,
            warrantyMonths = 0,
            responseTime = "< 15 min",
            createdAtMillis = daysAgo(9)
        ),
        ServicePost(
            id = "pub_4",
            title = "Paseo y Cuidado Canino Personalizado",
            category = "Mascotas",
            description = "Paseos recreativos individuales o en grupos reducidos con seguimiento GPS.",
            providerName = "Matías S.",
            providerLevel = "Profesional",
            minPrice = 8000,
            maxPrice = 18000,
            location = "Armenia, Quindío - Parque de la Vida",
            distance = "A 0.8 km",
            rating = 4.8f,
            reviewCount = 19,
            upvotesCount = 49,
            status = PostStatus.APPROVED,
            warrantyMonths = 0,
            responseTime = "< 1 hora",
            createdAtMillis = daysAgo(20)
        ),
        ServicePost(
            id = "pub_5",
            title = "Cerrajería Móvil Urgencias 24/7 y Apertura de Puertas",
            category = "Hogar",
            description = "Servicio de cerrajería rápido a domicilio para casas y autos.",
            providerName = "Rodrigo Tapia",
            providerLevel = "Principiante",
            minPrice = 30000,
            maxPrice = 60000,
            location = "Armenia, Quindío - Los Profesionales",
            distance = "A 2.1 km",
            rating = 4.6f,
            reviewCount = 8,
            upvotesCount = 27,
            status = PostStatus.PENDING_VERIFICATION,
            warrantyMonths = 3,
            responseTime = "< 25 min",
            createdAtMillis = daysAgo(5)
        ),
        ServicePost(
            id = "pub_6",
            title = "Mantenimiento e Instalación de Tableros Eléctricos",
            category = "Electricidad",
            description = "Levantamiento de planos, detección de fugas y reemplazo de protecciones.",
            providerName = "Fernando Lucas",
            providerLevel = "Nuevo",
            minPrice = 35000,
            maxPrice = 85000,
            location = "Armenia, Quindío - La Castellana",
            distance = "A 1.8 km",
            rating = 5.0f,
            reviewCount = 2,
            upvotesCount = 15,
            status = PostStatus.PENDING_VERIFICATION,
            warrantyMonths = 6,
            responseTime = "< 45 min",
            createdAtMillis = daysAgo(1)
        )
    )

    private fun getInitialChats(): List<ChatConversation> = listOf(
        ChatConversation(
            id = "chat_1",
            recipientName = "Carlos R.",
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
            messages = listOf(
                ChatMessageItem("m1", "Carlos R.", "Hola Valentina, gusto en saludarte. Ya revisé la solicitud.", "10:14 AM", isFromMe = false),
                ChatMessageItem("m2", "Valentina Morales", "¡Hola Carlos! Muchas gracias.", "10:16 AM", isFromMe = true, read = true),
                ChatMessageItem("m3", "Carlos R.", "Perfecto, llevo el multímetro a las 3:00 PM.", "10:20 AM", isFromMe = false)
            )
        ),
        ChatConversation(
            id = "chat_2",
            recipientName = "Valentina Morales",
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
        ChatConversation(
            id = "chat_3",
            recipientName = "Andrés Gómez",
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
        ChatConversation(
            id = "chat_4",
            recipientName = "Francisca Valenzuela",
            serviceTitle = "Instalación Eléctrica Residencial",
            servicePrice = "$ 40.000 COP",
            status = ServiceRequestStatus.NEW_REQUEST,
            lastMessage = "¡Hola! ¿Tienes disponibilidad este viernes para revisar una cerradura en Calle Mayor?",
            lastTime = "10:45 AM",
            unreadCount = 1,
            isClientRole = false,
            messages = listOf(
                ChatMessageItem("m6", "Mariana Rivas", "¡Hola! ¿Tienes disponibilidad este viernes para revisar una cerradura?", "10:45 AM", isFromMe = false)
            )
        ),
        ChatConversation(
            id = "chat_5",
            recipientName = "Jorge Palacios",
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

    private fun getInitialNotifications(): List<NotificationItem> = listOf(
        NotificationItem("notif_1", "Nueva solicitud de servicio recibida", "Francisca Valenzuela ha solicitado tu servicio.", "Hace 10 min", NotificationType.REQUEST, false, relatedChatId = "chat_4"),
        NotificationItem("notif_2", "¡Publicación Aprobada!", "La publicación 'Instalación Eléctrica' está disponible en el Feed.", "Hace 2 horas", NotificationType.SYSTEM, false, relatedPostId = "pub_1"),
        NotificationItem("notif_3", "Nuevo comentario en tu anuncio", "Matías Sepúlveda preguntó por instalación de cargadores.", "Hace 4 horas", NotificationType.COMMENT, false, relatedPostId = "pub_1"),
        NotificationItem("notif_4", "¡Nuevo logro desbloqueado: Super Proveedor!", "Completaste 10 servicios con 5 estrellas.", "Ayer", NotificationType.ACHIEVEMENT, true),
        NotificationItem("notif_5", "A 5 vecinos les interesó tu servicio", "Tu publicación tuvo alta interacción.", "Hace 3 días", NotificationType.INTERACTION, true)
    )

    private fun getInitialReports(): List<ModerationReportItem> = listOf(
        ModerationReportItem("rep_1", "Solicitud de pago externo", 3, "Publicación", "Instalación Eléctrica Residencial y Reparaciones", "Pide transferencia previa por WhatsApp.", targetPostId = "pub_1", createdAtMillis = daysAgo(0)),
        ModerationReportItem("rep_2", "Datos de contacto en descripción", 1, "Publicación", "Cerrajería Móvil Urgencias 24/7 y Apertura de Puertas", "Incluye número privado en la descripción.", targetPostId = "pub_5", createdAtMillis = daysAgo(9)),
        ModerationReportItem("rep_3", "Servicio engañoso o no autorizado", 2, "Chat", "Pintura y Remodelaciones Integrales", "Lenguaje inapropiado e incumplimiento de cita.", targetChatId = "chat_5", createdAtMillis = daysAgo(20))
    )

    private fun daysAgo(days: Int): Long =
        java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, -days)
        }.timeInMillis

    private fun defaultCurrentUser(): UserProfile = UserProfile(
        id = "user_val_1",
        name = "Valentina Morales",
        email = "valentina.morales@servilocal.co",
        role = UserRole.USER,
        level = "Nivel 3: Destacado",
        points = 220,
        nextLevelPoints = 300,
        workingHours = "Lunes a sábado · 8:00 a. m. - 6:00 p. m."
    )

    private fun defaultModeratorUser(): UserProfile = UserProfile(
        id = "mod_ale_1",
        name = "Alejandro Vargas",
        email = "alejandro.vargas@staff.servilocal.pe",
        role = UserRole.MODERATOR,
        staffId = "MOD-4892",
        activeShift = true,
        workingHours = "Lunes a viernes · 8:00 a. m. - 4:30 p. m."
    )

    /** Convierte la entidad de sesión en el modelo que consumen las pantallas. */
    private fun User.toUserProfile(): UserProfile {
        val levelLabel = when (level) {
            UserLevel.PRINCIPIANTE -> "Nivel 1: Principiante"
            UserLevel.PROFESIONAL -> "Nivel 2: Profesional"
            UserLevel.EXPERTO -> "Nivel 3: Experto"
            UserLevel.MAESTRO -> "Nivel 4: Maestro"
        }
        val isModerator = role == UserRole.MODERATOR
        return UserProfile(
            id = id,
            name = name,
            email = email,
            role = role,
            level = levelLabel,
            points = points,
            nextLevelPoints = level.next?.minPoints ?: 1000,
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
