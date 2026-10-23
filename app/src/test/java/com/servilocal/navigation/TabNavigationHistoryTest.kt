package com.servilocal.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class TabNavigationHistoryTest {

    @Test
    fun `selecting an existing tab moves it to the end without duplicates`() {
        val history = listOf("home", "create-post", "messages")

        assertEquals(
            listOf("create-post", "messages", "home"),
            moveTabToTop(history, "home")
        )
    }

    @Test
    fun `selecting a new tab appends it to history`() {
        val history = listOf("home", "create-post")

        assertEquals(
            listOf("home", "create-post", "profile"),
            moveTabToTop(history, "profile")
        )
    }
}
