package com.servilocal

import com.servilocal.core.data.repository.MockDataRepository
import com.servilocal.domain.model.PostStatus
import com.servilocal.domain.usecase.posts.AddPostCommentUseCase
import com.servilocal.domain.usecase.posts.TogglePostLikeUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PostInteractionUseCaseTest {

    private fun repository() = MockDataRepository()

    private fun toggleLikeUseCase(repository: MockDataRepository) =
        TogglePostLikeUseCase(repository, repository, repository)

    private fun addCommentUseCase(repository: MockDataRepository) =
        AddPostCommentUseCase(repository, repository, repository)

    @Test
    fun `a user can like another users post only once and remove that like`() {
        val repository = repository()
        val useCase = toggleLikeUseCase(repository)
        val post = repository.posts.value.first { it.id == "pub_1" }
        val initialCount = post.upvotesCount

        assertTrue(useCase(post.id))

        val liked = repository.posts.value.first { it.id == post.id }
        assertTrue(liked.userUpvoted)
        assertEquals(initialCount + 1, liked.upvotesCount)
        assertEquals(1, repository.likes.value.size)

        assertTrue(useCase(post.id))

        val unliked = repository.posts.value.first { it.id == post.id }
        assertFalse(unliked.userUpvoted)
        assertEquals(initialCount, unliked.upvotesCount)
        assertTrue(repository.likes.value.isEmpty())
    }

    @Test
    fun `a user cannot like their own post but can comment on it as the author`() {
        val repository = repository()
        val ownPost = repository.posts.value.first { it.providerId == repository.currentUser.value.id }

        assertFalse(toggleLikeUseCase(repository)(ownPost.id))
        assertTrue(addCommentUseCase(repository)(ownPost.id, "Comentario propio"))

        val comment = repository.posts.value.first { it.id == ownPost.id }.comments.first()
        assertTrue(comment.isAuthorReply)
        assertEquals(repository.currentUser.value.id, comment.authorId)

        assertFalse(repository.posts.value.first { it.id == ownPost.id }.userUpvoted)
        assertTrue(repository.likes.value.isEmpty())
        assertEquals(1, repository.comments.value.count { it.postId == ownPost.id })
    }

    @Test
    fun `a user can leave multiple comments on another users post`() {
        val repository = repository()
        val useCase = addCommentUseCase(repository)
        val post = repository.posts.value.first { it.id == "pub_2" }

        assertTrue(useCase(post.id, "Primer comentario"))
        assertTrue(useCase(post.id, "Segundo comentario"))

        assertEquals(2, repository.comments.value.count { it.postId == post.id })
        assertEquals(2, repository.posts.value.first { it.id == post.id }.comments.size)
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

        assertFalse(addCommentUseCase(repository)(post.id, "   "))
        assertTrue(repository.comments.value.isEmpty())
    }
}
