package com.opclient.core

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ApiClientTest {

    @Serializable
    private data class TestBody(
        val message: String,
    )

    @Test
    fun get_200_returnsSuccess() =
        runTest {
            val engine = MockEngine {
                respond(
                    content = """{"message":"hello"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
            val client = object : ApiClient("https://api.test", buildHttpClient(engine)) {}
            val result = client.get<TestBody>("/items")
            assertIs<Result.Success<TestBody>>(result)
            assertEquals("hello", result.value.message)
        }

    @Test
    fun get_404_returnsHttpError() =
        runTest {
            val engine = MockEngine {
                respond(
                    content = "Not Found",
                    status = HttpStatusCode.NotFound,
                    headers = headersOf(HttpHeaders.ContentType, "text/plain"),
                )
            }
            val client = object : ApiClient("https://api.test", buildHttpClient(engine)) {}
            val result = client.get<TestBody>("/missing")
            assertIs<Result.Failure<ApiError>>(result)
            assertIs<ApiError.HttpError>(result.error)
            assertEquals(404, (result.error as ApiError.HttpError).code)
        }

    @Test
    fun get_appendsQueryParameters() =
        runTest {
            var capturedUrl: String? = null
            val engine = MockEngine { request ->
                capturedUrl = request.url.toString()
                respond(
                    content = """{"message":"ok"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
            val client = object : ApiClient("https://api.test", buildHttpClient(engine)) {}
            client.get<TestBody>("/search", mapOf("q" to "kotlin"))
            assertEquals(true, capturedUrl?.contains("q=kotlin"))
        }
}
