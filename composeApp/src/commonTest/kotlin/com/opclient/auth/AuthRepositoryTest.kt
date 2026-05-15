package com.opclient.auth

import com.opclient.auth.data.AuthApiClient
import com.opclient.auth.data.AuthRepositoryImpl
import com.opclient.auth.domain.AuthRepository
import com.opclient.core.ApiError
import com.opclient.core.PersistentCookieStorage
import com.opclient.core.Result
import com.opclient.core.buildHttpClient
import com.opclient.settings.domain.ReadingGoal
import com.opclient.settings.domain.SettingsRepository
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class AuthRepositoryTest {

    private lateinit var fakeSettings: SettingsRepository

    @BeforeTest
    fun setUp() {
        fakeSettings = object : SettingsRepository {
            private var username: String? = null
            private var session: String? = null
            private var goals: Map<Int, ReadingGoal> = emptyMap()
            override suspend fun getUsername() = username
            override suspend fun setUsername(value: String) { username = value }
            override suspend fun getReadingGoal(year: Int) = goals[year]
            override suspend fun setReadingGoal(year: Int, target: Int) { goals = goals + (year to ReadingGoal(year, target)) }
            override suspend fun clearReadingGoal(year: Int) { goals = goals - year }
            override suspend fun getSessionCookie() = session
            override suspend fun setSessionCookie(value: String) { session = value }
            override suspend fun clearSessionCookie() { session = null }
        }
    }

    private fun makeRepo(engine: MockEngine): AuthRepository {
        val storage = PersistentCookieStorage(fakeSettings)
        return AuthRepositoryImpl(AuthApiClient(buildHttpClient(engine, storage)), fakeSettings)
    }

    @Test
    fun login_success_storesCookie() = runTest {
        var requestCount = 0
        val engine = MockEngine {
            requestCount++
            if (requestCount == 1) {
                respond(
                    content = "",
                    status = HttpStatusCode.Found,
                    headers = headersOf(
                        HttpHeaders.Location to listOf("https://openlibrary.org/"),
                        HttpHeaders.SetCookie to listOf("session=testsession123; Path=/; HttpOnly"),
                    ),
                )
            } else {
                respond(content = "<html>Welcome</html>", status = HttpStatusCode.OK)
            }
        }
        val repo = makeRepo(engine)
        val result = repo.login("user", "pass")
        assertIs<Result.Success<Unit>>(result)
        assertEquals("user", fakeSettings.getUsername())
    }

    @Test
    fun login_wrongCredentials_returnsFailure() = runTest {
        val engine = MockEngine {
            respond(content = "<html>invalid credentials</html>", status = HttpStatusCode.OK)
        }
        val repo = makeRepo(engine)
        val result = repo.login("user", "wrong")
        assertIs<Result.Failure<ApiError>>(result)
        assertNull(fakeSettings.getSessionCookie())
    }

    @Test
    fun logout_clearsCookie() = runTest {
        fakeSettings.setSessionCookie("abc")
        fakeSettings.setUsername("user")
        val repo = makeRepo(MockEngine { respond("", HttpStatusCode.OK) })
        repo.logout()
        assertNull(fakeSettings.getSessionCookie())
    }

    @Test
    fun isLoggedIn_trueWhenCookieSet() = runTest {
        fakeSettings.setSessionCookie("abc")
        val repo = makeRepo(MockEngine { respond("", HttpStatusCode.OK) })
        assertIs<Boolean>(repo.isLoggedIn())
        assertEquals(true, repo.isLoggedIn())
    }
}
