# Especificación e Implementación del Módulo de Chat en Firebase

Este documento define la arquitectura, modelo de datos, reglas de negocio y ciclo de vida para la implementación del módulo de Chat respaldado por **Firebase Cloud Firestore** con sincronización en tiempo real.

---

## 1. Reglas de Negocio y Flujo de Usuario

### A. Inicio de Conversación Simplificado
1. Se elimina por completo el formulario formal de solicitud (`ServiceRequestDialog`).
2. Al presionar **"Contactar"** en una publicación aprobada, se ejecuta `StartConversationUseCase(postId)`.
3. El caso de uso evalúa la relación `Cliente <-> Proveedor <-> Post`:
   - Si no existe un chat previo, crea la conversación y retorna `StartConversationResult(conversationId, isNewChat = true)`.
   - Si ya existía una conversación previa (o fue ocultada visualmente), reactiva el chat y retorna `StartConversationResult(conversationId, isNewChat = false)`.

### B. Mensaje Predeterminado en la Barra de Entrada
- **Únicamente cuando `isNewChat == true`:** La pantalla del chat ([`ChatDetailScreen`](file:///C:/Users/MI%20PC/workspace/servi-local/app/src/main/java/com/servilocal/features/chat/ui/ChatDetailScreen.kt)) coloca el texto `"Me interesa '[Título del Post]'"` en el campo/barra de escritura (`messageInputText`).
- **Comportamiento:** Este texto **NO se envía automáticamente**. Queda en el campo de texto para que el usuario pueda editarlo o presionar el botón de enviar voluntariamente.
- Si `isNewChat == false`, la barra de escritura se inicia vacía y se carga el historial de mensajes previo.

### C. Eliminación Visual (Soft Delete por Usuario)
- Se eliminan los botones de acción obsoletos ("Aceptar solicitud", "Rechazar", "Cancelar solicitud").
- En el menú de opciones del chat, el usuario o moderador puede seleccionar **"Eliminar chat"**.
- **Soft Delete en Firestore:** La conversación no se borra físicamente de la base de datos, sino que se agrega el ID del usuario al arreglo `deletedByUsers: ["usr_id"]`.
- La lista de conversaciones del usuario filtra los chats donde su ID esté presente en `deletedByUsers`.
- Si el usuario vuelve a presionar "Contactar" en la misma publicación, el chat se reabre (se remueve su ID de `deletedByUsers`), recupera todo el historial y `isNewChat` será `false` (no muestra el mensaje predeterminado).

### D. Indisponibilidad por Rechazo de Post o Bloqueo de Usuario
- Un chat depende de la vigencia del `Post` y de los usuarios involucrados:
  - Si el `Post` pasa a estado `REJECTED` (por moderación/reportes) o es eliminado.
  - O si el usuario autor/proveedor pasa a estado `UserStatus.BLOCKED` / `DELETED`.
- **Efecto en la UI ([`ChatDetailScreen`](file:///C:/Users/MI%20PC/workspace/servi-local/app/src/main/java/com/servilocal/features/chat/ui/ChatDetailScreen.kt)):**
  - La interfaz detecta `isPostAvailable == false`.
  - Deshabilita la barra de entrada de texto y el botón de enviar.
  - Muestra un banner explicativo: **"Este servicio/post ya no está disponible."**

---

## 2. Sincronización en Tiempo Real con Firebase (`.addSnapshotListener()`)

Para garantizar la recepción instantánea de mensajes y cambios de estado entre el cliente y el proveedor, el repositorio utiliza listeners reactivos de Cloud Firestore:

```kotlin
// Escucha reactiva en tiempo real para las conversaciones del usuario
override fun observeConversations(userId: String): Flow<List<ChatConversation>> = callbackFlow {
    val query = firestore.collection("chats")
        .whereArrayContains("participants", userId)

    val listener = query.addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(error)
            return@addSnapshotListener
        }
        val chats = snapshot?.documents.orEmpty().mapNotNull { doc ->
            doc.toChatConversation(currentUserId = userId)
        }.filterNot { userId in it.deletedByUsers }

        trySend(chats)
    }

    awaitClose { listener.remove() }
}
```

Asimismo, el detalle del chat escucha la subcolección de mensajes `/chats/{chatId}/messages` con `.addSnapshotListener()`, emitiendo los nuevos mensajes en tiempo real a la interfaz.

---

## 3. Estructura de Documentos en Cloud Firestore

### Colección Principal: `/chats/{chatId}`
```text
chats/{chatId}
  ├── clientId: "usr_cliente_123"
  ├── providerId: "usr_proveedor_456"
  ├── participants: ["usr_cliente_123", "usr_proveedor_456"]
  ├── postId: "pub_789"
  ├── postTitle: "Instalación Eléctrica Residencial"
  ├── postPrice: "$ 45.000 COP"
  ├── postStatus: "APPROVED" | "REJECTED"
  ├── providerStatus: "ACTIVE" | "BLOCKED"
  ├── deletedByUsers: []                    <-- Contiene los IDs de usuarios que hicieron soft-delete
  ├── lastMessage: "Hola, me interesa la cotización"
  ├── lastTime: ServerTimestamp
  └── unreadCounts: {
        "usr_cliente_123": 0,
        "usr_proveedor_456": 1
      }
```

### Subcolección: `/chats/{chatId}/messages/{messageId}`
```text
messages/{messageId}
  ├── senderId: "usr_cliente_123"
  ├── senderName: "Valentina Morales"
  ├── text: "Hola, me interesa la instalación eléctrica"
  ├── sentAt: ServerTimestamp
  └── read: false
```

---

## 4. Casos de Uso del Dominio (`domain/usecase/chat/`)

- [`ObserveChatConversationsUseCase`](file:///C:/Users/MI%20PC/workspace/servi-local/app/src/main/java/com/servilocal/domain/usecase/chat/ObserveChatConversationsUseCase.kt): Escucha las conversaciones activas mediante `.addSnapshotListener()`.
- [`StartConversationUseCase`](file:///C:/Users/MI%20PC/workspace/servi-local/app/src/main/java/com/servilocal/domain/usecase/chat/StartConversationUseCase.kt): Retorna `StartConversationResult(conversationId, isNewChat)`.
- [`SendMessageUseCase`](file:///C:/Users/MI%20PC/workspace/servi-local/app/src/main/java/com/servilocal/domain/usecase/chat/SendMessageUseCase.kt): Guarda el nuevo mensaje en Firestore y remueve al remitente de `deletedByUsers` si estaba oculto.
- [`MarkConversationReadUseCase`](file:///C:/Users/MI%20PC/workspace/servi-local/app/src/main/java/com/servilocal/domain/usecase/chat/MarkConversationReadUseCase.kt): Reinicia el contador de no leídos para el usuario.
- [`SoftDeleteChatUseCase`](file:///C:/Users/MI%20PC/workspace/servi-local/app/src/main/java/com/servilocal/domain/usecase/chat/SoftDeleteChatUseCase.kt): Agrega el `userId` a `deletedByUsers`.

---

## 5. Disponibilidad sincronizada y comportamiento de la UI

`FirebaseChatRepository` mantiene listeners independientes con
`addSnapshotListener()` para:

- `/chats/{chatId}`: datos de la conversación, mensajes pendientes y contadores.
- `/chats/{chatId}/messages`: historial ordenado por `sentAt`.
- `/posts/{postId}`: estado del post y existencia del documento.
- `/users/{providerId}`: estado actual del proveedor.

El chat se considera disponible únicamente cuando:

```text
postStatus != REJECTED
providerStatus != BLOCKED
providerStatus != DELETED
```

Si el post se elimina, el listener lo interpreta como `REJECTED`. Si el
documento del proveedor desaparece, se interpreta como `DELETED`. Estos
cambios actualizan `ChatConversation.isPostAvailable` en tiempo real sin
cerrar la conversación ni eliminar su historial.

Cuando `isPostAvailable == false`, `ChatDetailScreen`:

- Deshabilita el campo de texto.
- Deshabilita el botón de enviar y evita el envío aunque exista texto pendiente.
- Muestra el banner: **"Este servicio/post ya no está disponible."**

Los avatares se obtienen desde `users.photoUrl` y `posts.providerAvatar`.
`FirebaseChatRepository` mantiene una caché en memoria por `userId`, mientras
`AsyncImage` reutiliza la caché de Coil para evitar lecturas repetidas.

Las reglas de Firestore deben publicarse con:

```bash
firebase deploy --only firestore:rules
```
