package com.opclient.subject

import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.core.buildHttpClient
import com.opclient.subject.data.SubjectApiClient
import com.opclient.subject.data.SubjectCache
import com.opclient.subject.data.SubjectRepositoryImpl
import com.opclient.subject.domain.SubjectPage
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SubjectRepositoryTest {

    private val subjectJson = """
        {
          "name": "Science Fiction",
          "work_count": 100,
          "works": [
            {
              "key": "/works/OL1W",
              "title": "Dune",
              "authors": [{"name": "Frank Herbert"}],
              "cover_id": 123
            }
          ]
        }
    """.trimIndent()

    private fun makeRepo(
        engine: MockEngine,
        cache: SubjectCache = SubjectCache(),
    ): SubjectRepositoryImpl {
        val apiClient = SubjectApiClient(buildHttpClient(engine))
        return SubjectRepositoryImpl(apiClient = apiClient, cache = cache)
    }

    @Test
    fun getSubjectPage_cacheMiss_callsApiAndReturns() = runTest {
        var callCount = 0
        val engine = MockEngine {
            callCount++
            respond(subjectJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }

        val result = makeRepo(engine).getSubjectPage("Science Fiction", 12, 0)

        assertIs<Result.Success<SubjectPage>>(result)
        assertEquals("Science Fiction", result.value.subjectName)
        assertEquals(100, result.value.workCount)
        assertEquals(1, result.value.works.size)
        assertEquals("Dune", result.value.works[0].title)
        assertEquals(1, callCount)
    }

    @Test
    fun getSubjectPage_cacheHit_skipsApi() = runTest {
        var callCount = 0
        val engine = MockEngine {
            callCount++
            respond(subjectJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val cache = SubjectCache()
        val repo = makeRepo(engine, cache)

        repo.getSubjectPage("Science Fiction", 12, 0)
        assertEquals(1, callCount)

        repo.getSubjectPage("Science Fiction", 12, 0)
        assertEquals(1, callCount)
    }

    @Test
    fun getSubjectPage_offset0_storesInCache() = runTest {
        val engine = MockEngine {
            respond(subjectJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val cache = SubjectCache()
        val repo = makeRepo(engine, cache)

        repo.getSubjectPage("Science Fiction", 12, 0)

        val cached = cache.get("subject:science_fiction")
        assertEquals("Science Fiction", cached?.subjectName)
    }

    @Test
    fun getSubjectPage_offsetNonZero_bypassesCache() = runTest {
        var callCount = 0
        val engine = MockEngine {
            callCount++
            respond(subjectJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val cache = SubjectCache()
        val repo = makeRepo(engine, cache)

        repo.getSubjectPage("Science Fiction", 12, 0)
        assertEquals(1, callCount)

        repo.getSubjectPage("Science Fiction", 12, 12)
        assertEquals(2, callCount)

        repo.getSubjectPage("Science Fiction", 12, 12)
        assertEquals(3, callCount)
    }

    @Test
    fun getSubjectPage_normalizesSubjectName() = runTest {
        var capturedPath = ""
        val engine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            respond(subjectJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }

        makeRepo(engine).getSubjectPage("Science Fiction", 12, 0)

        assertTrue(capturedPath.contains("science_fiction"), "Expected 'science_fiction' in $capturedPath")
    }

    @Test
    fun getSubjectPage_apiError_propagatesFailure() = runTest {
        val engine = MockEngine {
            respond("error", HttpStatusCode.InternalServerError, headersOf(HttpHeaders.ContentType, "text/plain"))
        }

        val result = makeRepo(engine).getSubjectPage("Science Fiction", 12, 0)

        assertIs<Result.Failure<ApiError>>(result)
        assertIs<ApiError.HttpError>(result.error)
        assertEquals(500, (result.error as ApiError.HttpError).code)
    }
}
