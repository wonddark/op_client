package com.opclient.settings

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.opclient.settings.data.SettingsRepositoryImpl
import com.opclient.settings.SettingsDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertEquals

class SessionCookieTest {

    private fun createRepo(): SettingsRepositoryImpl {
        val driver = JdbcSqliteDriver("jdbc:sqlite::memory:")
        SettingsDatabase.Schema.create(driver)
        return SettingsRepositoryImpl(SettingsDatabase(driver))
    }

    @Test
    fun sessionCookie_nullByDefault() = runTest {
        assertNull(createRepo().getSessionCookie())
    }

    @Test
    fun setAndGet_roundtrips() = runTest {
        val repo = createRepo()
        repo.setSessionCookie("abc123")
        assertEquals("abc123", repo.getSessionCookie())
    }

    @Test
    fun clear_removesValue() = runTest {
        val repo = createRepo()
        repo.setSessionCookie("abc123")
        repo.clearSessionCookie()
        assertNull(repo.getSessionCookie())
    }
}
