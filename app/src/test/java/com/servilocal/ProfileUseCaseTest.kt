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
import com.servilocal.domain.usecase.profile.AwardPublicationPointsUseCase
import com.servilocal.domain.usecase.profile.CheckAndAwardBadgesUseCase
import com.servilocal.domain.usecase.profile.GetProfileStatsUseCase
import com.servilocal.domain.usecase.profile.ObserveUserBadgesUseCase
import com.servilocal.domain.usecase.user.ObserveCurrentUserUseCase
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
    private lateinit var awardPublicationPoints: AwardPublicationPointsUseCase
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
            ObserveCurrentUserUseCase(sessions, users),
            posts,
            badges
        )
        observeUserBadges = ObserveUserBadgesUseCase(sessions, badges)
        checkAndAwardBadges = CheckAndAwardBadgesUseCase(
            sessions,
            users,
            badges,
            posts,
            CreateNotificationUseCase(posts)
        )
        awardPublicationPoints = AwardPublicationPointsUseCase(addPoints, checkAndAwardBadges)
    }

    @Test
    fun `add points recalculates the level`() = runTest {
        val updated = addPoints(1L, 50)

        assertEquals(270, updated?.points)
        assertEquals(UserLevel.EXPERTO, updated?.level)
    }

    @Test
    fun `award publication points grants reputation and unlocks badges`() = runTest {
        sessions.saveSession(1L, UserRole.USER)

        val updated = awardPublicationPoints(1L)

        assertEquals(270, updated?.points)
        assertEquals(UserLevel.EXPERTO, updated?.level)
        assertTrue(
            badges.userBadges.value.any {
                it.userId == 1L && it.badgeId == "badge_3"
            }
        )
    }

    @Test
    fun `check and award badges grants new badges and creates a notification`() = runTest {
        sessions.saveSession(1L, UserRole.USER)
        addPoints(1L, 50)

        val awarded = checkAndAwardBadges()

        assertEquals(listOf("badge_3"), awarded.map { it.id })
        assertTrue(
            badges.userBadges.value.any {
                it.userId == 1L && it.badgeId == "badge_3"
            }
        )
        assertTrue(
            posts.notifications.value.any {
                it.userId == 1L &&
                    it.type == NotificationType.ACHIEVEMENT &&
                    it.title.contains("Súper Proveedor")
            }
        )
    }

    @Test
    fun `check and award badges does not duplicate an existing badge`() = runTest {
        sessions.saveSession(1L, UserRole.USER)
        addPoints(1L, 50)

        assertEquals(1, checkAndAwardBadges().size)
        assertTrue(checkAndAwardBadges().isEmpty())
    }

    @Test
    fun `first publication badge requires at least one post`() = runTest {
        sessions.saveSession(4L, UserRole.USER)

        val awarded = checkAndAwardBadges()

        assertEquals(listOf("badge_1"), awarded.map { it.id })
    }

    @Test
    fun `observe user badges exposes the catalog with unlocked state`() = runTest {
        sessions.saveSession(1L, UserRole.USER)

        val catalog = observeUserBadges().first()

        assertEquals(4, catalog.size)
        assertEquals(2, catalog.count { it.unlocked })
        assertTrue(catalog.first { it.id == "badge_1" }.unlocked)
        assertTrue(!catalog.first { it.id == "badge_4" }.unlocked)
    }

    @Test
    fun `profile stats combine points posts and badges`() = runTest {
        sessions.saveSession(1L, UserRole.USER)

        val stats = getProfileStats().first()

        assertNotNull(stats)
        assertEquals(1L, stats?.user?.id)
        assertEquals(220, stats?.user?.points)
        assertEquals(2, stats?.badgeCount)
        assertEquals(250, stats?.nextLevelPoints)
        assertEquals(0.8f, stats?.progress ?: 0f, 0.001f)
        assertEquals(30, stats?.remainingPoints)
        assertTrue((stats?.totalPosts ?: 0) >= 1)
        assertTrue(stats?.levelLabel?.contains("Nivel 2") == true)
    }

    @Test
    fun `master level exposes full progress without a next level`() = runTest {
        sessions.saveSession(3L, UserRole.MODERATOR)

        val stats = getProfileStats().first()

        assertNotNull(stats)
        assertEquals("Nivel 4: Maestro", stats?.levelLabel)
        assertNull(stats?.nextLevelPoints)
        assertEquals(1f, stats?.progress ?: 0f, 0.001f)
        assertEquals(0, stats?.remainingPoints)
    }

    @Test
    fun `profile stats and badges are empty without a session`() = runTest {
        assertNull(getProfileStats().first())
        assertTrue(observeUserBadges().first().isEmpty())
    }
}
