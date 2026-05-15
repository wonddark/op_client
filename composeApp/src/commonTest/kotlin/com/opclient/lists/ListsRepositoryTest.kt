package com.opclient.lists

import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.core.buildHttpClient
import com.opclient.lists.data.ListsApiClient
import com.opclient.lists.data.ListsRepositoryImpl
import com.opclient.lists.domain.ListsRepository
import com.opclient.settings.domain.ReadingGoal
import com.opclient.settings.domain.SettingsRepository
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ListsRepositoryTest {

    private fun fakeSettings(username: String? = "testuser") = object : SettingsRepository {
        override suspend fun getUsername() = username
        override suspend fun setUsername(v: String) = Unit
        override suspend fun getReadingGoal(y: Int): ReadingGoal? = null
        override suspend fun setReadingGoal(y: Int, t: Int) = Unit
        override suspend fun clearReadingGoal(y: Int) = Unit
        override suspend fun getSessionCookie() = "session=test"
        override suspend fun setSessionCookie(v: String) = Unit
        override suspend fun clearSessionCookie() = Unit
    }

    private fun makeRepo(engine: MockEngine, username: String? = "testuser"): ListsRepository =
        ListsRepositoryImpl(ListsApiClient(buildHttpClient(engine)), fakeSettings(username))

    @Test
    fun getLists_parsesList() = runTest {
        val json = """
            {"lists": [{"key": "/people/testuser/lists/OL1L", "name": "My List", "seed_count": 2}]}
        """.trimIndent()
        val engine = MockEngine {
            respond(json, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val result = makeRepo(engine).getLists()
        assertIs<Result.Success<*>>(result)
        val lists = (result as Result.Success).value
        assertEquals(1, lists.size)
        assertEquals("My List", lists[0].name)
        assertEquals("/people/testuser/lists/OL1L", lists[0].key)
    }

    @Test
    fun getLists_noUsername_returnsFailure() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.OK) }
        val result = makeRepo(engine, username = null).getLists()
        assertIs<Result.Failure<ApiError>>(result)
    }

    @Test
    fun getSeeds_parsesEntries() = runTest {
        val json = """
            {"size": 1, "entries": [{"work": {"key": "/works/OL1W", "title": "Dune", "author_names": ["Frank Herbert"], "cover_id": 8368541}}]}
        """.trimIndent()
        val engine = MockEngine {
            respond(json, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val result = makeRepo(engine).getSeeds("/people/testuser/lists/OL1L")
        assertIs<Result.Success<*>>(result)
        val seeds = (result as Result.Success).value
        assertEquals(1, seeds.size)
        assertEquals("Dune", seeds[0].title)
        assertEquals("Frank Herbert", seeds[0].authorName)
    }

    @Test
    fun getLists_404_returnsFailure() = runTest {
        val engine = MockEngine {
            respond("Not Found", HttpStatusCode.NotFound, headersOf(HttpHeaders.ContentType, "text/plain"))
        }
        val result = makeRepo(engine).getLists()
        assertIs<Result.Failure<ApiError>>(result)
    }
}
