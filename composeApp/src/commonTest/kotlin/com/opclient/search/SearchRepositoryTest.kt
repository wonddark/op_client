package com.opclient.search

import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.core.buildHttpClient
import com.opclient.search.data.SearchApiClient
import com.opclient.search.data.SearchCache
import com.opclient.search.data.SearchRepositoryImpl
import com.opclient.search.domain.SearchResults
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SearchRepositoryTest {

    private val successJson = """
        {
          "numFound": 100,
          "start": 0,
          "docs": [
            {
              "key": "/works/OL82563W",
              "title": "Dune",
              "author_name": ["Frank Herbert"],
              "first_publish_year": 1965,
              "cover_i": 8368541
            }
          ]
        }
    """.trimIndent()

    private fun makeRepo(
        engine: MockEngine,
        cache: SearchCache = SearchCache(),
    ): SearchRepositoryImpl {
        val apiClient = SearchApiClient(buildHttpClient(engine))
        return SearchRepositoryImpl(apiClient = apiClient, cache = cache)
    }

    @Test
    fun search_cacheMiss_callsApiAndReturnsResults() = runTest {
        var callCount = 0
        val engine = MockEngine {
            callCount++
            respond(
                content = successJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val result = makeRepo(engine).search("dune")

        assertIs<Result.Success<SearchResults>>(result)
        assertEquals(100, result.value.totalFound)
        assertEquals(1, result.value.books.size)
        assertEquals("Dune", result.value.books[0].title)
        assertEquals("Frank Herbert", result.value.books[0].author)
        assertEquals(1, callCount)
    }

    @Test
    fun search_cacheHit_skipsApi() = runTest {
        var callCount = 0
        val engine = MockEngine {
            callCount++
            respond(
                content = successJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val cache = SearchCache()
        val repo = makeRepo(engine, cache)

        repo.search("dune")  // miss — populates cache
        repo.search("dune")  // hit — must not call API

        assertEquals(1, callCount)
    }

    @Test
    fun search_differentOffsets_cachedIndependently() = runTest {
        var callCount = 0
        val engine = MockEngine {
            callCount++
            respond(
                content = successJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repo = makeRepo(engine)

        repo.search("dune", offset = 0)
        repo.search("dune", offset = 20)

        assertEquals(2, callCount)
    }

    @Test
    fun search_apiError_propagatesFailure() = runTest {
        val engine = MockEngine {
            respond(
                content = "Internal Server Error",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, "text/plain"),
            )
        }

        val result = makeRepo(engine).search("dune")

        assertIs<Result.Failure<ApiError>>(result)
        assertIs<ApiError.HttpError>(result.error)
        assertEquals(500, (result.error as ApiError.HttpError).code)
    }
}
