package com.opclient.searchinside.data

import com.opclient.core.ApiClient
import com.opclient.core.ApiError
import com.opclient.core.Result
import io.ktor.client.HttpClient

class SearchInsideApiClient(httpClient: HttpClient) :
    ApiClient(baseUrl = "https://openlibrary.org", httpClient = httpClient) {

    suspend fun search(query: String): Result<SearchInsideResponseDto, ApiError> =
        get(path = "/search/inside.json", queryParams = mapOf("q" to query))
}
