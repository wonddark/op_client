package com.opclient.core

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

abstract class ApiClient(
    @PublishedApi internal val baseUrl: String,
    @PublishedApi internal val httpClient: HttpClient,
) {
    internal suspend inline fun <reified T> get(
        path: String,
        queryParams: Map<String, String> = emptyMap(),
    ): Result<T, ApiError> = try {
        val response = httpClient.get(baseUrl + path) {
            queryParams.forEach { (key, value) -> url.parameters.append(key, value) }
        }
        Result.Success(response.body())
    } catch (e: ClientRequestException) {
        Result.Failure(ApiError.HttpError(e.response.status.value, e.message ?: ""))
    } catch (e: ServerResponseException) {
        Result.Failure(ApiError.HttpError(e.response.status.value, e.message ?: ""))
    } catch (e: Exception) {
        Result.Failure(ApiError.NetworkError(e))
    }

    internal suspend inline fun <reified T, reified B : Any> post(
        path: String,
        body: B,
    ): Result<T, ApiError> = try {
        val response = httpClient.post(baseUrl + path) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        Result.Success(response.body())
    } catch (e: ClientRequestException) {
        Result.Failure(ApiError.HttpError(e.response.status.value, e.message ?: ""))
    } catch (e: ServerResponseException) {
        Result.Failure(ApiError.HttpError(e.response.status.value, e.message ?: ""))
    } catch (e: Exception) {
        Result.Failure(ApiError.NetworkError(e))
    }
}
