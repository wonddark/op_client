package com.opclient.searchinside.domain

import com.opclient.core.ApiError
import com.opclient.core.Result

interface SearchInsideRepository {
    suspend fun search(query: String): Result<List<SearchInsideResult>, ApiError>
}
