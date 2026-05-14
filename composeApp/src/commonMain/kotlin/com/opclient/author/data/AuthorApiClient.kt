package com.opclient.author.data

import com.opclient.core.ApiClient
import com.opclient.core.ApiError
import com.opclient.core.Result
import io.ktor.client.HttpClient

class AuthorApiClient(httpClient: HttpClient) :
    ApiClient(baseUrl = "https://openlibrary.org", httpClient = httpClient) {

    suspend fun getAuthor(authorOlid: String): Result<AuthorDto, ApiError> =
        get("/authors/$authorOlid.json")

    suspend fun getAuthorWorks(authorOlid: String): Result<AuthorWorksDto, ApiError> =
        get("/authors/$authorOlid/works.json")
}
