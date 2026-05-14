package com.opclient.readinglog.data

import com.opclient.core.ApiClient
import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.library.domain.Shelf
import io.ktor.client.HttpClient

class ReadingLogApiClient(httpClient: HttpClient) :
    ApiClient(baseUrl = "https://openlibrary.org", httpClient = httpClient) {

    suspend fun getShelf(username: String, shelf: Shelf): Result<ReadingLogResponseDto, ApiError> =
        get(path = "/people/$username/books/${shelf.toRemoteKey()}.json")
}

private fun Shelf.toRemoteKey(): String = when (this) {
    Shelf.WANT_TO_READ -> "want-to-read"
    Shelf.READING -> "currently-reading"
    Shelf.READ -> "already-read"
}
