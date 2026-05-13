package com.opclient.search.domain

import com.opclient.core.ApiError
import com.opclient.core.Result

interface SearchRepository {
    suspend fun search(query: String, offset: Int = 0, limit: Int = 20): Result<SearchResults, ApiError>
}
