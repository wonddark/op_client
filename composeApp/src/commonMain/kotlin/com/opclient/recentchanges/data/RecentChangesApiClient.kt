package com.opclient.recentchanges.data

import com.opclient.core.ApiClient
import com.opclient.core.ApiError
import com.opclient.core.Result
import io.ktor.client.HttpClient

class RecentChangesApiClient(httpClient: HttpClient) :
    ApiClient(baseUrl = "https://openlibrary.org", httpClient = httpClient) {

    suspend fun getRecentChanges(): Result<List<RecentChangeDto>, ApiError> =
        get(path = "/recentchanges.json")
}
