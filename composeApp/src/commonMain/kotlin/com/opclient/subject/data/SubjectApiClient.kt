package com.opclient.subject.data

import com.opclient.core.ApiClient
import com.opclient.core.ApiError
import com.opclient.core.Result
import io.ktor.client.HttpClient

class SubjectApiClient(httpClient: HttpClient) :
    ApiClient(baseUrl = "https://openlibrary.org", httpClient = httpClient) {

    suspend fun getSubject(subjectName: String, limit: Int, offset: Int): Result<SubjectDto, ApiError> {
        val normalized = subjectName.replace(" ", "_").lowercase()
        return get(
            path = "/subjects/$normalized.json",
            queryParams = mapOf("limit" to "$limit", "offset" to "$offset"),
        )
    }

    suspend fun searchSubjects(query: String): Result<SubjectSearchResponseDto, ApiError> =
        get(path = "/search/subjects.json", queryParams = mapOf("q" to query))
}
