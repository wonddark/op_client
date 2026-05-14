package com.opclient.readinglog.data

import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.library.domain.LibraryRepository
import com.opclient.library.domain.Shelf
import com.opclient.readinglog.domain.ReadingLogSyncService

class ReadingLogSyncServiceImpl(
    private val apiClient: ReadingLogApiClient,
    private val libraryRepository: LibraryRepository,
) : ReadingLogSyncService {

    override suspend fun sync(username: String): Result<Int, ApiError> {
        var entriesSynced = 0
        for (shelf in Shelf.entries) {
            when (val result = apiClient.getShelf(username, shelf)) {
                is Result.Success -> {
                    result.value.entries.forEach { entry ->
                        libraryRepository.addToShelf(entry.toLibraryEntry(shelf))
                        entriesSynced++
                    }
                }
                is Result.Failure -> return result
            }
        }
        return Result.Success(entriesSynced)
    }
}
