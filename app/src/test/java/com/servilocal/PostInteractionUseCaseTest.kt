package com.servilocal

import com.servilocal.core.data.repository.MockDataRepository
import com.servilocal.domain.model.NotificationType
import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.usecase.notifications.CreateNotificationUseCase
import com.servilocal.domain.usecase.posts.AddCommentResult
import com.servilocal.domain.usecase.posts.AddPostCommentUseCase
import com.servilocal.domain.usecase.posts.ObservePostCommentsUseCase
import com.servilocal.domain.usecase.posts.ObservePostLikesUseCase
import com.servilocal.domain.usecase.posts.ToggleLikeResult
import com.servilocal.domain.usecase.posts.TogglePostLikeUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PostInteractionUseCaseTest {

    private fun repository() = MockDataRepository()

    private fun toggleLikeUseCase(repository: MockDataRepository) =
        TogglePostLikeUseCase(repository, repository, repository, CreateNotificationUseCase(repository))

    private fun addCommentUseCase(repository: MockDataRepository) =
        AddPostCommentUseCase(repository, repository, repository, CreateNotificationUseCase(repository))

    @Test
    fun `a user can like another users post only once and remove that like`() {
        val repository = repository()
        val useCase = toggleLikeUseCase(repository)
        val post = repository.posts.value.first { it.id == "pub_1" }
        val initialCount = post.upvotesCount

        assertEquals(ToggleLikeResult.Success(liked = true), useCase(post.id))

        val liked = repository.posts.value.first { it.id == post.id }
        assertTrue(liked.userUpvoted)
        assertEquals(initialCount + 1, liked.upvotesCount)
        assertEquals(1, repository.likes.value.size)

        assertEquals(ToggleLikeResult.Success(liked = false), useCase(post.id))

        val unliked = repository.posts.value.first { it.id == post.id }
        assertFalse(unliked.userUpvoted)
        assertEquals(initialCount, unliked.upvotesCount)
        assertTrue(repository.likes.value.isEmpty())
    }

    @Test
    fun `removing a like does not notify the author again`() {
        val repository = repository()
        val useCase = toggleLikeUseCase(repository)
        val authorId = repository.posts.value.first { it.id == "pub_1" }.providerId
        fun likeNotifications() = repository.notifications.value.count {
            it.userId == authorId && it.type == NotificationType.INTERACTION
        }
        val before = likeNotifications()

        useCase("pub_1")
        useCase("pub_1")

        assertEquals(before + 1, likeNotifications())
    }

    @Test
    fun `a user cannot like their own post but can comment on it as the author`() {
        val repository = repository()
        val ownPost = repository.posts.value.first { it.providerId == repository.currentUser.value.id }

        assertEquals(ToggleLikeResult.OwnPost, toggleLikeUseCase(repository)(ownPost.id))
        assertEquals(AddCommentResult.Success, addCommentUseCase(repository)(ownPost.id, "Comentario propio"))

        val comment = repository.posts.value.first { it.id == ownPost.id }.comments.first()
        assertTrue(comment.isAuthorReply)
        assertEquals(repository.currentUser.value.id, comment.authorId)

        assertFalse(repository.posts.value.first { it.id == ownPost.id }.userUpvoted)
        assertTrue(repository.likes.value.isEmpty())
        assertEquals(1, repository.comments.value.count { it.postId == ownPost.id })
    }

    @Test
    fun `posts that are not verified cannot be liked or commented`() {
        val repository = repository()
        val pending = repository.posts.value.first { it.status == PostStatus.PENDING_VERIFICATION }

        assertEquals(ToggleLikeResult.PostNotVerified, toggleLikeUseCase(repository)(pending.id))
        assertEquals(AddCommentResult.PostNotVerified, addCommentUseCase(repository)(pending.id, "Hola"))
        assertEquals(ToggleLikeResult.PostNotFound, toggleLikeUseCase(repository)("no_existe"))
        assertEquals(AddCommentResult.PostNotFound, addCommentUseCase(repository)("no_existe", "Hola"))
        assertTrue(repository.likes.value.isEmpty())
        assertTrue(repository.comments.value.isEmpty())
    }

    @Test
    fun `a user can leave multiple comments on another users post`() {
        val repository = repository()
        val useCase = addCommentUseCase(repository)
        val post = repository.posts.value.first { it.id == "pub_2" }

        assertEquals(AddCommentResult.Success, useCase(post.id, "Primer comentario"))
        assertEquals(AddCommentResult.Success, useCase(post.id, "Segundo comentario"))

        assertEquals(2, repository.comments.value.count { it.postId == post.id })
        assertEquals(2, repository.posts.value.first { it.id == post.id }.comments.size)
        // Los ids deben ser únicos porque la lista de la UI los usa como key.
        assertEquals(2, repository.comments.value.map { it.id }.toSet().size)
    }

    @Test
    fun `approving a new post does not add a like automatically`() {
        val repository = repository()
        val newPost = repository.createPost(
            title = "Nuevo servicio",
            category = "Hogar",
            description = "Descripción del servicio",
            minPrice = 10000,
            maxPrice = 20000,
            isPriceToAgree = false,
            location = "Armenia"
        )

        assertEquals(0, newPost.upvotesCount)
        assertFalse(newPost.userUpvoted)

        repository.approvePost(newPost.id)

        val approved = repository.posts.value.first { it.id == newPost.id }
        assertEquals(PostStatus.APPROVED, approved.status)
        assertEquals(0, approved.upvotesCount)
        assertFalse(approved.userUpvoted)
        assertTrue(repository.likes.value.isEmpty())
    }

    @Test
    fun `blank comments are rejected without touching the comment table`() {
        val repository = repository()
        val post = repository.posts.value.first { it.id == "pub_2" }

        assertEquals(AddCommentResult.EmptyContent, addCommentUseCase(repository)(post.id, "   "))
        assertTrue(repository.comments.value.isEmpty())
    }

    @Test
    fun `comments longer than the limit are rejected`() {
        val repository = repository()
        val tooLong = "a".repeat(AddPostCommentUseCase.MAX_LENGTH + 1)

        assertEquals(AddCommentResult.TooLong, addCommentUseCase(repository)("pub_2", tooLong))
        assertTrue(repository.comments.value.isEmpty())
    }

    @Test
    fun `observe likes reflects the count and whether the current user liked the post`() = runTest {
        val repository = repository()
        val observeLikes = ObservePostLikesUseCase(repository, repository, repository)
        val initial = observeLikes("pub_1").first()
        assertFalse(initial.likedByCurrentUser)

        toggleLikeUseCase(repository)("pub_1")

        val afterLike = observeLikes("pub_1").first()
        assertTrue(afterLike.likedByCurrentUser)
        assertEquals(initial.count + 1, afterLike.count)
    }

    @Test
    fun `observe comments returns the newest comment first`() = runTest {
        val repository = repository()
        val observeComments = ObservePostCommentsUseCase(repository)
        val initialSize = observeComments("pub_2").first().size

        addCommentUseCase(repository)("pub_2", "¿Atienden los domingos?")

        val comments = observeComments("pub_2").first()
        assertEquals(initialSize + 1, comments.size)
        assertEquals("¿Atienden los domingos?", comments.first().text)
        assertTrue(observeComments("no_existe").first().isEmpty())
    }
}
