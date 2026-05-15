package com.opclient.lists.domain

import com.opclient.core.ApiError
import com.opclient.core.Result

interface ListsRepository {
    suspend fun getLists(): Result<List<ReadingList>, ApiError>
    suspend fun createList(name: String, description: String): Result<ReadingList, ApiError>
    suspend fun deleteList(listKey: String): Result<Unit, ApiError>
    suspend fun getSeeds(listKey: String): Result<List<ListSeed>, ApiError>
    suspend fun addSeed(listKey: String, workKey: String): Result<Unit, ApiError>
    suspend fun removeSeed(listKey: String, workKey: String): Result<Unit, ApiError>
}
