package com.opclient.profile

import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.core.buildHttpClient
import com.opclient.profile.data.ProfileApiClient
import com.opclient.profile.data.UserProfileRepositoryImpl
import com.opclient.profile.domain.UserProfile
import com.opclient.profile.domain.UserProfileRepository
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ProfileRepositoryTest {

    private val profileJson = """
        {"key":"/people/mark","name":"Mark Reader","photos":[12345]}
    """.trimIndent()

    private fun makeRepo(engine: MockEngine): UserProfileRepository {
        val apiClient = ProfileApiClient(buildHttpClient(engine))
        return UserProfileRepositoryImpl(apiClient)
    }

    @Test
    fun getProfile_parsesDisplayName() = runTest {
        val engine = MockEngine {
            respond(profileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val result = makeRepo(engine).getProfile("mark")
        assertIs<Result.Success<UserProfile>>(result)
        assertEquals("Mark Reader", result.value.displayName)
    }

    @Test
    fun getProfile_parsesUsernameFromKey() = runTest {
        val engine = MockEngine {
            respond(profileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val result = makeRepo(engine).getProfile("mark")
        assertIs<Result.Success<UserProfile>>(result)
        assertEquals("mark", result.value.username)
    }

    @Test
    fun getProfile_404_returnsFailure() = runTest {
        val engine = MockEngine {
            respond("Not Found", HttpStatusCode.NotFound, headersOf(HttpHeaders.ContentType, "text/plain"))
        }
        val result = makeRepo(engine).getProfile("nonexistent")
        assertIs<Result.Failure<ApiError>>(result)
    }

    @Test
    fun getProfile_withBioObject_parsesBioText() = runTest {
        val json = """{"key":"/people/mark","name":"Mark","bio":{"type":"/type/text","value":"Book lover"}}"""
        val engine = MockEngine {
            respond(json, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val result = makeRepo(engine).getProfile("mark")
        assertIs<Result.Success<UserProfile>>(result)
        assertEquals("Book lover", result.value.bio)
    }

    @Test
    fun getProfile_withBioString_parsesBioText() = runTest {
        val json = """{"key":"/people/mark","name":"Mark","bio":"Plain bio text"}"""
        val engine = MockEngine {
            respond(json, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val result = makeRepo(engine).getProfile("mark")
        assertIs<Result.Success<UserProfile>>(result)
        assertEquals("Plain bio text", result.value.bio)
    }

    @Test
    fun getProfile_missingPhotoId_returnsNullPhotoUrl() = runTest {
        val json = """{"key":"/people/mark","name":"Mark"}"""
        val engine = MockEngine {
            respond(json, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val result = makeRepo(engine).getProfile("mark")
        assertIs<Result.Success<UserProfile>>(result)
        assertEquals(null, result.value.photoUrl)
    }
}
