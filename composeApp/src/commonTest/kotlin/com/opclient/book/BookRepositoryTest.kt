package com.opclient.book

import com.opclient.author.domain.AuthorDetail
import com.opclient.author.domain.AuthorRepository
import com.opclient.book.data.BookApiClient
import com.opclient.book.data.BookCache
import com.opclient.book.data.BookRepositoryImpl
import com.opclient.book.domain.BookDetail
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

class BookRepositoryTest {

    private val workJson = """
        {
          "key": "/works/OL82563W",
          "title": "Dune",
          "description": "A great novel",
          "first_publish_date": "1965",
          "subjects": ["Science Fiction"],
          "covers": [12345],
          "authors": [
            { "author": { "key": "/authors/OL26320A" } }
          ]
        }
    """.trimIndent()

    private fun fakeAuthorRepo(
        authorDetail: AuthorDetail = AuthorDetail(
            key = "/authors/OL26320A", name = "Frank Herbert", bio = null,
            birthDate = null, deathDate = null, photoUrl = null, works = emptyList(),
        ),
    ): AuthorRepository = object : AuthorRepository {
        override suspend fun getAuthor(authorKey: String): Result<AuthorDetail, ApiError> =
            Result.Success(authorDetail)
    }

    private fun makeRepo(
        engine: MockEngine,
        cache: BookCache = BookCache(),
        authorRepository: AuthorRepository = fakeAuthorRepo(),
    ): BookRepositoryImpl {
        val apiClient = BookApiClient(buildHttpClient(engine))
        return BookRepositoryImpl(apiClient = apiClient, cache = cache, authorRepository = authorRepository)
    }

    @Test
    fun getBook_cacheMiss_callsApiAndReturnsDetail() = runTest {
        var callCount = 0
        val engine = MockEngine {
            callCount++
            respond(workJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }

        val result = makeRepo(engine).getBook("/works/OL82563W")

        assertIs<Result.Success<BookDetail>>(result)
        assertEquals("Dune", result.value.title)
        assertEquals("A great novel", result.value.description)
        assertEquals("Frank Herbert", result.value.authors[0].name)
        assertEquals(1, callCount)
    }

    @Test
    fun getBook_cacheHit_skipsApi() = runTest {
        var callCount = 0
        val engine = MockEngine {
            callCount++
            respond(workJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val cache = BookCache()
        val repo = makeRepo(engine, cache)

        repo.getBook("/works/OL82563W")
        repo.getBook("/works/OL82563W")

        assertEquals(1, callCount)
    }

    @Test
    fun getBook_apiError_propagatesFailure() = runTest {
        val engine = MockEngine {
            respond("error", HttpStatusCode.InternalServerError, headersOf(HttpHeaders.ContentType, "text/plain"))
        }

        val result = makeRepo(engine).getBook("/works/OL82563W")

        assertIs<Result.Failure<ApiError>>(result)
        assertIs<ApiError.HttpError>(result.error)
        assertEquals(500, (result.error as ApiError.HttpError).code)
    }

    @Test
    fun getBook_authorFetchFails_authorRefExcluded() = runTest {
        val engine = MockEngine {
            respond(workJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val failingAuthorRepo = object : AuthorRepository {
            override suspend fun getAuthor(authorKey: String): Result<AuthorDetail, ApiError> =
                Result.Failure(ApiError.HttpError(404, "Not Found"))
        }

        val result = makeRepo(engine, authorRepository = failingAuthorRepo).getBook("/works/OL82563W")

        assertIs<Result.Success<BookDetail>>(result)
        assertEquals(emptyList(), result.value.authors)
    }

    @Test
    fun getBook_multipleAuthors_fetchedInParallel() = runTest {
        val multiAuthorJson = """
            {
              "key": "/works/OL1W",
              "title": "Co-written",
              "authors": [
                { "author": { "key": "/authors/OL1A" } },
                { "author": { "key": "/authors/OL2A" } }
              ]
            }
        """.trimIndent()
        val engine = MockEngine {
            respond(multiAuthorJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        var authorCallCount = 0
        val countingAuthorRepo = object : AuthorRepository {
            override suspend fun getAuthor(authorKey: String): Result<AuthorDetail, ApiError> {
                authorCallCount++
                return Result.Success(AuthorDetail(key = authorKey, name = "A$authorCallCount",
                    bio = null, birthDate = null, deathDate = null, photoUrl = null, works = emptyList()))
            }
        }

        val result = makeRepo(engine, authorRepository = countingAuthorRepo).getBook("/works/OL1W")

        assertIs<Result.Success<BookDetail>>(result)
        assertEquals(2, result.value.authors.size)
        assertEquals(2, authorCallCount)
    }
}
