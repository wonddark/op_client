package com.opclient.recentchanges.domain

import com.opclient.core.ApiError
import com.opclient.core.Result

interface RecentChangesRepository {
    suspend fun getRecentChanges(): Result<List<RecentChange>, ApiError>
}
