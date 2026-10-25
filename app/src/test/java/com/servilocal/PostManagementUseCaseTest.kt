package com.servilocal

import com.servilocal.core.data.repository.MockDataRepository
import com.servilocal.domain.model.PostStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PostManagementUseCaseTest {

    @Test
    fun `owner edits and finishes approved posts and cannot open a self conversation`() {
        val repository = MockDataRepository()
        val ownPost = repository.posts.value.first { it.providerId == 1L && it.status == PostStatus.APPROVED }
        val otherPost = repository.posts.value.first { it.providerId != 1L && it.status == PostStatus.APPROVED }

        assertNull(repository.startConversation(ownPost.id))
        assertFalse(
            repository.updatePost(
                otherPost.id, "Ajeno", "Hogar", "No permitido", 10000, 20000, false, "Armenia"
            )
        )

        assertTrue(
            repository.updatePost(
                ownPost.id, "Título actualizado", "Educación", "Nueva descripción", 20000, 40000, false, "Armenia"
            )
        )
        assertEquals("Título actualizado", repository.posts.value.first { it.id == ownPost.id }.title)

        assertTrue(repository.finishPost(ownPost.id))
        assertEquals(PostStatus.FINISHED, repository.posts.value.first { it.id == ownPost.id }.status)

        assertFalse(
            repository.updatePost(
                ownPost.id, "Otra", "Educación", "Desc", 10000, 20000, false, "Armenia"
            )
        )
    }
}
