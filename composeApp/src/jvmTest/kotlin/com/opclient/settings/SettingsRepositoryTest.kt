package com.opclient.settings

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.opclient.settings.data.SettingsRepositoryImpl
import com.opclient.settings.domain.SettingsRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SettingsRepositoryTest {

    private fun createRepo(): SettingsRepository {
        val driver = JdbcSqliteDriver("jdbc:sqlite::memory:")
        SettingsDatabase.Schema.create(driver)
        return SettingsRepositoryImpl(SettingsDatabase(driver))
    }

    @Test
    fun getUsername_returnsNullWhenNotSet() = runTest {
        assertNull(createRepo().getUsername())
    }

    @Test
    fun setUsername_thenGet_returnsSavedUsername() = runTest {
        val repo = createRepo()
        repo.setUsername("mark")
        assertEquals("mark", repo.getUsername())
    }

    @Test
    fun setUsername_overwritesPreviousValue() = runTest {
        val repo = createRepo()
        repo.setUsername("mark")
        repo.setUsername("newuser")
        assertEquals("newuser", repo.getUsername())
    }

    @Test
    fun getReadingGoal_returnsNullWhenNotSet() = runTest {
        assertNull(createRepo().getReadingGoal(2026))
    }

    @Test
    fun setReadingGoal_thenGet_returnsSavedGoal() = runTest {
        val repo = createRepo()
        repo.setReadingGoal(2026, 24)
        val goal = repo.getReadingGoal(2026)
        assertEquals(2026, goal?.year)
        assertEquals(24, goal?.target)
    }

    @Test
    fun clearReadingGoal_removesGoal() = runTest {
        val repo = createRepo()
        repo.setReadingGoal(2026, 24)
        repo.clearReadingGoal(2026)
        assertNull(repo.getReadingGoal(2026))
    }

    @Test
    fun readingGoal_isolatedByYear() = runTest {
        val repo = createRepo()
        repo.setReadingGoal(2025, 20)
        repo.setReadingGoal(2026, 24)
        assertEquals(20, repo.getReadingGoal(2025)?.target)
        assertEquals(24, repo.getReadingGoal(2026)?.target)
    }
}
