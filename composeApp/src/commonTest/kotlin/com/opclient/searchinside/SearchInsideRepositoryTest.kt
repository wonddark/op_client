package com.opclient.searchinside

import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.core.buildHttpClient
import com.opclient.searchinside.data.SearchInsideApiClient
import com.opclient.searchinside.data.SearchInsideRepositoryImpl
import com.opclient.searchinside.domain.SearchInsideRepository
import com.opclient.searchinside.domain.SearchInsideResult
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SearchInsideRepositoryTest {

    private fun makeRepo(engine: MockEngine): SearchInsideRepository =
        SearchInsideRepositoryImpl(SearchInsideApiClient(buildHttpClient(engine)))

    @Test
    fun search_parsesResults() = runTest {
        val json = """
            {
              "hits": {
                "hits": [
                  {
                    "highlight": {"text": ["The spice must flow."]},
                    "edition": {
                      "work_key": "/works/OL82563W",
                      "title": "Dune",
                      "authors": [{"name": "Frank Herbert"}],
                      "cover_url": "//covers.openlibrary.org/b/id/8368541-M.jpg"
                    }
                  }
                ]
              }
            }
        """.trimIndent()
        val engine = MockEngine {
            respond(json, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val result = makeRepo(engine).search("spice")
        assertIs<Result.Success<List<SearchInsideResult>>>(result)
        assertEquals(1, result.value.size)
        val item = result.value[0]
        assertEquals("/works/OL82563W", item.workKey)
        assertEquals("Dune", item.title)
        assertEquals("Frank Herbert", item.authorName)
        assertEquals("The spice must flow.", item.passage)
        assertEquals("https://covers.openlibrary.org/b/id/8368541-M.jpg", item.coverUrl)
    }

    @Test
    fun search_stripsHighlightMarkers() = runTest {
        val json = """
            {
              "hits": {
                "hits": [
                  {
                    "highlight": {"text": ["The {{{spice}}} must flow."]},
                    "edition": {"work_key": "/works/OL82563W", "title": "Dune", "authors": []}
                  }
                ]
              }
            }
        """.trimIndent()
        val engine = MockEngine {
            respond(json, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val result = makeRepo(engine).search("spice")
        assertIs<Result.Success<List<SearchInsideResult>>>(result)
        assertEquals("The spice must flow.", result.value[0].passage)
    }

    @Test
    fun search_emptyResponse_returnsEmptyList() = runTest {
        val json = """{"hits": {"hits": []}}"""
        val engine = MockEngine {
            respond(json, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val result = makeRepo(engine).search("xyz")
        assertIs<Result.Success<List<SearchInsideResult>>>(result)
        assertEquals(0, result.value.size)
    }

    @Test
    fun search_404_returnsFailure() = runTest {
        val engine = MockEngine {
            respond("Not Found", HttpStatusCode.NotFound, headersOf(HttpHeaders.ContentType, "text/plain"))
        }
        val result = makeRepo(engine).search("anything")
        assertIs<Result.Failure<ApiError>>(result)
    }

    @Test
    fun search_filtersHitsWithNullWorkKey() = runTest {
        val json = """
            {
              "hits": {
                "hits": [
                  {
                    "highlight": {"text": ["passage A"]},
                    "edition": {"work_key": "/works/OL1W", "title": "Book A", "authors": []}
                  },
                  {
                    "highlight": {"text": ["passage B"]},
                    "edition": {"title": "No Key Book", "authors": []}
                  }
                ]
              }
            }
        """.trimIndent()
        val engine = MockEngine {
            respond(json, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val result = makeRepo(engine).search("query")
        assertIs<Result.Success<List<SearchInsideResult>>>(result)
        assertEquals(1, result.value.size)
        assertEquals("/works/OL1W", result.value[0].workKey)
    }
}
