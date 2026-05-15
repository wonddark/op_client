package com.opclient.lists.data

import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.core.map
import com.opclient.lists.domain.ListSeed
import com.opclient.lists.domain.ListsRepository
import com.opclient.lists.domain.ReadingList
import com.opclient.settings.domain.SettingsRepository

class ListsRepositoryImpl(
    private val apiClient: ListsApiClient,
    private val settings: SettingsRepository,
) : ListsRepository {

    private suspend fun username(): String? = settings.getUsername()

    override suspend fun getLists(): Result<List<ReadingList>, ApiError> {
        val user = username()
            ?: return Result.Failure(ApiError.NetworkError(Exception("Not logged in — username not set")))
        return apiClient.getLists(user).map { dto -> dto.lists.map { it.toDomain() } }
    }

    override suspend fun createList(name: String, description: String): Result<ReadingList, ApiError> {
        val user = username()
            ?: return Result.Failure(ApiError.NetworkError(Exception("Not logged in")))
        val request = CreateListRequestDto(name = name, description = description)
        return apiClient.createList(user, request).map { dto ->
            ReadingList(key = dto.key, name = dto.name, description = description, seedCount = 0)
        }
    }

    override suspend fun deleteList(listKey: String): Result<Unit, ApiError> =
        apiClient.deleteList(listKey).map { }

    override suspend fun getSeeds(listKey: String): Result<List<ListSeed>, ApiError> =
        apiClient.getSeeds(listKey).map { dto ->
            dto.entries.mapNotNull { it.toDomain() }
        }

    override suspend fun addSeed(listKey: String, workKey: String): Result<Unit, ApiError> =
        apiClient.updateSeeds(listKey, SeedUpdateRequestDto(add = listOf(SeedRefDto(key = workKey)))).map { }

    override suspend fun removeSeed(listKey: String, workKey: String): Result<Unit, ApiError> =
        apiClient.updateSeeds(listKey, SeedUpdateRequestDto(remove = listOf(SeedRefDto(key = workKey)))).map { }
}
