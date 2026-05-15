package com.opclient.searchinside.data

import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.searchinside.domain.SearchInsideRepository
import com.opclient.searchinside.domain.SearchInsideResult

class SearchInsideRepositoryImpl(
    private val apiClient: SearchInsideApiClient,
) : SearchInsideRepository {

    override suspend fun search(query: String): Result<List<SearchInsideResult>, ApiError> =
        when (val result = apiClient.search(query)) {
            is Result.Success -> Result.Success(
                result.value.docs
                    .filter { it.key != null }
                    .map { it.toDomain() },
            )
            is Result.Failure -> result
        }
}
