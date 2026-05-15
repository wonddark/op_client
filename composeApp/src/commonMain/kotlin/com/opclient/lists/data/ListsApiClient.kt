package com.opclient.lists.data

import com.opclient.core.ApiClient
import com.opclient.core.ApiError
import com.opclient.core.Result
import io.ktor.client.HttpClient

class ListsApiClient(httpClient: HttpClient) :
    ApiClient(baseUrl = "https://openlibrary.org", httpClient = httpClient) {

    suspend fun getLists(username: String): Result<ListsResponseDto, ApiError> =
        get("/people/$username/lists.json")

    suspend fun createList(
        username: String,
        request: CreateListRequestDto,
    ): Result<CreateListResponseDto, ApiError> =
        post("/people/$username/lists.json", request)

    suspend fun deleteList(listKey: String): Result<DeleteResponseDto, ApiError> =
        post("$listKey/delete.json", DeleteResponseDto())

    suspend fun getSeeds(listKey: String): Result<SeedsResponseDto, ApiError> =
        get("$listKey/seeds.json")

    suspend fun updateSeeds(
        listKey: String,
        request: SeedUpdateRequestDto,
    ): Result<DeleteResponseDto, ApiError> =
        post("$listKey/seeds.json", request)
}
