package com.opclient.recentchanges

import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.core.buildHttpClient
import com.opclient.recentchanges.data.RecentChangesApiClient
import com.opclient.recentchanges.data.RecentChangesRepositoryImpl
import com.opclient.recentchanges.domain.RecentChangesRepository
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RecentChangesRepositoryTest {

    private fun makeRepo(engine: MockEngine): RecentChangesRepository =
        RecentChangesRepositoryImpl(RecentChangesApiClient(buildHttpClient(engine)))

    @Test
    fun getRecentChanges_parsesItems() = runTest {
        val json = """
            [
              {
                "id": "abc123",
                "kind": "add-book",
                "timestamp": "2024-01-15T10:00:00Z",
                "author": {"key": "/people/mark"},
                "changes": [{"key": "/works/OL82563W"}]
              }
            ]
        """.trimIndent()
        val engine = MockEngine {
            respond(json, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val result = makeRepo(engine).getRecentChanges()
        assertIs<Result.Success<List<com.opclient.recentchanges.domain.RecentChange>>>(result)
        assertEquals(1, result.value.size)
        val item = result.value[0]
        assertEquals("abc123", item.id)
        assertEquals("Book added", item.label)
        assertEquals("/works/OL82563W", item.targetKey)
    }

    @Test
    fun getRecentChanges_emptyList_returnsEmptyList() = runTest {
        val engine = MockEngine {
            respond("[]", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val result = makeRepo(engine).getRecentChanges()
        assertIs<Result.Success<List<com.opclient.recentchanges.domain.RecentChange>>>(result)
        assertEquals(0, result.value.size)
    }

    @Test
    fun getRecentChanges_500_returnsFailure() = runTest {
        val engine = MockEngine {
            respond("Server Error", HttpStatusCode.InternalServerError, headersOf(HttpHeaders.ContentType, "text/plain"))
        }
        val result = makeRepo(engine).getRecentChanges()
        assertIs<Result.Failure<ApiError>>(result)
    }
}
