package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.UserRole
import com.example.ui.viewmodel.AppDestination
import com.example.ui.viewmodel.ServiLocalViewModel
import com.example.ui.viewmodel.UserTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ServiLocal", appName)
  }

  @Test
  fun `viewModel handles login and role transition`() {
    val viewModel = ServiLocalViewModel()
    assertEquals(AppDestination.AUTH_LOGIN, viewModel.currentDestination.value)

    viewModel.login(UserRole.USER)
    assertEquals(AppDestination.MAIN_SHELL, viewModel.currentDestination.value)
    assertEquals(UserRole.USER, viewModel.activeRole.value)

    viewModel.switchRole(UserRole.MODERATOR)
    assertEquals(UserRole.MODERATOR, viewModel.activeRole.value)
  }

  @Test
  fun `viewModel upvote and chat interaction`() {
    val viewModel = ServiLocalViewModel()
    val initialPosts = viewModel.posts.value
    assertTrue(initialPosts.isNotEmpty())

    val firstPost = initialPosts.first()
    val initialUpvotes = firstPost.upvotesCount
    viewModel.toggleUpvote(firstPost.id)

    val updatedPost = viewModel.posts.value.first { it.id == firstPost.id }
    assertEquals(initialUpvotes + 1, updatedPost.upvotesCount)
    assertTrue(updatedPost.userUpvoted)

    // Message sending
    val conv = viewModel.conversations.value.first()
    val initialMsgCount = conv.messages.size
    viewModel.sendMessage(conv.id, "Hola, necesito cotización urgente")
    val updatedConv = viewModel.conversations.value.first { it.id == conv.id }
    assertEquals(initialMsgCount + 1, updatedConv.messages.size)
  }
}
