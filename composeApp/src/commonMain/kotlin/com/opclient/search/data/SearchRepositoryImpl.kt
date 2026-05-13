package com.opclient.search.data

import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.search.domain.SearchRepository
import com.opclient.search.domain.SearchResults

class SearchRepositoryImpl(
    private val apiClient: SearchApiClient,
    private val cache: SearchCache,
) : SearchRepository {

    override suspend fun search(
        query: String,
        offset: Int,
        limit: Int,
    ): Result<SearchResults, ApiError> {
        val key = "$query:$offset"
        val cached = cache.get(key)
        if (cached != null) return Result.Success(cached)

        return when (val result = apiClient.search(query, offset, limit)) {
            is Result.Success -> {
                val domain = result.value.toDomain(query)
                cache.put(key, domain)
                Result.Success(domain)
            }
            is Result.Failure -> result
        }
    }
}
