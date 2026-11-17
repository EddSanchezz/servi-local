package com.servilocal.data.repository.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import com.servilocal.domain.model.ChatConversation
import com.servilocal.domain.model.ChatMessageItem
import com.servilocal.domain.model.ChatStatus
import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.model.ServiceRequestStatus
import com.servilocal.domain.model.ServicePost
import com.servilocal.domain.model.UserProfile
import com.servilocal.domain.model.UserStatus
import com.servilocal.domain.repository.ChatRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

@Singleton
class FirebaseChatRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ChatRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val chatsCollection get() = firestore.collection("chats")

    private val _conversations = MutableStateFlow<List<ChatConversation>>(emptyList())
    override val conversations: StateFlow<List<ChatConversation>> = _conversations.asStateFlow()

    private var chatsListener: ListenerRegistration? = null
    private val messageListeners = mutableMapOf<String, ListenerRegistration>()
    private val postListeners = mutableMapOf<String, ListenerRegistration>()
    private val providerListeners = mutableMapOf<String, ListenerRegistration>()
    private val postAvailability = ConcurrentHashMap<String, PostStatus>()
    private val providerAvailability = ConcurrentHashMap<String, UserStatus>()
    private val chatCreationJobs = ConcurrentHashMap<String, Job>()
    private val avatarCache = ConcurrentHashMap<String, String>()
    private val avatarLoads = ConcurrentHashMap.newKeySet<String>()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val userId = firebaseAuth.currentUser?.uid
            chatsListener?.remove()
            messageListeners.values.forEach(ListenerRegistration::remove)
            messageListeners.clear()
            postListeners.values.forEach(ListenerRegistration::remove)
            postListeners.clear()
            providerListeners.values.forEach(ListenerRegistration::remove)
            providerListeners.clear()
            postAvailability.clear()
            providerAvailability.clear()
            if (userId != null) {
                chatsListener = chatsCollection
                    .whereArrayContains("participants", userId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null || snapshot == null) {
                            error?.let { Log.e(TAG, "No se pudieron leer las conversaciones", it) }
                            return@addSnapshotListener
                        }
                        val previousConversations = _conversations.value.associateBy { it.id }
                        val chats = snapshot.documents.mapNotNull { doc ->
                            val id = doc.id
                            val clientId = doc.getString("clientId") ?: ""
                            val providerId = doc.getString("providerId") ?: ""
                            val postId = doc.getString("postId") ?: ""
                            val clientName = doc.getString("clientName") ?: "Cliente"
                            val providerName = doc.getString("providerName") ?: "Proveedor"
                            val clientAvatar = doc.getString("clientAvatar")
                            val providerAvatar = doc.getString("providerAvatar")
                            clientAvatar?.takeIf(String::isNotBlank)?.let { avatarCache[clientId] = it }
                            providerAvatar?.takeIf(String::isNotBlank)?.let { avatarCache[providerId] = it }
                            ensureAvatarCached(clientId)
                            ensureAvatarCached(providerId)
                            observeAvailability(postId, providerId)
                            val serviceTitle = doc.getString("serviceTitle") ?: ""
                            val servicePrice = doc.getString("servicePrice") ?: ""
                            val statusStr = doc.getString("status") ?: ServiceRequestStatus.IN_PROGRESS.name
                            val lastMessage = doc.getString("lastMessage") ?: ""
                            val lastTime = doc.getString("lastTime") ?: "Ahora"
                            val deletedByUsers = (doc.get("deletedByUsers") as? List<*>)?.filterIsInstance<String>().orEmpty()
                            val unreadCounts = (doc.get("unreadCounts") as? Map<*, *>).orEmpty()
                            val postStatusStr = doc.getString("postStatus") ?: PostStatus.APPROVED.name
                            val providerStatusStr = doc.getString("providerStatus") ?: UserStatus.ACTIVE.name

                            if (userId in deletedByUsers) return@mapNotNull null

                            val isClient = userId == clientId
                            val recipient = if (isClient) providerName else clientName
                            val status = runCatching { ServiceRequestStatus.valueOf(statusStr) }.getOrDefault(ServiceRequestStatus.IN_PROGRESS)
                            val postStatus = runCatching { PostStatus.valueOf(postStatusStr) }.getOrDefault(PostStatus.APPROVED)
                            val providerStatus = runCatching { UserStatus.valueOf(providerStatusStr) }.getOrDefault(UserStatus.ACTIVE)
                            val currentPostStatus = postAvailability[postId] ?: postStatus
                            val currentProviderStatus = providerAvailability[providerId] ?: providerStatus

                            ChatConversation(
                                id = id,
                                clientId = clientId,
                                providerId = providerId,
                                postId = postId,
                                clientName = clientName,
                                providerName = providerName,
                                recipientName = recipient,
                                recipientAvatar = if (isClient) providerAvatar else clientAvatar,
                                clientAvatar = clientAvatar ?: avatarCache[clientId],
                                providerAvatar = providerAvatar ?: avatarCache[providerId],
                                serviceTitle = serviceTitle,
                                servicePrice = servicePrice,
                                status = status,
                                lastMessage = lastMessage,
                                lastTime = lastTime,
                                unreadCount = (unreadCounts[userId] as? Number)?.toInt() ?: 0,
                                isClientRole = isClient,
                                deletedByUsers = deletedByUsers,
                                postStatus = currentPostStatus,
                                providerStatus = currentProviderStatus,
                                messages = previousConversations[id]?.messages.orEmpty()
                            )
                        }

                        _conversations.value = chats
                        val activeChatIds = chats.map { it.id }.toSet()
                        messageListeners.keys
                            .filterNot { it in activeChatIds }
                            .toList()
                            .forEach { chatId ->
                                messageListeners.remove(chatId)?.remove()
                            }
                        chats.forEach { conversation ->
                            if (conversation.id !in messageListeners) {
                                listenToMessages(conversation.id, userId)
                            }
                            postListeners.keys
                                .filterNot { postId -> chats.any { it.postId == postId } }
                                .toList()
                                .forEach { postId -> postListeners.remove(postId)?.remove() }
                            providerListeners.keys
                                .filterNot { providerId -> chats.any { it.providerId == providerId } }
                                .toList()
                                .forEach { providerId -> providerListeners.remove(providerId)?.remove() }
                        }
                    }

            } else {
                _conversations.value = emptyList()
            }
        }
    }

    private fun ensureAvatarCached(userId: String) {
        if (userId.isBlank() || avatarCache.containsKey(userId) || !avatarLoads.add(userId)) return
        scope.launch {
            try {
                firestore.collection(PUBLIC_USERS_COLLECTION).document(userId).get().await()
                    .getString("photoUrl")
                    ?.takeIf(String::isNotBlank)
                    ?.let { avatarCache[userId] = it }
                _conversations.value = _conversations.value.map { conversation ->
                    if (conversation.clientId == userId || conversation.providerId == userId) {
                        val clientAvatar = conversation.clientAvatar ?: avatarCache[conversation.clientId]
                        val providerAvatar = conversation.providerAvatar ?: avatarCache[conversation.providerId]
                        conversation.copy(
                            clientAvatar = clientAvatar,
                            providerAvatar = providerAvatar,
                            recipientAvatar = if (conversation.isClientRole) providerAvatar else clientAvatar
                        )
                    } else {
                        conversation
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w(TAG, "No se pudo cargar el avatar público del usuario $userId", error)
            } finally {
                avatarLoads.remove(userId)
            }
        }
    }

    private fun observeAvailability(postId: String, providerId: String) {
        if (postId.isNotBlank() && postId !in postListeners) {
            postListeners[postId] = firestore.collection("posts")
                .document(postId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "No se pudo verificar la disponibilidad del post $postId", error)
                        return@addSnapshotListener
                    }
                    val status = if (snapshot?.exists() == true) {
                        parsePostStatus(snapshot.getString("status"))
                    } else {
                        PostStatus.REJECTED
                    }
                    postAvailability[postId] = status
                    updatePostAvailability(postId, status)
                }
        }
        if (providerId.isNotBlank() && providerId !in providerListeners) {
            providerListeners[providerId] = firestore.collection(PUBLIC_USERS_COLLECTION)
                .document(providerId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "No se pudo verificar el estado del proveedor $providerId", error)
                        return@addSnapshotListener
                    }
                    val status = if (snapshot?.exists() == true) {
                        parseUserStatus(snapshot.getString("status"))
                    } else {
                        UserStatus.DELETED
                    }
                    providerAvailability[providerId] = status
                    updateProviderAvailability(providerId, status)
                }
        }
    }

    private fun parsePostStatus(rawStatus: String?): PostStatus {
        return when (rawStatus?.trim()?.uppercase(Locale.ROOT)) {
            "REJECTED", "RECHAZADA" -> PostStatus.REJECTED
            "FINISHED", "FINALIZADA" -> PostStatus.FINISHED
            "PENDING_VERIFICATION", "PENDIENTE_VERIFICACION" -> PostStatus.PENDING_VERIFICATION
            "APPROVED", "VERIFIED", "VERIFICADA", "PUBLISHED", "ACTIVE" ->
                PostStatus.APPROVED
            else -> PostStatus.APPROVED
        }
    }

    private fun parseUserStatus(rawStatus: String?): UserStatus {
        return when (rawStatus?.trim()?.uppercase(Locale.ROOT)) {
            "BLOCKED", "BLOQUEADO" -> UserStatus.BLOCKED
            "DELETED", "ELIMINADO" -> UserStatus.DELETED
            else -> UserStatus.ACTIVE
        }
    }

    private fun updatePostAvailability(postId: String, status: PostStatus) {
        _conversations.value = _conversations.value.map { conversation ->
            if (conversation.postId == postId) {
                conversation.copy(
                    postStatus = status,
                    isPostAvailable = status != PostStatus.REJECTED &&
                        conversation.providerStatus != UserStatus.BLOCKED &&
                        conversation.providerStatus != UserStatus.DELETED
                )
            } else {
                conversation
            }
        }
    }

    private fun updateProviderAvailability(providerId: String, status: UserStatus) {
        _conversations.value = _conversations.value.map { conversation ->
            if (conversation.providerId == providerId) {
                conversation.copy(
                    providerStatus = status,
                    isPostAvailable = conversation.postStatus != PostStatus.REJECTED &&
                        status != UserStatus.BLOCKED &&
                        status != UserStatus.DELETED
                )
            } else {
                conversation
            }
        }
    }

    private fun listenToMessages(conversationId: String, currentUserId: String) {
        messageListeners[conversationId] = chatsCollection
            .document(conversationId)
            .collection("messages")
            .orderBy("sentAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    error?.let { Log.e(TAG, "No se pudieron leer los mensajes", it) }
                    return@addSnapshotListener
                }
                val messages = snapshot.documents.map { message ->
                    val senderId = message.getString("senderId").orEmpty()
                    val conversation = _conversations.value.firstOrNull { it.id == conversationId }
                    ChatMessageItem(
                        id = message.id,
                        senderName = message.getString("senderName") ?: "Usuario",
                        text = message.getString("text").orEmpty(),
                        time = formatMessageTime(message.getTimestamp("sentAt")),
                        isFromMe = senderId == currentUserId,
                        senderAvatar = when (senderId) {
                            conversation?.clientId -> conversation.clientAvatar
                            conversation?.providerId -> conversation.providerAvatar
                            else -> null
                        },
                        read = message.getBoolean("read") ?: true
                    )
                }

                _conversations.value = _conversations.value.map { conversation ->
                    if (conversation.id == conversationId) {
                        conversation.copy(
                            messages = messages,
                            lastMessage = messages.lastOrNull()?.text ?: conversation.lastMessage,
                            lastTime = messages.lastOrNull()?.time ?: conversation.lastTime
                        )
                    } else {
                        conversation
                    }
                }
            }
    }

    private fun formatMessageTime(timestamp: Timestamp?): String {
        val date = timestamp?.toDate() ?: return "Ahora"
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    }

    override fun sendMessage(conversationId: String, text: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        if (text.isBlank()) return
        scope.launch {
            try {
            chatCreationJobs[conversationId]?.join()
            val conversation = _conversations.value.firstOrNull { it.id == conversationId }
                ?: chatsCollection.document(conversationId).get().await().let { document ->
                    ChatConversation(
                        id = document.id,
                        clientId = document.getString("clientId").orEmpty(),
                        providerId = document.getString("providerId").orEmpty(),
                        recipientName = "",
                        serviceTitle = "",
                        servicePrice = "",
                        lastMessage = "",
                        lastTime = ""
                    )
                }
            val recipientId = if (conversation.clientId == currentUserId) {
                conversation.providerId
            } else {
                conversation.clientId
            }
            val messageData = mapOf(
                "senderId" to currentUserId,
                "senderName" to (auth.currentUser?.displayName ?: "Usuario"),
                "text" to text.trim(),
                "sentAt" to FieldValue.serverTimestamp()
            )
            chatsCollection.document(conversationId)
                .collection("messages")
                .add(messageData)
                .await()

                chatsCollection.document(conversationId).update(
                    mapOf(
                        "lastMessage" to text.trim(),
                        "lastTime" to "Ahora",
                        "deletedByUsers" to emptyList<String>(),
                        "unreadCounts.$recipientId" to FieldValue.increment(1)
                    )
                ).await()
            } catch (error: Exception) {
                Log.e(TAG, "No se pudo enviar el mensaje", error)
            }
        }
    }

    override fun softDeleteChat(conversationId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        scope.launch {
            try {
                chatsCollection.document(conversationId).update(
                    "deletedByUsers", FieldValue.arrayUnion(currentUserId)
                ).await()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w(TAG, "No se pudo ocultar el chat $conversationId", error)
            }
        }
    }

    override fun startConversation(postId: String): String? {
        val currentUserId = auth.currentUser?.uid ?: return null
        val existing = conversations.value.find { it.postId == postId && (it.clientId == currentUserId || it.providerId == currentUserId) }
        if (existing != null) {
            if (currentUserId in existing.deletedByUsers) {
                scope.launch {
                    try {
                        chatsCollection.document(existing.id).update(
                            "deletedByUsers", FieldValue.arrayRemove(currentUserId)
                        ).await()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        Log.w(TAG, "No se pudo reabrir el chat ${existing.id}", error)
                    }
                }
            }
            return existing.id
        }
        return null
    }

    override fun startConversation(post: ServicePost, currentUser: UserProfile): String? {
        val currentUserId = auth.currentUser?.uid ?: currentUser.id
        val providerId = post.providerId ?: return null
        if (providerId == currentUserId) return null

        val existing = conversations.value.find {
            it.postId == post.id &&
                ((it.clientId == currentUserId && it.providerId == providerId) ||
                    (it.clientId == providerId && it.providerId == currentUserId))
        }
        if (existing != null) {
            if (currentUserId in existing.deletedByUsers) {
                scope.launch {
                    try {
                        chatsCollection.document(existing.id).update(
                            "deletedByUsers", FieldValue.arrayRemove(currentUserId)
                        ).await()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        Log.w(TAG, "No se pudo reabrir el chat ${existing.id}", error)
                    }
                }
            }
            return existing.id
        }

        val chatRef = chatsCollection.document()
        val providerPrice = if (post.isPriceToAgree) "A convenir" else "$ ${post.minPrice} COP"
        val chatData = mapOf(
            "clientId" to currentUserId,
            "providerId" to providerId,
            "participants" to listOf(currentUserId, providerId),
            "postId" to post.id,
            "serviceTitle" to post.title,
            "servicePrice" to providerPrice,
            "clientName" to currentUser.name,
            "providerName" to post.providerName,
            "clientAvatar" to currentUser.photoUrl,
            "providerAvatar" to post.providerAvatar,
            "unreadCounts" to mapOf(currentUserId to 0, providerId to 0),
            "lastMessage" to "",
            "lastTime" to "Ahora",
            "status" to ServiceRequestStatus.IN_PROGRESS.name,
            "deletedByUsers" to emptyList<String>(),
            "postStatus" to post.status.name,
            "providerStatus" to post.providerStatus.name
        )
        val localConversation = ChatConversation(
            id = chatRef.id,
            clientId = currentUserId,
            providerId = providerId,
            postId = post.id,
            clientName = currentUser.name,
            providerName = post.providerName,
            recipientName = post.providerName,
            recipientAvatar = post.providerAvatar,
            clientAvatar = currentUser.photoUrl,
            providerAvatar = post.providerAvatar,
            serviceTitle = post.title,
            servicePrice = providerPrice,
            status = ServiceRequestStatus.IN_PROGRESS,
            lastMessage = "",
            lastTime = "Ahora",
            isClientRole = true
        )
        _conversations.value = listOf(localConversation) + _conversations.value

        val creationJob = scope.launch(start = CoroutineStart.LAZY) {
            try {
                chatRef.set(chatData).await()
                listenToMessages(chatRef.id, currentUserId)
            } catch (error: Exception) {
                Log.e(TAG, "No se pudo crear el chat ${chatRef.id}", error)
                _conversations.value = _conversations.value.filterNot { it.id == chatRef.id }
            } finally {
                chatCreationJobs.remove(chatRef.id)
            }
        }
        chatCreationJobs[chatRef.id] = creationJob
        creationJob.start()
        return chatRef.id
    }

    override fun markConversationRead(conversationId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        scope.launch {
            try {
                chatsCollection.document(conversationId)
                    .update("unreadCounts.$currentUserId", 0)
                    .await()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w(TAG, "No se pudo marcar leído el chat $conversationId", error)
            }
        }
    }

    override fun updateConversationStatus(conversationId: String, status: ChatStatus) {
        scope.launch {
            try {
                chatsCollection.document(conversationId).update("status", status.name).await()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w(TAG, "No se pudo actualizar el estado del chat $conversationId", error)
            }
        }
    }

    override fun createServiceRequest(
        postId: String,
        applicantName: String,
        contactMethod: String,
        address: String,
        description: String
    ): String? {
        return startConversation(postId)
    }

    private companion object {
        const val TAG = "FirebaseChatRepository"
        const val PUBLIC_USERS_COLLECTION = "users_public"
    }
}
