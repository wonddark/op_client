package com.opclient.search.data

import com.opclient.core.ApiClient
import com.opclient.core.ApiError
import com.opclient.core.Result
import io.ktor.client.HttpClient

class SearchApiClient(httpClient: HttpClient) :
    ApiClient(baseUrl = "https://openlibrary.org", httpClient = httpClient) {

    suspend fun search(
        query: String,
        offset: Int,
        limit: Int,
    ): Result<SearchResponseDto, ApiError> =
        get<SearchResponseDto>(
            path = "/search.json",
            queryParams = mapOf(
                "q" to query,
                "offset" to "$offset",
                "limit" to "$limit",
                "fields" to "key,title,author_name,cover_i,first_publish_year",
            ),
        )
}
