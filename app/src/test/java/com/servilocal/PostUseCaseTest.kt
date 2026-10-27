package com.servilocal

import com.servilocal.core.data.repository.MockDataRepository
import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.usecase.posts.DeleteServicePostUseCase
import com.servilocal.domain.usecase.posts.FinishServicePostUseCase
import com.servilocal.domain.usecase.posts.FilterPostsByCategoryUseCase
import com.servilocal.domain.usecase.posts.FilterPostsByLocationUseCase
import com.servilocal.domain.usecase.posts.FilterPostsByQueryUseCase
import com.servilocal.domain.usecase.posts.GetPostDetailUseCase
import com.servilocal.domain.usecase.posts.ObserveMyPostsUseCase
import com.servilocal.domain.usecase.posts.ObservePostsUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PostUseCaseTest {

    @Test
    fun `feed only emits approved posts and category query and location filters compose`() = runTest {
        val repository = MockDataRepository()
        val feedFlow: kotlinx.coroutines.flow.Flow<List<com.servilocal.domain.model.ServicePost>> =
            ObservePostsUseCase(repository).invoke()
        val feed = feedFlow.first()

        assertTrue(feed.isNotEmpty())
        assertTrue(feed.all { it.status == PostStatus.APPROVED })
        assertEquals(
            listOf("pub_4"),
            FilterPostsByLocationUseCase()(feed, locationQuery = "Parque de la Vida", maxDistanceKm = 1.0)
                .map { it.id }
        )
        assertEquals(
            listOf("pub_2", "pub_4"),
            FilterPostsByLocationUseCase()(feed, locationQuery = "", maxDistanceKm = 1.0).map { it.id }
        )

        val hogarPosts = FilterPostsByCategoryUseCase()(feed, "Hogar")
        assertTrue(hogarPosts.isNotEmpty())
        assertTrue(hogarPosts.all { it.category.equals("Hogar", ignoreCase = true) })
        assertEquals(
            listOf("pub_2"),
            FilterPostsByQueryUseCase()(feed, "Destapes de Urgencia").map { it.id }
        )
    }

    @Test
    fun `my posts updates from the active user and does not include other authors`() = runTest {
        val repository = MockDataRepository()
        val myPostsFlow: kotlinx.coroutines.flow.Flow<List<com.servilocal.domain.model.ServicePost>> =
            ObserveMyPostsUseCase(repository, repository).invoke()
        val myPosts = myPostsFlow.first()

        assertTrue(myPosts.isNotEmpty())
        assertTrue(myPosts.all { it.providerId == repository.currentUser.value.id })
        assertTrue(myPosts.any { it.id == "pub_3" })
    }

    @Test
    fun `post detail observes deletion and only its owner can delete it`() = runTest {
        val repository = MockDataRepository()
        val detail = GetPostDetailUseCase(repository)

        assertEquals("pub_3", detail.invoke("pub_3").first()?.id)
        assertFalse(DeleteServicePostUseCase(repository)("pub_1"))
        assertTrue(DeleteServicePostUseCase(repository)("pub_3"))
        assertNull(detail.invoke("pub_3").first())
        assertFalse(DeleteServicePostUseCase(repository)("pub_3"))
    }

    @Test
    fun `shared resolved use case lets the owner finish an approved publication`() {
        val repository = MockDataRepository()
        val finishPost = FinishServicePostUseCase(
            com.servilocal.domain.usecase.posts.MarkPostAsResolvedUseCase(repository, repository)
        )

        assertTrue(finishPost("pub_3"))
        assertEquals(PostStatus.FINISHED, repository.posts.value.first { it.id == "pub_3" }.status)
        assertFalse(finishPost("pub_3"))
    }

    @Test
    fun `new publications receive unique ids and start pending moderation`() {
        val repository = MockDataRepository()

        val first = repository.createPost("Servicio uno", "Hogar", "Descripción", 10000, 20000, false, "Armenia")
        val second = repository.createPost("Servicio dos", "Hogar", "Descripción", 10000, 20000, false, "Armenia")

        assertNotEquals(first.id, second.id)
        assertEquals(PostStatus.PENDING_VERIFICATION, first.status)
        assertEquals(PostStatus.PENDING_VERIFICATION, second.status)
        assertEquals(4.5339, first.latitude!!, 0.0001)
        assertEquals(-75.6811, first.longitude!!, 0.0001)
    }

    @Test
    fun `localized category names are accepted and unknown categories are rejected`() {
        val repository = MockDataRepository()

        val educationPost = repository.createPost(
            "Tutorías", "Educación", "Tutorías de matemáticas", 10000, 20000, false, "Armenia"
        )
        val invalidCategory = runCatching {
            repository.createPost("Otro servicio", "Categoría inválida", "Descripción", 10000, 20000, false, "Armenia")
        }

        assertEquals("Educación", educationPost.category)
        assertTrue(invalidCategory.isFailure)
        assertEquals(1, repository.posts.value.count { it.id == educationPost.id })
    }
}
