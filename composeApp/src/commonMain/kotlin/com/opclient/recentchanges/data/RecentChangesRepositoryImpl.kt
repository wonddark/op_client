package com.opclient.recentchanges.data

import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.recentchanges.domain.RecentChange
import com.opclient.recentchanges.domain.RecentChangesRepository

class RecentChangesRepositoryImpl(
    private val apiClient: RecentChangesApiClient,
) : RecentChangesRepository {

    override suspend fun getRecentChanges(): Result<List<RecentChange>, ApiError> =
        when (val result = apiClient.getRecentChanges()) {
            is Result.Success -> Result.Success(
                result.value
                    .filter { it.id != null }
                    .map { it.toDomain() },
            )
            is Result.Failure -> result
        }
}
