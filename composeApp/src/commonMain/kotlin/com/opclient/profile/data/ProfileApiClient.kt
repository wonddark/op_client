package com.opclient.profile.data

import com.opclient.core.ApiClient
import com.opclient.core.ApiError
import com.opclient.core.Result
import io.ktor.client.HttpClient

class ProfileApiClient(httpClient: HttpClient) :
    ApiClient(baseUrl = "https://openlibrary.org", httpClient = httpClient) {

    suspend fun getProfile(username: String): Result<UserProfileDto, ApiError> =
        get(path = "/people/$username.json")
}
