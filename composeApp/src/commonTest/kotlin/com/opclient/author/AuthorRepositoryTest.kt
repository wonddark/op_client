// composeApp/src/commonTest/kotlin/com/opclient/author/AuthorRepositoryTest.kt
package com.opclient.author

import com.opclient.author.data.AuthorApiClient
import com.opclient.author.data.AuthorCache
import com.opclient.author.data.AuthorRepositoryImpl
import com.opclient.author.domain.AuthorDetail
import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.core.buildHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AuthorRepositoryTest {

    private val profileJson = """
        {
          "key": "/authors/OL26320A",
          "name": "Frank Herbert",
          "bio": "American author",
          "birth_date": "1920",
          "death_date": "1986",
          "photos": [7777]
        }
    """.trimIndent()

    private val worksJson = """
        {
          "entries": [
            { "key": "/works/OL82563W", "title": "Dune", "covers": [12345] }
          ],
          "size": 1
        }
    """.trimIndent()

    private fun makeRepo(
        engine: MockEngine,
        cache: AuthorCache = AuthorCache(),
    ): AuthorRepositoryImpl {
        val apiClient = AuthorApiClient(buildHttpClient(engine))
        return AuthorRepositoryImpl(apiClient = apiClient, cache = cache)
    }

    @Test
    fun getAuthor_cacheMiss_fetchesProfileAndWorks() = runTest {
        var callCount = 0
        val engine = MockEngine { request ->
            callCount++
            val body = if (request.url.encodedPath.endsWith("/works.json")) worksJson else profileJson
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }

        val result = makeRepo(engine).getAuthor("/authors/OL26320A")

        assertIs<Result.Success<AuthorDetail>>(result)
        assertEquals("Frank Herbert", result.value.name)
        assertEquals("American author", result.value.bio)
        assertEquals(1, result.value.works.size)
        assertEquals("Dune", result.value.works[0].title)
        assertEquals(2, callCount)
    }

    @Test
    fun getAuthor_cacheHit_skipsApi() = runTest {
        var callCount = 0
        val engine = MockEngine { request ->
            callCount++
            val body = if (request.url.encodedPath.endsWith("/works.json")) worksJson else profileJson
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val cache = AuthorCache()
        val repo = makeRepo(engine, cache)

        repo.getAuthor("/authors/OL26320A")
        repo.getAuthor("/authors/OL26320A")

        assertEquals(2, callCount)
    }

    @Test
    fun getAuthor_profileApiError_propagatesFailure() = runTest {
        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("/works.json")) {
                respond(worksJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                respond("error", HttpStatusCode.InternalServerError, headersOf(HttpHeaders.ContentType, "text/plain"))
            }
        }

        val result = makeRepo(engine).getAuthor("/authors/OL26320A")

        assertIs<Result.Failure<ApiError>>(result)
    }

    @Test
    fun getAuthor_worksApiError_returnsAuthorWithEmptyWorks() = runTest {
        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("/works.json")) {
                respond("error", HttpStatusCode.InternalServerError, headersOf(HttpHeaders.ContentType, "text/plain"))
            } else {
                respond(profileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }

        val result = makeRepo(engine).getAuthor("/authors/OL26320A")

        assertIs<Result.Success<AuthorDetail>>(result)
        assertEquals(emptyList(), result.value.works)
    }
}
