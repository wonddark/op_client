package com.opclient.readinglog

import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.core.buildHttpClient
import com.opclient.library.domain.LibraryEntry
import com.opclient.library.domain.LibraryRepository
import com.opclient.library.domain.Shelf
import com.opclient.readinglog.data.ReadingLogApiClient
import com.opclient.readinglog.data.ReadingLogSyncServiceImpl
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ReadingLogSyncServiceTest {

    private val wantToReadJson = """
        {
          "reading_log_entries": [
            {
              "work": {
                "key": "/works/OL82563W",
                "title": "Dune",
                "author_names": ["Frank Herbert"],
                "cover_id": 8368541
              },
              "logged_date": "2024-01-15T10:00:00+00:00"
            }
          ]
        }
    """.trimIndent()

    private val emptyJson = """{"reading_log_entries":[]}"""

    private fun makeFakeLibrary(addedEntries: MutableList<LibraryEntry>) = object : LibraryRepository {
        override fun getShelf(shelf: Shelf): Flow<List<LibraryEntry>> = flowOf(emptyList())
        override fun getCurrentShelf(workKey: String): Flow<Shelf?> = flowOf(null)
        override suspend fun addToShelf(entry: LibraryEntry) { addedEntries += entry }
        override suspend fun removeFromShelf(workKey: String) {}
        override suspend fun moveToShelf(workKey: String, shelf: Shelf) {}
    }

    private fun makeService(engine: MockEngine, added: MutableList<LibraryEntry>): ReadingLogSyncServiceImpl {
        val apiClient = ReadingLogApiClient(buildHttpClient(engine))
        return ReadingLogSyncServiceImpl(apiClient, makeFakeLibrary(added))
    }

    @Test
    fun sync_importsEntryFromWantToRead() = runTest {
        val added = mutableListOf<LibraryEntry>()
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath.contains("want-to-read") ->
                    respond(wantToReadJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                else ->
                    respond(emptyJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val result = makeService(engine, added).sync("mark")
        assertIs<Result.Success<Int>>(result)
        assertEquals(1, result.value)
        assertEquals(1, added.size)
        assertEquals("/works/OL82563W", added[0].workKey)
        assertEquals(Shelf.WANT_TO_READ, added[0].shelf)
    }

    @Test
    fun sync_mapsCoverUrl() = runTest {
        val added = mutableListOf<LibraryEntry>()
        val engine = MockEngine { _ ->
            respond(wantToReadJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        makeService(engine, added).sync("mark")
        assertEquals("https://covers.openlibrary.org/b/id/8368541-M.jpg", added[0].coverUrl)
    }

    @Test
    fun sync_mapsAuthorName() = runTest {
        val added = mutableListOf<LibraryEntry>()
        val engine = MockEngine { _ ->
            respond(wantToReadJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        makeService(engine, added).sync("mark")
        assertEquals("Frank Herbert", added[0].authorName)
    }

    @Test
    fun sync_emptyLog_returnsZero() = runTest {
        val added = mutableListOf<LibraryEntry>()
        val engine = MockEngine { _ ->
            respond(emptyJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val result = makeService(engine, added).sync("mark")
        assertIs<Result.Success<Int>>(result)
        assertEquals(0, result.value)
    }

    @Test
    fun sync_apiError_returnsFailureEarly() = runTest {
        val added = mutableListOf<LibraryEntry>()
        val engine = MockEngine { _ ->
            respond("Server Error", HttpStatusCode.InternalServerError, headersOf(HttpHeaders.ContentType, "text/plain"))
        }
        val result = makeService(engine, added).sync("mark")
        assertIs<Result.Failure<ApiError>>(result)
    }
}
