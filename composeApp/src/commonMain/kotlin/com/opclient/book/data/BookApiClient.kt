package com.opclient.book.data

import com.opclient.core.ApiClient
import com.opclient.core.ApiError
import com.opclient.core.Result
import io.ktor.client.HttpClient

class BookApiClient(httpClient: HttpClient) :
    ApiClient(baseUrl = "https://openlibrary.org", httpClient = httpClient) {

    suspend fun getWork(workOlid: String): Result<WorkDto, ApiError> =
        get("/works/$workOlid.json")
}
