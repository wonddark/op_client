package com.opclient.auth.data

import com.opclient.core.ApiError
import com.opclient.core.Result
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters

class AuthApiClient(private val httpClient: HttpClient) {

    @Suppress("TooGenericExceptionCaught")
    suspend fun login(username: String, password: String): Result<HttpStatusCode, ApiError> =
        try {
            val response = httpClient.post("https://openlibrary.org/account/login") {
                setBody(
                    FormDataContent(
                        Parameters.build {
                            append("username", username)
                            append("password", password)
                            append("redirect", "/")
                        },
                    )
                )
            }
            Result.Success(response.status)
        } catch (e: ClientRequestException) {
            Result.Failure(ApiError.HttpError(e.response.status.value, e.message ?: ""))
        } catch (e: ServerResponseException) {
            Result.Failure(ApiError.HttpError(e.response.status.value, e.message ?: ""))
        } catch (e: Exception) {
            Result.Failure(ApiError.NetworkError(e))
        }
}
