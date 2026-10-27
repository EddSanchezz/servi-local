package com.servilocal

import com.servilocal.core.data.repository.MockDataRepository
import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.model.ServiceRequestStatus
import com.servilocal.domain.usecase.chat.CreateServiceRequestResult
import com.servilocal.domain.usecase.chat.CreateServiceRequestUseCase
import com.servilocal.domain.usecase.chat.ObserveChatMessagesUseCase
import com.servilocal.domain.usecase.chat.ObserveMyRequestsUseCase
import com.servilocal.domain.usecase.chat.ObserveReceivedRequestsUseCase
import com.servilocal.domain.usecase.chat.SendMessageUseCase
import com.servilocal.domain.usecase.chat.UpdateRequestStatusUseCase
import com.servilocal.domain.usecase.notifications.CreateNotificationUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceRequestUseCaseTest {

    @Test
    fun `request validates inputs ownership and publication state`() {
        val repository = MockDataRepository()
        val useCase = CreateServiceRequestUseCase(repository, repository, repository, CreateNotificationUseCase(repository))
        val initialConversationCount = repository.conversations.value.size

        assertEquals(
            CreateServiceRequestResult.MissingApplicantName,
            useCase("pub_1", " ", "3001234567", "Centro", "Reparar una fuga")
        )
        assertEquals(
            CreateServiceRequestResult.MissingContactMethod,
            useCase("pub_1", "Ana", " ", "Centro", "Reparar una fuga")
        )
        assertEquals(
            CreateServiceRequestResult.OwnPost,
            useCase("pub_3", "Valentina", "3001234567", "Norte", "Necesito tutoría")
        )
        assertEquals(
            CreateServiceRequestResult.PostNotApproved,
            useCase("pub_5", "Ana", "3001234567", "Centro", "Abrir una puerta")
        )
        assertEquals(
            CreateServiceRequestResult.PostNotFound,
            useCase("missing", "Ana", "3001234567", "Centro", "Solicito servicio")
        )
        assertEquals(initialConversationCount, repository.conversations.value.size)
    }

    @Test
    fun `request creates a conversation with contact and address and notifies provider`() = runTest {
        val repository = MockDataRepository()
        val useCase = CreateServiceRequestUseCase(repository, repository, repository, CreateNotificationUseCase(repository))

        val result = useCase("pub_1", "Valentina Morales", "3001234567", "Calle 10 #12-34", "Revisar un tablero")

        assertTrue(result is CreateServiceRequestResult.Success)
        val conversationId = (result as CreateServiceRequestResult.Success).conversationId
        val conversation = repository.conversations.value.first { it.id == conversationId }
        assertEquals(ServiceRequestStatus.PENDING, conversation.status)
        assertEquals("Calle 10 #12-34", conversation.requestAddress)
        assertEquals("Revisar un tablero", conversation.requestProblem)
        assertEquals("3001234567", conversation.requestPhone)
        assertTrue(repository.notifications.value.any { it.userId == 2L && it.relatedChatId == conversationId })

        val sent = ObserveMyRequestsUseCase(repository)().first()
        val received = ObserveReceivedRequestsUseCase(repository)().first()
        assertTrue(sent.any { it.id == conversationId && it.isClientRole })
        assertFalse(received.any { it.id == conversationId })
    }

    @Test
    fun `messages and request status remain reactive for both participants`() = runTest {
        val repository = MockDataRepository()
        val request = CreateServiceRequestUseCase(repository, repository, repository, CreateNotificationUseCase(repository))(
            "pub_1", "Valentina Morales", "3001234567", "Centro", "Cambiar una toma eléctrica"
        ) as CreateServiceRequestResult.Success
        val observeMessages = ObserveChatMessagesUseCase(repository)
        val sendMessage = SendMessageUseCase(repository, repository, CreateNotificationUseCase(repository))
        val updateStatus = UpdateRequestStatusUseCase(repository, repository, CreateNotificationUseCase(repository))

        sendMessage(request.conversationId, "¿Puedes venir mañana?")
        assertTrue(observeMessages(request.conversationId).first().any { it.text == "¿Puedes venir mañana?" })

        updateStatus(request.conversationId, ServiceRequestStatus.IN_PROGRESS)
        assertEquals(
            ServiceRequestStatus.IN_PROGRESS,
            repository.conversations.value.first { it.id == request.conversationId }.status
        )
        assertEquals(PostStatus.APPROVED, repository.posts.value.first { it.id == "pub_1" }.status)
    }
}
