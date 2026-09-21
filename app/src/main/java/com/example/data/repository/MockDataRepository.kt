package com.example.data.repository

import com.example.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MockDataRepository {

    // Current logged in user
    private val _currentUser = MutableStateFlow(
        UserProfile(
            id = "user_val_1",
            name = "Valentina Morales",
            email = "valentina.morales@servilocal.co",
            role = UserRole.USER,
            level = "Nivel 3: Destacado",
            points = 220,
            nextLevelPoints = 300
        )
    )
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    // Moderator profile for staff view
    private val _moderatorUser = MutableStateFlow(
        UserProfile(
            id = "mod_ale_1",
            name = "Alejandro Vargas",
            email = "alejandro.vargas@staff.servilocal.pe",
            role = UserRole.MODERATOR,
            staffId = "MOD-4892",
            activeShift = true
        )
    )
    val moderatorUser: StateFlow<UserProfile> = _moderatorUser.asStateFlow()

    // Service posts
    private val _posts = MutableStateFlow<List<ServicePost>>(getInitialPosts())
    val posts: StateFlow<List<ServicePost>> = _posts.asStateFlow()

    // Chat conversations
    private val _conversations = MutableStateFlow<List<ChatConversation>>(getInitialChats())
    val conversations: StateFlow<List<ChatConversation>> = _conversations.asStateFlow()

    // Notifications
    private val _notifications = MutableStateFlow<List<NotificationItem>>(getInitialNotifications())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    // Moderation reports
    private val _reports = MutableStateFlow<List<ModerationReportItem>>(getInitialReports())
    val reports: StateFlow<List<ModerationReportItem>> = _reports.asStateFlow()

    // Upvote toggle
    fun toggleUpvote(postId: String) {
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

    // Add comment to a service
    fun addComment(postId: String, text: String, authorName: String, isAuthorReply: Boolean = false) {
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

    // Create new post
    fun createPost(
        title: String,
        category: String,
        description: String,
        minPrice: Int,
        maxPrice: Int,
        isPriceToAgree: Boolean,
        location: String
    ): ServicePost {
        val newPost = ServicePost(
            id = "pub_${System.currentTimeMillis()}",
            title = title,
            category = category,
            description = description,
            providerName = _currentUser.value.name,
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

    // Moderation: Approve post
    fun approvePost(postId: String) {
        _posts.update { list ->
            list.map { post ->
                if (post.id == postId) {
                    post.copy(status = PostStatus.APPROVED)
                } else post
            }
        }
    }

    // Moderation: Reject post with mandatory reason
    fun rejectPost(postId: String, reason: String, notes: String?) {
        _posts.update { list ->
            list.map { post ->
                if (post.id == postId) {
                    post.copy(
                        status = PostStatus.REJECTED,
                        rejectionReason = reason,
                        rejectionNotes = notes
                    )
                } else post
            }
        }
    }

    // Send chat message
    fun sendMessage(conversationId: String, text: String) {
        _conversations.update { list ->
            list.map { chat ->
                if (chat.id == conversationId) {
                    val newMsg = ChatMessageItem(
                        id = "m_${System.currentTimeMillis()}",
                        senderName = _currentUser.value.name,
                        text = text,
                        time = "Ahora",
                        isFromMe = true,
                        read = false
                    )
                    chat.copy(
                        lastMessage = text,
                        lastTime = "Ahora",
                        messages = chat.messages + newMsg
                    )
                } else chat
            }
        }
    }

    // Create request and start conversation
    fun createServiceRequest(
        postId: String,
        applicantName: String,
        contactMethod: String,
        address: String,
        description: String
    ): String {
        val post = _posts.value.find { it.id == postId }
        val recipient = post?.providerName ?: "Carlos Mendoza"
        val convId = "chat_${System.currentTimeMillis()}"

        val newConv = ChatConversation(
            id = convId,
            recipientName = recipient,
            serviceTitle = post?.title ?: "Servicio Solicitado",
            servicePrice = "$ ${post?.minPrice ?: 25000} COP",
            status = ChatStatus.PENDING,
            lastMessage = description,
            lastTime = "Ahora",
            unreadCount = 0,
            isClientRole = true,
            requestAddress = address,
            requestProblem = description,
            requestPhone = contactMethod,
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

    // Dismiss report
    fun dismissReport(reportId: String) {
        _reports.update { list -> list.filterNot { it.id == reportId } }
    }

    // Mark all notifications as read
    fun markAllNotificationsRead() {
        _notifications.update { list ->
            list.map { it.copy(read = true) }
        }
    }

    fun toggleNotificationRead(notificationId: String) {
        _notifications.update { list ->
            list.map { if (it.id == notificationId) it.copy(read = !it.read) else it }
        }
    }

    fun toggleModeratorShift() {
        _moderatorUser.update { it.copy(activeShift = !it.activeShift) }
    }

    private fun getInitialPosts(): List<ServicePost> = listOf(
        ServicePost(
            id = "pub_1",
            title = "Instalación Eléctrica Residencial y Reparaciones",
            category = "Hogar",
            description = "Ingeniero Eléctrico con certificación técnica oficial. Realizo diagnóstico, instalación y reparación de cableados domiciliarios, tableros eléctricos, detección de cortocircuitos y sobrecargas con instrumental calibrado de precisión.",
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
            comments = listOf(
                ServiceComment(
                    id = "c_1",
                    authorName = "Francisca Valenzuela",
                    date = "Ayer a las 18:40",
                    text = "Excelente servicio. Don Carlos detectó la fuga a tierra de inmediato y cambió el interruptor diferencial de forma muy pulcra y rápida. 100% recomendado.",
                    isAuthorReply = false,
                    isRecommended = true
                ),
                ServiceComment(
                    id = "c_2",
                    authorName = "Matías Sepúlveda",
                    date = "Hace 3 días",
                    text = "Hola Carlos, ¿haces instalación de cargadores para autos eléctricos tipo Wallbox domiciliario? Saludos.",
                    isAuthorReply = false,
                    isRecommended = false
                ),
                ServiceComment(
                    id = "c_3",
                    authorName = "Carlos Mendoza (Autor)",
                    date = "Hace 3 días",
                    text = "¡Hola Matías! Sí, cuento con certificación para dimensionamiento de empalmes e instalación de Wallbox. Puedes agendar una visita diagnóstica.",
                    isAuthorReply = true,
                    isRecommended = true
                )
            )
        ),
        ServicePost(
            id = "pub_2",
            title = "Plomería y Destapes de Urgencia 24/7",
            category = "Hogar",
            description = "Atención inmediata para fugas de agua, cambio de griferías, destapes con sonda eléctrica y reparación de sanitarios. Servicio limpio, rápido y con garantía escrita de 30 días.",
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
            responseTime = "< 20 min"
        ),
        ServicePost(
            id = "pub_3",
            title = "Clases Particulares de Matemáticas y Física",
            category = "Educación",
            description = "Docente universitaria de matemáticas puras. Apoyo escolar y preparación para exámenes universitarios (Cálculo I, II, Álgebra Lineal y Física). Modalidad online o presencial a domicilio en Armenia.",
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
            responseTime = "< 15 min"
        ),
        ServicePost(
            id = "pub_4",
            title = "Paseo y Cuidado Canino Personalizado",
            category = "Mascotas",
            description = "Paseos recreativos individuales o en grupos reducidos con seguimiento GPS en tiempo real. Adiestramiento básico en positivo y cuidados a domicilio.",
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
            responseTime = "< 1 hora"
        ),
        ServicePost(
            id = "pub_5",
            title = "Cerrajería Móvil Urgencias 24/7 y Apertura de Puertas",
            category = "Hogar",
            description = "Servicio de cerrajería rápido a domicilio para casas y autos sin romper cerraduras. Atención inmediata día y noche ante extravío de llaves, cambio de combinación y chapas blindadas.",
            providerName = "Rodrigo Tapia",
            providerLevel = "Principiante",
            minPrice = 30000,
            maxPrice = 60000,
            location = "Armenia, Quindío - Los Profesionales",
            distance = "A 2.1 km",
            rating = 4.6f,
            reviewCount = 8,
            upvotesCount = 27,
            status = PostStatus.PENDING_VERIFICATION, // In moderation queue
            warrantyMonths = 3,
            responseTime = "< 25 min"
        ),
        ServicePost(
            id = "pub_6",
            title = "Mantenimiento e Instalación de Tableros Eléctricos",
            category = "Electricidad",
            description = "Levantamiento de planos, detección de fugas con pinza amperimétrica True RMS, reemplazo de protecciones magnetotérmicas y diferenciales.",
            providerName = "Fernando Lucas",
            providerLevel = "Nuevo",
            minPrice = 35000,
            maxPrice = 85000,
            location = "Armenia, Quindío - La Castellana",
            distance = "A 1.8 km",
            rating = 5.0f,
            reviewCount = 2,
            upvotesCount = 15,
            status = PostStatus.PENDING_VERIFICATION, // In moderation queue
            warrantyMonths = 6,
            responseTime = "< 45 min"
        )
    )

    private fun getInitialChats(): List<ChatConversation> = listOf(
        ChatConversation(
            id = "chat_1",
            recipientName = "Carlos R.",
            serviceTitle = "Instalación Eléctrica Residencial",
            servicePrice = "$ 45.000 COP",
            status = ChatStatus.IN_PROGRESS,
            lastMessage = "Perfecto, llevo el multímetro y los repuestos a las 3:00 PM.",
            lastTime = "10:20 AM",
            unreadCount = 2,
            isClientRole = true,
            requestAddress = "Calle 14 # 23-45, Barrio Granada, Armenia, Quindío",
            requestProblem = "El interruptor principal del circuito de la cocina genera chispas al encender el horno eléctrico. Requiere revisión urgente.",
            requestPhone = "+57 312 456 7890",
            messages = listOf(
                ChatMessageItem(
                    id = "m1",
                    senderName = "Carlos R.",
                    text = "Hola Valentina, gusto en saludarte. Ya revisé la solicitud. Ese síntoma puede ser un falso contacto en la bornera del disyuntor.",
                    time = "10:14 AM",
                    isFromMe = false
                ),
                ChatMessageItem(
                    id = "m2",
                    senderName = "Valentina Morales",
                    text = "¡Hola Carlos! Muchas gracias. ¿Podrías traer repuesto por si toca cambiar el breaker?",
                    time = "10:16 AM",
                    isFromMe = true,
                    read = true
                ),
                ChatMessageItem(
                    id = "m3",
                    senderName = "Carlos R.",
                    text = "Perfecto, llevo el multímetro y los repuestos a las 3:00 PM.",
                    time = "10:20 AM",
                    isFromMe = false
                )
            )
        ),
        ChatConversation(
            id = "chat_2",
            recipientName = "Valentina Morales",
            serviceTitle = "Clases de Matemáticas",
            servicePrice = "$ 15.000 COP / hr",
            status = ChatStatus.PENDING,
            lastMessage = "Te adjunto el temario del examen de cálculo para revisarlo.",
            lastTime = "Hace 2 h",
            unreadCount = 0,
            isClientRole = true,
            messages = listOf(
                ChatMessageItem(
                    id = "m4",
                    senderName = "Valentina Morales",
                    text = "Te adjunto el temario del examen de cálculo para revisarlo.",
                    time = "Hace 2 h",
                    isFromMe = false
                )
            )
        ),
        ChatConversation(
            id = "chat_3",
            recipientName = "Andrés Gómez",
            serviceTitle = "Plomería y Destapes 24/7",
            servicePrice = "$ 30.000 COP",
            status = ChatStatus.COMPLETED,
            lastMessage = "Servicio completado exitosamente. ¡Muchas gracias por calificar!",
            lastTime = "Ayer",
            unreadCount = 0,
            isClientRole = true,
            messages = listOf(
                ChatMessageItem(
                    id = "m5",
                    senderName = "Andrés Gómez",
                    text = "Servicio completado exitosamente. ¡Muchas gracias por calificar!",
                    time = "Ayer",
                    isFromMe = false
                )
            )
        ),
        // Requests received as Provider
        ChatConversation(
            id = "chat_4",
            recipientName = "Mariana Rivas (Vecina)",
            serviceTitle = "Tu Anuncio: Reparaciones Hogar",
            servicePrice = "$ 40.000 COP",
            status = ChatStatus.NEW_REQUEST,
            lastMessage = "¡Hola! ¿Tienes disponibilidad este viernes para revisar una cerradura en Calle Mayor?",
            lastTime = "10:45 AM",
            unreadCount = 1,
            isClientRole = false,
            messages = listOf(
                ChatMessageItem(
                    id = "m6",
                    senderName = "Mariana Rivas",
                    text = "¡Hola! ¿Tienes disponibilidad este viernes para revisar una cerradura en Calle Mayor?",
                    time = "10:45 AM",
                    isFromMe = false
                )
            )
        ),
        ChatConversation(
            id = "chat_5",
            recipientName = "Jorge Palacios",
            serviceTitle = "Tu Anuncio: Reparaciones Hogar",
            servicePrice = "$ 65.000 COP",
            status = ChatStatus.IN_PROGRESS,
            lastMessage = "Me interesa cotizar la pintura para dos habitaciones. Te mando medidas.",
            lastTime = "Ayer",
            unreadCount = 0,
            isClientRole = false,
            messages = listOf(
                ChatMessageItem(
                    id = "m7",
                    senderName = "Jorge Palacios",
                    text = "Me interesa cotizar la pintura para dos habitaciones. Te mando medidas.",
                    time = "Ayer",
                    isFromMe = false
                )
            )
        )
    )

    private fun getInitialNotifications(): List<NotificationItem> = listOf(
        NotificationItem(
            id = "notif_1",
            title = "Nueva solicitud de servicio recibida",
            description = "Francisca Valenzuela ha solicitado tu servicio de 'Instalación Eléctrica Residencial'. Toca para revisar detalles.",
            time = "Hace 10 min",
            type = NotificationType.REQUEST,
            read = false
        ),
        NotificationItem(
            id = "notif_2",
            title = "¡Publicación Aprobada!",
            description = "Tu publicación 'Mantenimiento de Jardines' fue verificada y ahora es pública en el Feed de la comunidad.",
            time = "Hace 2 horas",
            type = NotificationType.SYSTEM,
            read = false
        ),
        NotificationItem(
            id = "notif_3",
            title = "Nuevo comentario en tu anuncio",
            description = "Matías Sepúlveda preguntó: '¿Haces instalación de cargadores para autos eléctricos...?'",
            time = "Hace 4 horas",
            type = NotificationType.COMMENT,
            read = false
        ),
        NotificationItem(
            id = "notif_4",
            title = "¡Nuevo logro desbloqueado: Super Proveedor!",
            description = "Completaste 10 servicios con calificación 5 estrellas. Ganaste 150 pts de reputación comunitaria.",
            time = "Ayer",
            type = NotificationType.ACHIEVEMENT,
            read = true
        ),
        NotificationItem(
            id = "notif_5",
            title = "A 5 vecinos les interesó tu servicio",
            description = "Tu publicación tuvo alta interacción en el sector de Los Profesionales y Centro.",
            time = "Hace 3 días",
            type = NotificationType.INTERACTION,
            read = true
        )
    )

    private fun getInitialReports(): List<ModerationReportItem> = listOf(
        ModerationReportItem(
            id = "rep_1",
            reasonTitle = "Solicitud de pago externo",
            reportCount = 3,
            targetType = "Publicación",
            targetTitle = "Electricista Domiciliario Urgencias 24/7",
            description = "Pide transferencia bancaria previa por WhatsApp antes de acudir al domicilio fuera de los canales de ServiLocal."
        ),
        ModerationReportItem(
            id = "rep_2",
            reasonTitle = "Datos de contacto en descripción",
            reportCount = 1,
            targetType = "Publicación",
            targetTitle = "Fletes y Mudanzas Express Armenia",
            description = "Incluye número de teléfono privado y enlace directo a Telegram en la descripción del servicio."
        ),
        ModerationReportItem(
            id = "rep_3",
            reasonTitle = "Servicio engañoso o no autorizado",
            reportCount = 2,
            targetType = "Chat",
            targetTitle = "Pintura y Remodelaciones Integrales",
            description = "Lenguaje inapropiado e incumplimiento de cita acordada tras confirmación de guardia."
        )
    )
}
