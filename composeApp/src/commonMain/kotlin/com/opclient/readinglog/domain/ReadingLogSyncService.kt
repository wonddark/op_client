package com.opclient.readinglog.domain

import com.opclient.core.ApiError
import com.opclient.core.Result

interface ReadingLogSyncService {
    suspend fun sync(username: String): Result<Int, ApiError>
}
