package com.servilocal

import com.servilocal.core.data.repository.MockDataRepository
import com.servilocal.data.repository.memory.BadgeRepositoryImpl
import com.servilocal.data.repository.memory.SessionRepositoryImpl
import com.servilocal.data.repository.memory.UserRepositoryImpl
import com.servilocal.domain.model.NotificationType
import com.servilocal.domain.model.UserLevel
import com.servilocal.domain.model.UserRole
import com.servilocal.domain.usecase.notifications.CreateNotificationUseCase
import com.servilocal.domain.usecase.profile.AddPointsUseCase
import com.servilocal.domain.usecase.profile.CheckAndAwardBadgesUseCase
import com.servilocal.domain.usecase.profile.GetProfileStatsUseCase
import com.servilocal.domain.usecase.profile.ObserveUserBadgesUseCase
import com.servilocal.domain.usecase.user.GetCurrentUserUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProfileUseCaseTest {

    private lateinit var users: UserRepositoryImpl
    private lateinit var sessions: SessionRepositoryImpl
    private lateinit var badges: BadgeRepositoryImpl
    private lateinit var posts: MockDataRepository
    private lateinit var addPoints: AddPointsUseCase
    private lateinit var getProfileStats: GetProfileStatsUseCase
    private lateinit var observeUserBadges: ObserveUserBadgesUseCase
    private lateinit var checkAndAwardBadges: CheckAndAwardBadgesUseCase

    @Before
    fun setUp() {
        users = UserRepositoryImpl()
        sessions = SessionRepositoryImpl()
        badges = BadgeRepositoryImpl()
        posts = MockDataRepository()
        addPoints = AddPointsUseCase(users)
        getProfileStats = GetProfileStatsUseCase(
            GetCurrentUserUseCase(sessions, users),
            posts,
            badges
        )
        observeUserBadges = ObserveUserBadgesUseCase(sessions, badges)
        checkAndAwardBadges = CheckAndAwardBadgesUseCase(
            sessions,
            users,
            badges,
            CreateNotificationUseCase(posts)
        )
    }

    @Test
    fun `add points recalculates the level`() = runTest {
        val updated = addPoints("user_val_1", 50)

        assertEquals(270, updated?.points)
        assertEquals(UserLevel.EXPERTO, updated?.level)
    }

    @Test
    fun `check and award badges grants new badges and creates a notification`() = runTest {
        sessions.saveSession("user_val_1", UserRole.USER)
        addPoints("user_val_1", 50)

        val awarded = checkAndAwardBadges()

        assertEquals(listOf("badge_3"), awarded.map { it.id })
        assertTrue(
            badges.userBadges.value.any {
                it.userId == "user_val_1" && it.badgeId == "badge_3"
            }
        )
        assertTrue(
            posts.notifications.value.any {
                it.userId == "user_val_1" &&
                    it.type == NotificationType.ACHIEVEMENT &&
                    it.title.contains("Súper Proveedor")
            }
        )
    }

    @Test
    fun `check and award badges does not duplicate an existing badge`() = runTest {
        sessions.saveSession("user_val_1", UserRole.USER)
        addPoints("user_val_1", 50)

        assertEquals(1, checkAndAwardBadges().size)
        assertTrue(checkAndAwardBadges().isEmpty())
    }

    @Test
    fun `observe user badges exposes the catalog with unlocked state`() = runTest {
        sessions.saveSession("user_val_1", UserRole.USER)

        val catalog = observeUserBadges().first()

        assertEquals(4, catalog.size)
        assertEquals(2, catalog.count { it.unlocked })
        assertTrue(catalog.first { it.id == "badge_1" }.unlocked)
        assertTrue(!catalog.first { it.id == "badge_4" }.unlocked)
    }

    @Test
    fun `profile stats combine points posts and badges`() = runTest {
        sessions.saveSession("user_val_1", UserRole.USER)

        val stats = getProfileStats()

        assertNotNull(stats)
        assertEquals("user_val_1", stats?.user?.id)
        assertEquals(220, stats?.user?.points)
        assertEquals(2, stats?.badgeCount)
        assertTrue((stats?.totalPosts ?: 0) >= 1)
        assertTrue((stats?.progress ?: 0f) > 0f)
        assertTrue(stats?.levelLabel?.contains("Nivel") == true)
    }

    @Test
    fun `profile stats and badges are empty without a session`() = runTest {
        assertNull(getProfileStats())
        assertTrue(observeUserBadges().first().isEmpty())
    }
}
