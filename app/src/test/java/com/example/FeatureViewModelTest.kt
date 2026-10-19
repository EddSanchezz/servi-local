package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.data.model.PostStatus
import com.example.core.data.model.UserRole
import com.example.core.data.repository.MockDataRepository
import com.example.features.app.viewmodel.AppViewModel
import com.example.features.auth.viewmodel.AuthViewModel
import com.example.features.moderation.viewmodel.ModerationViewModel
import com.example.features.posts.viewmodel.CreatePostViewModel
import com.example.features.explore.viewmodel.ExploreViewModel
import com.example.features.explore.viewmodel.ViewDisplayMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FeatureViewModelTest {

    @Test
    fun `app session changes role without owning navigation`() {
        val viewModel = AppViewModel(MockDataRepository())

        viewModel.login(UserRole.MODERATOR)
        assertEquals(UserRole.MODERATOR, viewModel.uiState.value.activeRole)

        viewModel.logout()
        assertEquals(UserRole.USER, viewModel.uiState.value.activeRole)
    }

    @Test
    fun `login validation returns feedback for malformed fields`() {
        val viewModel = AuthViewModel(MockDataRepository())

        assertFalse(viewModel.login("not-an-email", "123"))
        assertEquals("Ingresa un correo válido.", viewModel.uiState.value.emailError)
        assertEquals("Usa al menos 6 caracteres.", viewModel.uiState.value.passwordError)
    }

    @Test
    fun `valid local login passes form validation`() {
        val viewModel = AuthViewModel(MockDataRepository())

        assertTrue(viewModel.login("ana@example.com", "clave2026"))
        assertEquals(null, viewModel.uiState.value.emailError)
        assertEquals(null, viewModel.uiState.value.passwordError)
    }

    @Test
    fun `registration rejects missing name and accepts valid details`() {
        val repository = MockDataRepository()
        val viewModel = AuthViewModel(repository)

        assertFalse(viewModel.register(" ", "ana@example.com"))
        assertNotNull(viewModel.uiState.value.nameError)
        assertTrue(viewModel.register("Ana Torres", "ana@example.com"))
        assertEquals("Ana Torres", repository.currentUser.value.name)
    }

    @Test
    fun `create post validation returns an actionable error and saves valid posts`() {
        val repository = MockDataRepository()
        val viewModel = CreatePostViewModel(repository)

        assertFalse(viewModel.createPost("", "Hogar", "", 0, 0, false, ""))
        assertNotNull(viewModel.uiState.value.submissionError)

        assertTrue(viewModel.createPost("Pintura", "Hogar", "Pintura interior", 50000, 90000, false, "Armenia"))
        assertEquals(PostStatus.PENDING_VERIFICATION, repository.posts.value.first().status)
    }

    @Test
    fun `explore controls keep screen state separate from session`() {
        val viewModel = ExploreViewModel(MockDataRepository())

        viewModel.setSearchQuery("electricista")
        viewModel.setSelectedCategory("Electricidad")
        viewModel.setDisplayMode(ViewDisplayMode.MAP)

        assertEquals("electricista", viewModel.uiState.value.searchQuery)
        assertEquals("Electricidad", viewModel.uiState.value.selectedCategory)
        assertEquals(ViewDisplayMode.MAP, viewModel.uiState.value.displayMode)
    }

    @Test
    fun `user and moderator schedules are available and moderator shift can be toggled`() {
        val repository = MockDataRepository()
        val moderationViewModel = ModerationViewModel(repository)

        assertTrue(repository.currentUser.value.workingHours.contains("sábado"))
        assertTrue(repository.moderatorUser.value.workingHours.contains("viernes"))
        assertTrue(repository.moderatorUser.value.activeShift)

        moderationViewModel.toggleModeratorShift()
        assertFalse(repository.moderatorUser.value.activeShift)
    }

    @Test
    fun `application name is available to Android resources`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("ServiLocal", context.getString(R.string.app_name))
    }
}
